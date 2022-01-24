<template>
  <section id="Protected" class="hero is-fullheight is-info">
    <div class="hero-body">
      <div class="container has-text-centered">
        <validation-observer v-if="!isAuthenticated" v-slot="{ invalid }" slim>
          <form
            class="mx-auto"
            style="max-width: 400px"
            @submit.prevent="validatePassword"
          >
            <!-- Personal information name field -->
            <validation-provider
              v-slot="{ errors, valid }"
              name="password"
              rules="required"
              slim
            >
              <b-field
                :label="$t('password.label')"
                :message="errors"
                :type="{ 'is-danger': errors.length > 0, 'is-success': valid }"
              >
                <b-input
                  v-model.trim="password"
                  password-reveal
                  size="is-medium"
                  type="password"
                  :placeholder="$t('password.placeholder')"
                />
              </b-field>
            </validation-provider>
            <b-field>
              <b-button
                native-type="submit"
                size="is-medium"
                type="is-primary"
                :disabled="invalid"
                :loading="isSending"
              >
                {{ $t('submit') }}
              </b-button>
            </b-field>
          </form>
        </validation-observer>
        <template v-else>
          <b-button
            class="mb-4"
            size="is-medium"
            type="is-primary"
            :loading="downloading"
            @click="downloadCSV"
          >
            Descargar
          </b-button>
          <full-calendar ref="fullCalendar" :options="calendarOptions" />
        </template>
      </div>
      <b-modal scroll="keep" :active.sync="isModalActive" :width="480">
        <div class="box content is-medium has-text-centered">
          <div v-if="!loading" class="divider">Información de la Cita</div>
          <p>
            <b class="mr-4">Nombre del Cliente</b>
            <span style="display: block">{{ activeEvent.contactName }}</span>
          </p>
          <p>
            <b class="mr-4">Teléfono del Cliente</b>
            <span style="display: block">
              {{ activeEvent.contactPhone }}
            </span>
          </p>
          <p>
            <b class="mr-4">Correo del Cliente</b>
            <span style="display: block; word-break: break-all">
              {{ activeEvent.contactEmail }}
            </span>
          </p>
          <p>
            <b class="mr-4">Ubicación</b>
            <span style="display: block">
              {{ activeEvent.locationName }}
            </span>
          </p>
          <p>
            <b class="mr-4">Dirección</b>
            <span style="display: block">
              {{ activeEvent.locationAddress }}
            </span>
          </p>
          <div v-if="!loading" class="divider">
            Información de los Productos
          </div>
          <div class="content">
            <b-table :data="activeEvent.products" :loading="loading">
              <b-table-column v-slot="{ row }" header-class="is-hidden">
                <p>
                  <strong>Categoría: </strong>
                  <span>{{ row.category }}</span>
                </p>
                <p>
                  <strong>Cantidad: </strong>
                  <span>{{ row.quantity }}</span>
                </p>
              </b-table-column>
            </b-table>
          </div>
          <div class="divider">Administrar</div>
          <b-button
            tag="a"
            target="_blank"
            type="is-danger is-light"
            :href="`${appUrl}/cancelar-cita?id=${activeEvent.id}`"
            @click="refetchEventsAfterDelay"
          >
            Cancelar Cita
          </b-button>
        </div>
      </b-modal>
    </div>
  </section>
</template>

