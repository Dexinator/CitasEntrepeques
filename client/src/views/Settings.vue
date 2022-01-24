<template>
  <section id="Protected" class="hero is-fullheight">
    <div class="hero-body">
      <div class="container has-text-centered">
        <validation-observer v-if="!isAuthenticated" v-slot="{ invalid }" slim>
          <form
            class="mx-auto"
            style="max-width: 400px"
            @submit.prevent="validatePassword"
          >
            <!-- Password field -->
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
          <div class="divider">Cambiar Contraseña Maestra</div>
          <validation-observer
            ref="masterPasswordChange"
            v-slot="{ invalid }"
            slim
          >
            <form
              class="mx-auto"
              style="max-width: 480px"
              @submit.prevent="changePassword(true)"
            >
              <!-- Old password field -->
              <validation-provider
                v-slot="{ failedRules, errors, valid }"
                slim
                :name="password.length > 0 ? 'newPassword' : 'oldPassword'"
                :rules="`required|notMatch:${newMasterPassword}`"
              >
                <b-field
                  :label="$t('password.old')"
                  :message="errors"
                  :type="{
                    'is-danger':
                      errors.length > 0 &&
                      failedRules['notMatch'] === undefined,
                    'is-warning':
                      errors.length > 0 &&
                      failedRules['notMatch'] !== undefined,
                    'is-success': valid,
                  }"
                >
                  <b-input
                    v-model.trim="oldMasterPassword"
                    password-reveal
                    type="password"
                    :placeholder="$t('password.placeholder')"
                  />
                </b-field>
              </validation-provider>
              <!-- New password field -->
              <validation-provider
                v-slot="{ failedRules, errors, valid }"
                rules="required|min:6|confirmed:password"
                slim
                :name="
                  newMasterPassword.length >= 6
                    ? 'repeatPassword'
                    : 'newPassword'
                "
              >
                <b-field
                  :label="$t('password.new')"
                  :message="errors"
                  :type="{
                    'is-danger':
                      errors.length > 0 &&
                      failedRules['confirmed'] === undefined,
                    'is-warning':
                      errors.length > 0 &&
                      failedRules['confirmed'] !== undefined,
                    'is-success': valid,
                  }"
                >
                  <b-input
                    v-model.trim="newMasterPassword"
                    password-reveal
                    type="password"
                    :placeholder="$t('password.placeholder')"
                  />
                </b-field>
              </validation-provider>
              <!-- Repeat new password field -->
              <validation-provider
                v-slot="{ errors, valid }"
                name="repeatPassword"
                rules="required|min:6"
                slim
                vid="password"
              >
                <b-field
                  :label="$t('password.repeat')"
                  :message="errors"
                  :type="{
                    'is-danger': errors.length > 0,
                    'is-success': valid,
                  }"
                >
                  <b-input
                    v-model.trim="repeatMasterPassword"
                    password-reveal
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
          <div class="divider">Cambiar Contraseña Calendario</div>
          <validation-observer
            ref="calendarPasswordChange"
            v-slot="{ invalid }"
            slim
          >
            <form
              class="mx-auto"
              style="max-width: 480px"
              @submit.prevent="changePassword(false)"
            >
              <!-- New password field -->
              <validation-provider
                v-slot="{ failedRules, errors, valid }"
                rules="required|min:6|confirmed:password"
                slim
                :name="
                  newCalendarPassword.length >= 6
                    ? 'repeatPassword'
                    : 'newPassword'
                "
              >
                <b-field
                  :label="$t('password.new')"
                  :message="errors"
                  :type="{
                    'is-danger':
                      errors.length > 0 &&
                      failedRules['confirmed'] === undefined,
                    'is-warning':
                      errors.length > 0 &&
                      failedRules['confirmed'] !== undefined,
                    'is-success': valid,
                  }"
                >
                  <b-input
                    v-model.trim="newCalendarPassword"
                    password-reveal
                    type="password"
                    :placeholder="$t('password.placeholder')"
                  />
                </b-field>
              </validation-provider>
              <!-- Repeat new password field -->
              <validation-provider
                v-slot="{ errors, valid }"
                name="repeatPassword"
                rules="required|min:6"
                slim
                vid="password"
              >
                <b-field
                  :label="$t('password.repeat')"
                  :message="errors"
                  :type="{
                    'is-danger': errors.length > 0,
                    'is-success': valid,
                  }"
                >
                  <b-input
                    v-model.trim="repeatCalendarPassword"
                    password-reveal
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
          <div class="divider">{{ $t('Cambiar Notificación') }}</div>
          <validation-observer ref="messageChange" v-slot="{ invalid }" slim>
            <form
              class="mx-auto"
              style="max-width: 480px"
              @submit.prevent="changeNotification"
            >
              <!-- Notification message field -->
              <validation-provider
                v-slot="{ errors, valid }"
                name="notification"
                slim
              >
                <b-field
                  :label="$t('Notificación')"
                  :message="errors"
                  :type="{
                    'is-danger': errors.length > 0,
                    'is-success': valid,
                  }"
                >
                  <b-input
                    v-model.trim="notification"
                    maxlength="512"
                    type="textarea"
                    :loading="loading || isSending"
                    :placeholder="$t('Ingresar notificacion')"
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
          <div class="divider">{{ $t('Categorias Activas') }}</div>
          <div>
            <b-table
              default-sort="name"
              paginated
              per-page="10"
              :data="categories"
              :loading="loading || isSending"
            >
              <b-table-column
                v-slot="{ row }"
                field="active"
                :label="$t('Activo')"
              >
                <b-checkbox
                  :value="row.active"
                  @input="value => setCategoryActive(value, row)"
                />
              </b-table-column>
              <b-table-column
                v-slot="{ row }"
                field="name"
                searchable
                sortable
                :label="$t('Nombre')"
              >
                {{ row.name }}
              </b-table-column>
              <b-table-column
                v-slot="{ row }"
                field="collectionName"
                searchable
                sortable
                :label="$t('Categoría')"
              >
                {{ row.collectionName }}
              </b-table-column>
              <b-table-column
                v-slot="{ row }"
                field="active"
                :label="$t('Requiere Imagen')"
              >
                <b-checkbox
                  :value="row.requiresImage"
                  @input="value => setRequiresImage(value, row)"
                />
              </b-table-column>
            </b-table>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script lang="ts">
  import { ValidationObserver, ValidationProvider, extend } from 'vee-validate';
  import Vue from 'vue';
  import { confirmed, excluded, min, required } from 'vee-validate/dist/rules';
  import delay from 'lodash/delay';
  import range from 'lodash/range';

  const appUrl =
    process.env.NODE_ENV === 'development'
      ? 'http://localhost:9000'
      : 'https://api.entrepeques.com.mx';

  // Define rules for use with vee validate.
  extend('confirmed', confirmed);
  extend('notMatch', excluded);
  extend('min', min);
  extend('required', required);

  enum Collection {
    ForAWalk = 1,
    ElectronicsAndAccesories,
    Entertainment,
    BathAndHome,
    Clothing,
  }

  interface Category {
    readonly id: number;
    readonly name: string;
    readonly collection: Collection;
    collectionName?: string;
    readonly icon: string;
    requiresImage: boolean;
    active: boolean;
  }

  interface CategoryOption {
    readonly active: boolean;
    readonly id: number;
    readonly password: string;
    readonly requiresImage: boolean;
  }

  export default Vue.extend({
    // Allow this component to know about the existance of certain components.
    components: { ValidationObserver, ValidationProvider },
    data() {
      return {
        categories: [] as Array<Category>,
        collections: range(6).map(value => this.$t(`Collections.${value + 1}`)),
        isAuthenticated: false,
        isSending: false,
        loading: true,
        newCalendarPassword: '',
        newMasterPassword: '',
        oldMasterPassword: '',
        notification: '',
        password: '',
        repeatCalendarPassword: '',
        repeatMasterPassword: '',
      };
    },
    mounted() {
      this.loading = true;
      Promise.all([
        this.$http.get(`${appUrl}/categories`),
        this.$http.get(`${appUrl}/notification`),
      ])
        .then(([categories, notification]) => {
          this.categories = (categories.data as Array<Category>).map(
            category => {
              category.collectionName =
                this.collections[category.collection - 1].toString();
              return category;
            },
          );
          this.notification = notification.data;
        })
        .catch(exception => {
          console.error(exception);
        })
        .finally(() => (this.loading = false));
    },
    methods: {
      changeNotification() {
        this.isSending = true;
        this.$http
          .post(`${appUrl}/change-notification`, {
            password: this.password,
            notification: this.notification,
          })
          .then(() => {
            delay(
              (toast, options) => {
                toast.open(options);
              },
              200,
              this.$buefy.toast,
              {
                duration: 3000,
                message: '¡Exito! El valor ha sido actualizada',
                type: 'is-success',
              },
            );
          })
          .catch(error => {
            console.log(error);
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
          })
          .finally(() => {
            this.isSending = false;
          });
      },
      changePassword(master: boolean) {
        this.isSending = true;
        this.$http
          .post(`${appUrl}/change-password`, {
            newPassword: master
              ? this.newMasterPassword
              : this.newCalendarPassword,
            masterPassword: this.oldMasterPassword,
            master,
          })
          .then(() => {
            delay(
              (toast, options) => {
                toast.open(options);
              },
              200,
              this.$buefy.toast,
              {
                duration: 3000,
                message: '¡Exito! La contraseña ha sido actualizada',
                type: 'is-success',
              },
            );
            if (master) {
              this.password = this.newMasterPassword;
              this.oldMasterPassword = this.newMasterPassword;
              this.newMasterPassword = '';
              this.repeatMasterPassword = '';
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              (this.$refs.masterPasswordChange as any).reset();
            } else {
              this.newCalendarPassword = '';
              this.repeatCalendarPassword = '';
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              (this.$refs.calendarPasswordChange as any).reset();
            }
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
                    message: 'La contraseña anterior es incorrecta',
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
      setCategoryActive(value: boolean, row: Category) {
        row.active = value;
        this.updateCategory(row, () => (row.active = !value));
      },
      setRequiresImage(value: boolean, row: Category) {
        row.requiresImage = value;
        this.updateCategory(row, () => (row.requiresImage = !value));
      },
      updateCategory(category: Category, unapply: () => void) {
        this.isSending = true;
        this.$http
          .patch(`${appUrl}/category-options`, {
            password: this.password,
            id: category.id,
            active: category.active,
            requiresImage: category.requiresImage,
          })
          .then(() => {
            delay(
              (toast, options) => {
                toast.open(options);
              },
              200,
              this.$buefy.toast,
              {
                duration: 3000,
                message: '¡Exito! El valor ha sido actualizada',
                type: 'is-success',
              },
            );
          })
          .catch(error => {
            console.log(error);
            unapply();
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
          })
          .finally(() => {
            this.isSending = false;
          });
      },
      validatePassword() {
        this.isSending = true;
        this.$http
          .post(`${appUrl}/authorize`, {
            id: 0,
            value: this.password,
            master: true,
          })
          .then(() => {
            this.isAuthenticated = true;
            this.oldMasterPassword = this.password;
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
