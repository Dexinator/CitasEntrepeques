<template>
  <section class="hero is-fullheight is-info">
    <div class="hero-body">
      <div class="container has-text-centered">
        <full-calendar :options="calendarOptions" />
      </div>
      <b-modal scroll="keep" :active.sync="isModalActive" :width="300">
        <div class="box content is-medium has-text-centered">
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
  import Vue from 'vue';

  import bulmaPlugin from '@/bulma_theme';

  interface ClickedEvent {
    readonly event: Event;
  }

  interface Event {
    readonly id: number;
    readonly start: string;
    readonly end: string;
    readonly color: string;
    readonly title: string;
    readonly extendedProps: {
      readonly locationId: number;
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

  interface ServerEvent {
    readonly id: number;
    readonly start: string;
    readonly duration: string;
    readonly locationId: number;
    readonly confirmed: boolean;
  }

  const appUrl =
    process.env.NODE_ENV === 'development'
      ? 'http://localhost:9000'
      : 'https://api.entrepeques.com.mx';

  export default Vue.extend({
    components: { FullCalendar },
    data() {
      return {
        activeEvent: {
          id: 0,
          start: '',
          end: '',
          color: '',
          title: '',
          locationAddress: '',
          locationName: '',
        },
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
        loading: true,
        locations: {} as LocationMap,
        isModalActive: false,
      };
    },
    mounted() {
      this.loading = true;
      this.$http
        .get(`${appUrl}/locations`)
        .then(response => {
          this.locations = (response.data as Array<Location>).reduce(
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
          id: eventData.id,
          end: start.plus(duration).withFixedOffsetZone().toString(),
          start: start.withFixedOffsetZone().toString(),
          color,
          title,
          extendedProps: {
            locationId: eventData.locationId,
          },
        };
      },
      openModal(event: Event) {
        this.activeEvent.id = event.id;
        this.activeEvent.start = event.start;
        this.activeEvent.end = event.end;
        this.activeEvent.color = event.color;
        this.activeEvent.title = event.title;

        const location = this.locations[event.extendedProps.locationId];
        if (location !== undefined) {
          this.activeEvent.locationAddress = location.address;
          this.activeEvent.locationName = location.name;
        }

        this.isModalActive = true;
      },
    },
  });
</script>