<script lang="ts">
  import FullCalendar from '@fullcalendar/vue';
  import dayGridPlugin from '@fullcalendar/daygrid';
  import interactionPlugin from '@fullcalendar/interaction';
  import listPlugin from '@fullcalendar/list';
  import esLocale from '@fullcalendar/core/locales/es';

  import { Duration, ZonedDateTime } from '@js-joda/core';
  import { ValidationObserver, ValidationProvider, extend } from 'vee-validate';
  import Vue from 'vue';
  import delay from 'lodash/delay';
  import { required } from 'vee-validate/dist/rules';

  import bulmaPlugin from '@/bulma_theme';

  enum Collection {
    ForAWalk = 1,
    ElectronicsAndAccesories,
    Entertainment,
    BathAndHome,
    Clothing,
  }

  enum Quality {
    Used = 1,
    Good,
    Excellent,
  }

  interface Category {
    readonly id: number;
    readonly name: string;
    readonly collection: Collection;
    readonly icon: string;
  }

  interface ClickedEvent {
    readonly event: Event;
  }

  interface Event {
    readonly id: string;
    readonly start: string;
    readonly end: string;
    readonly color: string;
    readonly title: string;
    readonly extendedProps: {
      readonly locationId: number;
      readonly contactEmail: string;
      readonly contactName: string;
      readonly contactPhone: string;
    };
  }

  interface Location {
    readonly id: number;
    readonly name: string;
    readonly address: string;
  }

  interface LocationMap {
    [key: number]: Location;
  }

  interface Product {
    readonly category?: string;
    readonly collection?: Collection;
    readonly quality: Quality;
    readonly quantity: number;
  }

  interface ServerProduct {
    readonly categoryId: number;
    readonly quality: Quality;
    readonly quantity: number;
  }

  interface ServerEvent {
    readonly id: string;
    readonly start: string;
    readonly duration: string;
    readonly locationId: number;
    readonly contactEmail: string;
    readonly contactName: string;
    readonly contactPhone: string;
    readonly confirmed: boolean;
  }

  const appUrl =
    process.env.NODE_ENV === 'development'
      ? 'http://localhost:9000'
      : 'https://api.entrepeques.com.mx';

  // Define rules for use with vee validate.
  extend('required', required);

  export default Vue.extend({
    // Allow this component to know about the existance of certain components.
    components: { FullCalendar, ValidationObserver, ValidationProvider },
    data() {
      return {
        activeEvent: {
          id: '',
          start: '',
          end: '',
          color: '',
          title: '',
          locationAddress: '',
          locationName: '',
          contactName: '',
          contactPhone: '',
          contactEmail: '',
          products: [] as Array<Product>,
        },
        appUrl,
        calendarOptions: {
          aspectRatio: 2,
          headerToolbar: {
            left: 'prev,next,today',
            center: 'title',
            right: 'dayGridMonth,dayGridWeek,listDay',
          },
          initialView: 'dayGridMonth',
          locale: 'es',
          locales: [esLocale],
          plugins: [bulmaPlugin, dayGridPlugin, interactionPlugin, listPlugin],
          themeSystem: 'bulma',
          views: {
            dayGridMonth: {
              dayMaxEventRows: 4,
            },
            dayGridWeek: {
              dayMaxEventRows: 10,
            },
          },
        },
        categories: [] as Array<Category>,
        downloading: false,
        isAuthenticated: false,
        isModalActive: false,
        isSending: false,
        loading: true,
        locations: {} as LocationMap,
        password: '',
      };
    },
    mounted() {
      this.loading = true;
      Promise.all([
        this.$http.get(`${appUrl}/categories`),
        this.$http.get(`${appUrl}/locations`),
      ])
        .then(([categories, locations]) => {
          this.categories = categories.data;
          this.locations = (locations.data as Array<Location>).reduce(
            (locations, location) => {
              locations[location.id] = location;
              return locations;
            },
            {} as LocationMap,
          );
        })
        .catch(exception => console.error(exception))
        .finally(() => {
          this.loading = false;
          this.$set(this.calendarOptions, 'events', `${appUrl}/events/range`);
          this.$set(this.calendarOptions, 'eventClick', (event: ClickedEvent) =>
            this.openModal(event.event),
          );
          this.$set(
            this.calendarOptions,
            'eventDataTransform',
            (event: ServerEvent) => this.eventDataTransform(event),
          );
        });
    },
    methods: {
      downloadCSV() {
        this.downloading = true;
        this.$http
          .post(
            `${appUrl}/download-csv`,
            {
              value: this.password,
              id: 0,
              master: false,
            },
            { responseType: 'blob' },
          )
          .then(response => {
            const blob = new Blob([response.data], {
              type: response.headers['content-type'],
            });
            const link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download = 'Eventos.csv';
            link.click();
          })
          .catch(exception => {
            console.error(exception);
          })
          .finally(() => {
            this.downloading = false;
          });
      },
      eventDataTransform(eventData: ServerEvent): Event {
        const start = ZonedDateTime.parse(eventData.start);
        const duration = Duration.parse(eventData.duration);

        let color = '';
        if (eventData.confirmed) {
          if (eventData.locationId === 1) color = '#257942';
          else color = '#947600';
        } else {
          if (eventData.locationId === 1) color = '#48c774';
          else color = '#ffdd57';
        }

        let title = eventData.confirmed ? `Confirmada` : `Sin Confirmar`;

        const location = this.locations[eventData.locationId];
        if (location !== undefined) title += ` (${location.name})`;

        return {
          id: eventData.id.toString(),
          end: start.plus(duration).withFixedOffsetZone().toString(),
          start: start.withFixedOffsetZone().toString(),
          color,
          title,
          extendedProps: {
            locationId: eventData.locationId,
            contactName: eventData.contactName,
            contactPhone: eventData.contactPhone,
            contactEmail: eventData.contactEmail,
          },
        };
      },
      openModal(event: Event) {
        this.activeEvent.id = event.id;
        this.activeEvent.start = event.start;
        this.activeEvent.end = event.end;
        this.activeEvent.color = event.color;
        this.activeEvent.title = event.title;
        this.activeEvent.contactName = event.extendedProps.contactName;
        this.activeEvent.contactPhone = event.extendedProps.contactPhone;
        this.activeEvent.contactEmail = event.extendedProps.contactEmail;

        const location = this.locations[event.extendedProps.locationId];
        if (location !== undefined) {
          this.activeEvent.locationAddress = location.address;
          this.activeEvent.locationName = location.name;
        }

        this.loading = true;
        this.activeEvent.products = [];
        this.$http
          .post(`${appUrl}/products/event`, {
            eventId: this.activeEvent.id,
            password: this.password,
            master: false,
          })
          .then(response => {
            this.activeEvent.products = (
              response.data as Array<ServerProduct>
            ).map(product => {
              const category = this.categories.find(
                category => category.id === product.categoryId,
              );
              return {
                category: category?.name,
                collection: category?.collection,
                quality: product.quality,
                quantity: product.quantity,
              };
            });
          })
          .catch(exception => console.error(exception))
          .finally(() => (this.loading = false));

        this.isModalActive = true;
      },
      refetchEventsAfterDelay() {
        setTimeout(() => {
          //eslint-disable-next-line @typescript-eslint/no-explicit-any
          (this.$refs.fullCalendar as any).getApi().refetchEvents();
        }, 1000);
      },
      validatePassword() {
        this.isSending = true;
        this.$http
          .post(`${appUrl}/authorize`, {
            id: 0,
            value: this.password,
            master: false,
          })
          .then(() => {
            this.isAuthenticated = true;
            delay(
              (toast, options) => {
                toast.open(options);
              },
              200,
              this.$buefy.toast,
              {
                duration: 3000,
                message: '¡Listo! Ya tienes acceso',
                type: 'is-success',
              },
            );
          })
          .catch(error => {
            // Attempt to gracefully resolve error.
            if (error.response !== undefined) {
              // Handles 401 error, which occurs when the password
              // does not match the one on the server.
              if (error.response.status === 401) {
                delay(
                  (toast, options) => {
                    toast.open(options);
                  },
                  200,
                  this.$buefy.toast,
                  {
                    duration: 4000,
                    message: 'La contraseña es incorrecta',
                    type: 'is-warning',
                  },
                );
              }
              // Handles 500 error, which occurs when the server had an unexpected error;
              // in general this occurs very rarely & sending a new request should be all that is needed.
              if (error.response.status === 500) {
                delay(
                  (toast, options) => {
                    toast.open(options);
                  },
                  200,
                  this.$buefy.toast,
                  {
                    duration: 3000,
                    message: 'Ha occurido un error; favor de intentar de nuevo',
                    type: 'is-danger',
                  },
                );
              }
            }
          })
          .finally(() => {
            this.isSending = false;
          });
      },
    },
  });
</script>
