<template>
  <section id="Home" class="hero is-info is-fullheight is-aligned-top">
    <div class="hero-body">
      <b-loading v-model="loading" />
      <validation-observer
        v-slot="props"
        class="container has-text-centered"
        style="max-width: 500px; position: relative"
        tag="div"
      >
        <b-tooltip
          label="Regresar"
          position="is-right"
          style="left: 0rem; position: absolute; top: -1rem; z-index: 50"
          type="is-dark"
        >
          <b-button
            href="https://www.entrepeques.mx/"
            icon-right="chevron-left"
            outlined
            tag="a"
            type="is-dark"
          />
        </b-tooltip>
        <div class="mx-auto" style="max-width: 200px">
          <figure class="image is-3by2">
            <img src="@/assets/img/logo.png" />
          </figure>
        </div>
        <h1 class="subtitle is-6 is-uppercase has-text-weight-bold mt-5">
          Agenda aquí tu cita
        </h1>
        <b-notification v-if="notification.length > 0" type="is-danger">
          {{ notification }}
        </b-notification>
        <form class="mx-auto" @submit.prevent="saveAppointment">
          <div class="divider">{{ $t('contactInformation') }}</div>
          <!-- Personal information name field -->
          <validation-provider
            v-slot="{ errors, valid }"
            class="field"
            name="contactName"
            rules="required"
            tag="div"
          >
            <b-field
              :label="$t('contactName.label')"
              :message="errors"
              :type="{ 'is-danger': errors.length > 0, 'is-success': valid }"
            >
              <b-input
                v-model.trim="contactName"
                :placeholder="$t('contactName.placeholder')"
              />
            </b-field>
          </validation-provider>
          <!-- Personal information email field -->
          <validation-provider
            v-slot="{ errors, failedRules, valid }"
            class="field"
            name="contactEmail"
            rules="required|email"
            tag="div"
          >
            <b-field
              :label="$t('contactEmail.label')"
              :message="errors"
              :type="{
                'is-danger':
                  errors.length > 0 && failedRules['email'] === undefined,
                'is-warning':
                  errors.length > 0 && failedRules['email'] !== undefined,
                'is-success': valid,
              }"
            >
              <b-input
                v-model.trim="contactEmail"
                type="email"
                :placeholder="$t('contactEmail.placeholder')"
              />
            </b-field>
          </validation-provider>
          <!-- Personal information phone field -->
          <validation-provider
            v-slot="{ errors, failedRules, valid }"
            class="field"
            name="contactPhone"
            tag="div"
            :rules="{
              required: true,
              phone: /^(\(?\d[\ \-\(\)]*){3}(\d[\ \-]*){7}$/,
            }"
          >
            <b-field
              :label="$t('contactPhone.label')"
              :message="errors"
              :type="{
                'is-danger':
                  errors.length > 0 && failedRules['phone'] === undefined,
                'is-warning':
                  errors.length > 0 && failedRules['phone'] !== undefined,
                'is-success': valid,
              }"
            >
              <b-input
                v-model.trim="contactPhone"
                type="phone"
                :placeholder="$t('contactPhone.placeholder')"
              />
            </b-field>
          </validation-provider>
          <div class="divider">{{ $t('productInformation') }}</div>
          <b-notification
            v-if="inactiveCategories.length > 0"
            type="is-warning"
          >
            No estamos comprando los siguientes artículos hasta nuevo aviso:
            <div class="content">
              <ul>
                <li v-for="category in inactiveCategories" :key="category.id">
                  {{ category.name }}
                </li>
              </ul>
            </div>
          </b-notification>
          <!-- Product information -->
          <template v-if="products.length > 0">
            <div
              v-for="(product, index) in products"
              :key="index"
              class="field"
            >
              <div class="level">
                <div class="level-item">
                  <label class="label">#{{ index + 1 }}</label>
                </div>
                <div class="level-right">
                  <div class="level-item">
                    <b-button
                      icon-right="times"
                      size="is-small"
                      type="is-danger"
                      @click.prevent="removeProductAt(index)"
                    />
                  </div>
                </div>
              </div>
              <!-- Product information category field -->
              <validation-provider
                v-slot="{ errors, valid }"
                class="field"
                name="categoryId"
                rules="required"
                tag="div"
              >
                <b-field
                  label-position="on-border"
                  :label="$t('categoryId.label')"
                  :message="errors"
                  :type="{
                    'is-danger': errors.length > 0,
                    'is-success': valid,
                  }"
                >
                  <b-select
                    v-model.number="products[index].categoryId"
                    expanded
                    :placeholder="$t('categoryId.placeholder')"
                    @input="value => updateCollectionForProductAt(value, index)"
                  >
                    <optgroup
                      v-for="categoryGroup in categoryGroups"
                      :key="categoryGroup.collection"
                      :label="collections[categoryGroup.collection - 1]"
                    >
                      <option
                        v-for="category in categoryGroup.categories"
                        :key="category.id"
                        :value="category.id"
                        :disabled="!category.active"
                      >
                        <span>{{ category.name }}</span>
                        <span v-if="!category.active">
                          - No por el momento
                        </span>
                      </option>
                    </optgroup>
                  </b-select>
                </b-field>
              </validation-provider>
              <!-- Product information image field -->
              <validation-provider
                v-if="product.requiresImage"
                v-slot="{ failedRules, errors, valid }"
                class="field"
                immediate
                name="image"
                rules="size:10000|imageMimes:image/*"
                tag="div"
              >
                <b-field
                  class="file mt-5"
                  :type="{
                    'is-success': valid,
                    'is-danger': failedRules['size'] !== undefined,
                    'is-warning': failedRules['mimes'] !== undefined,
                    'is-dark': !valid && errors.length === 0,
                  }"
                >
                  <b-upload v-model="products[index].image" expanded>
                    <a
                      class="button is-fullwidth"
                      :class="{
                        'is-success': valid,
                        'is-danger': failedRules['size'] !== undefined,
                        'is-warning': failedRules['mimes'] !== undefined,
                        'is-dark': !valid && errors.length === 0,
                      }"
                    >
                      <b-icon icon="upload" size="is-small" />
                      <span>{{ productImageName(index) }}</span>
                    </a>
                  </b-upload>
                </b-field>
                <b-field
                  class="file"
                  :type="{
                    'is-success': valid,
                    'is-danger': failedRules['size'] !== undefined,
                    'is-warning': failedRules['mimes'] !== undefined,
                    'is-dark': !valid && errors.length === 0,
                  }"
                >
                  <b-upload v-model="products[index].image" drag-drop expanded>
                    <section class="section">
                      <div
                        class="content has-text-centered"
                        :class="{
                          'has-text-success': valid,
                          'has-text-danger': failedRules['size'] !== undefined,
                          'has-text-warning-dark':
                            failedRules['mimes'] !== undefined,
                          'has-text-dark': !valid && errors.length === 0,
                        }"
                      >
                        <p>
                          <b-icon
                            custom-size="2x"
                            icon="upload"
                            size="is-large"
                          />
                        </p>
                        <p>
                          {{
                            $t(
                              `dragFileTo${
                                product.image === null ? 'Add' : 'Change'
                              }`,
                            )
                          }}
                        </p>
                      </div>
                    </section>
                  </b-upload>
                </b-field>
                <b-field
                  :message="errors"
                  :type="{
                    'is-success': valid,
                    'is-danger':
                      errors.length > 0 && failedRules['mimes'] === undefined,
                    'is-warning': failedRules['mimes'] !== undefined,
                  }"
                />
              </validation-provider>
              <!-- Product information product quantity field -->
              <validation-provider
                v-slot="{ errors, valid }"
                class="field"
                name="productQuantity"
                rules="required|minValue:1"
                tag="div"
              >
                <b-field
                  class="with-slider"
                  label-position="on-border"
                  :label="productQuantityLabel(product, index)"
                  :message="errors"
                  :type="{
                    'is-danger': errors.length > 0,
                    'is-success': valid,
                  }"
                >
                  <b-slider
                    v-if="products[index].collection === Collection.Clothing"
                    v-model.number="products[index].quantity"
                    lazy
                    size="is-medium"
                    style="margin-bottom: 3.5rem"
                    tooltip-type="is-link"
                    :aria-label="$t('productQuantity.label')"
                    :custom-formatter="
                      value =>
                        value > 1
                          ? apparelQuantities[(value - 1) / 40]
                          : apparelQuantities[0]
                    "
                    :max="(apparelQuantities.length - 1) * 40 + 1"
                    :min="1"
                    :step="40"
                  >
                    <b-slider-tick
                      v-for="(quantity, n) in apparelQuantities"
                      :key="n + 1"
                      :value="n * 40 + 1"
                    >
                      {{ quantity }}
                    </b-slider-tick>
                  </b-slider>
                  <b-slider
                    v-else-if="
                      products[index].categoryId === 26 ||
                      products[index].categoryId === 27
                    "
                    v-model.number="products[index].quantity"
                    lazy
                    size="is-medium"
                    style="margin-bottom: 3.5rem"
                    tooltip-type="is-link"
                    :aria-label="$t('productQuantity.label')"
                    :custom-formatter="
                      value =>
                        value > 1
                          ? toyQuantities[(value - 1) / 6]
                          : toyQuantities[0]
                    "
                    :max="(toyQuantities.length - 1) * 6 + 1"
                    :min="1"
                    :step="6"
                  >
                    <b-slider-tick
                      v-for="(quantity, n) in toyQuantities"
                      :key="n + 1"
                      :value="n * 6 + 1"
                    >
                      {{ quantity }}
                    </b-slider-tick>
                  </b-slider>
                  <b-numberinput
                    v-else
                    v-model.number="products[index].quantity"
                    controls-position="compact"
                    expanded
                    min="1"
                    :placeholder="$t('productQuantity.placeholder')"
                  />
                </b-field>
              </validation-provider>
              <!-- Product information quality field -->
              <validation-provider
                v-slot="{ errors, valid }"
                class="field"
                name="productQuality"
                tag="div"
              >
                <b-field
                  class="with-slider"
                  label-position="on-border"
                  :label="`${$t('productQuality.label')} #${index + 1}`"
                  :message="errors"
                  :type="{
                    'is-danger': errors.length > 0,
                    'is-success': valid,
                  }"
                >
                  <b-slider
                    v-model.number="products[index].quality"
                    lazy
                    size="is-medium"
                    tooltip-type="is-link"
                    :aria-label="$t('productQuality.label')"
                    :custom-formatter="value => qualities[value - 1]"
                    :min="1"
                    :max="qualities.length"
                  >
                    <b-slider-tick
                      v-for="(quality, n) in qualities"
                      :key="n + 1"
                      :value="n + 1"
                    >
                      {{ quality }}
                    </b-slider-tick>
                  </b-slider>
                </b-field>
              </validation-provider>
              <p v-if="product.quality === 1" class="help is-danger is-size-6">
                Recuerda que solo compramos cosas en buen o excelente estado.
              </p>
              <b-button
                v-if="product.quality === 1"
                type="is-danger"
                @click.prevent="removeProductAt(index)"
              >
                Eliminar
              </b-button>
            </div>
          </template>
          <template v-else>
            <p class="content is-medium has-text-color-gray-light mt-5">
              {{ $t('products.empty') }}
            </p>
          </template>
          <b-button
            icon-right="plus"
            type="is-primary"
            :size="products.length > 0 ? undefined : 'is-medium'"
            @click.prevent="event => addProduct()"
          >
            {{ $t('Ingresar otros artículos') }}
          </b-button>
          <div class="divider">{{ $t('eventInformation') }}</div>
          <!-- Appointment information location field -->
          <validation-provider
            v-slot="{ errors, valid }"
            class="field"
            name="locationId"
            rules="required"
            tag="div"
          >
            <b-field
              :label="$t('locationId.label')"
              :message="errors"
              :type="{ 'is-danger': errors.length > 0, 'is-success': valid }"
            >
              <b-select
                v-model.number="locationId"
                expanded
                :placeholder="$t('locationId.placeholder')"
              >
                <option
                  v-for="location in locations"
                  :key="location.id"
                  :value="location.id"
                >
                  {{ `${location.name}, ${location.address}` }}
                </option>
              </b-select>
            </b-field>
          </validation-provider>
          <!-- Appointment information date & time slot field -->
          <validation-provider
            v-slot="{ errors, valid }"
            class="field"
            name="eventDate"
            rules="required"
            tag="div"
          >
            <b-field
              :label="$t('eventDate.label')"
              :message="errors"
              :type="{ 'is-danger': errors.length > 0, 'is-success': valid }"
            >
              <b-datepicker
                v-model="eventDate"
                inline
                :day-names="dayNames"
                :disabled="locationId === null"
                :min-date="minDate"
                :month-names="monthNames"
                :unselectable-days-of-week="offWorkDays"
                :unselectable-dates="unselectableDates"
              >
                <!-- Appointment information time slot field -->
                <validation-provider
                  v-slot="timeProps"
                  class="field"
                  immediate
                  name="eventTimeSlot"
                  rules="required"
                  tag="div"
                >
                  <input v-model="selectedTimeSlot" type="hidden" />
                  <div
                    v-if="
                      locationId !== null &&
                      eventDate !== null &&
                      timeSlots.length > 0
                    "
                    class="columns is-multiline is-mobile is-centered py-2"
                  >
                    <div
                      v-for="(timeSlot, index) in timeSlots"
                      :key="index"
                      class="column is-narrow"
                    >
                      <b-tooltip
                        v-if="timeSlot[1]"
                        type="is-danger"
                        :label="'Cita no disponible'"
                      >
                        <b-button disabled outlined type="is-danger is-dark">
                          {{ timeSlot[0] }}
                        </b-button>
                      </b-tooltip>
                      <b-button
                        v-else
                        type="is-success is-dark"
                        :outlined="
                          selectedTimeSlot === null ||
                          !selectedTimeSlot.equals(timeSlot[0])
                        "
                        @click="selectedTimeSlot = timeSlot[0]"
                      >
                        {{ timeSlot[0] }}
                      </b-button>
                    </div>
                  </div>
                  <p
                    v-if="
                      locationId !== null &&
                      eventDate !== null &&
                      timeSlots.length > 0
                    "
                    class="help"
                    :class="{
                      'is-danger': timeProps.errors.length > 0,
                    }"
                  >
                    {{ timeProps.errors.join(';') }}
                  </p>
                  <p
                    v-else-if="locationId !== null && eventDate !== null"
                    class="help is-danger is-size-6"
                  >
                    {{ timeSlotErrorMessage }}
                  </p>
                </validation-provider>
              </b-datepicker>
            </b-field>
          </validation-provider>
          <div class="divider">{{ $t('appointmentSummary') }}</div>
          <p class="content">{{ $t('verifyDetails') }}</p>
          <b-table
            bordered
            class="mb-5"
            narrowed
            striped
            :columns="[
              {
                cellClass: 'has-text-weight-bold',
                customKey: 'key',
                field: 'key',
                headerClass: 'is-hidden',
              },
              { customKey: 'value', field: 'value', headerClass: 'is-hidden' },
            ]"
            :data="appointmentSummary"
          />
          <validation-provider
            v-slot="{ errors, valid }"
            class="field"
            name="acceptTerms"
            tag="div"
            :rules="{ required: { allowFalse: false } }"
          >
            <b-field
              :message="errors"
              :type="{ 'is-danger': errors.length > 0, 'is-success': valid }"
            >
              <b-checkbox v-model="acceptTerms">
                {{ $t('acceptTerms') }}
              </b-checkbox>
            </b-field>
          </validation-provider>
          <b-field
            :message="
              props.invalid
                ? 'Hay campos inválidos; revise la información'
                : undefined
            "
            :type="{
              'is-danger': props.invalid,
              'is-success': props.valid,
            }"
          >
            <b-button
              native-type="submit"
              size="is-medium"
              :disabled="props.invalid"
              :loading="sending"
              :type="{
                'is-danger': Object.values(props.errors).length > 0,
                'is-success': props.valid,
              }"
            >
              {{ $t('submit') }}
            </b-button>
          </b-field>
        </form>
      </validation-observer>
    </div>
  </section>
</template>

<script lang="ts">
  import delay from 'lodash/delay';
  import range from 'lodash/range';
  import {
    DateTimeFormatter,
    Duration,
    LocalDate,
    LocalDateTime,
    LocalTime,
    ZoneId,
    ZonedDateTime,
    nativeJs,
  } from '@js-joda/core';
  import { TranslateResult } from 'vue-i18n';
  import { ValidationObserver, ValidationProvider, extend } from 'vee-validate';
  /* eslint-disable camelcase */
  import {
    email,
    max_value,
    min_value,
    mimes,
    regex,
    required,
    size,
  } from 'vee-validate/dist/rules';
  import Vue from 'vue';
  /* eslint-enable camelcase */

  // Define rules for use with vee validate.
  extend('email', email);
  extend('maxValue', max_value);
  extend('imageMimes', mimes);
  extend('minValue', min_value);
  extend('phone', regex);
  extend('required', required);
  extend('size', size);

  type LazyDate = null | Date;
  type LazyFile = null | File;
  type LazyNumber = null | number;
  type LazyLocalTime = null | LocalTime;

  enum Collection {
    ForAWalk = 1,
    ElectronicsAndAccesories,
    Entertainment,
    BathAndHome,
    Clothing,
    Other,
  }

  enum Day {
    Sunday = 0,
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
  }

  enum ProductError {
    NoCategory = -1,
    WrongQuality = -2,
  }

  enum Quality {
    Used = 1,
    Good,
    Excellent,
  }

  interface BusinessHours {
    readonly start: LocalTime;
    readonly duration: Duration;
    readonly day: Day;
  }

  interface Category {
    readonly id: number;
    readonly name: string;
    readonly collection: Collection;
    readonly icon: string;
    readonly requiresImage: boolean;
    readonly active: boolean;
  }

  interface CategoryGroup {
    readonly collection: Collection;
    readonly categories: Array<Category>;
  }

  interface KeyValuePair<K, V> {
    readonly key: K;
    readonly value: V;
  }

  interface Location {
    readonly id: number;
    readonly name: string;
    readonly address: string;
    readonly businessHours: Array<BusinessHours>;
  }

  interface Product {
    categoryId: LazyNumber;
    collection: null | Collection;
    image: LazyFile;
    name: string;
    quality: Quality;
    quantity: number;
    requiresImage: boolean;
  }

  interface TimeSlot {
    readonly start: LocalDateTime;
    readonly duration: Duration;
  }

  interface ReservedTimeSlot extends TimeSlot {
    readonly locationId: number;
    readonly confirmed: boolean;
  }

  // Server
  interface ServerBusinessHours {
    readonly id: number;
    readonly start: string;
    readonly duration: string;
    readonly day: Day;
  }

  interface ServerEventShort {
    readonly start: string;
    readonly duration: string;
    readonly locationId: number;
    readonly confirmed: boolean;
  }

  interface ServerLocation {
    readonly id: number;
    readonly name: string;
    readonly address: string;
    readonly businessHours: Array<ServerBusinessHours>;
  }

  interface ServerProduct {
    categoryId: number;
    categoryName: string;
    collection: number;
    image?: string;
    quality: Quality;
    quantity: number;
  }

  const toyAndClothesTimeSlot = {
    start: LocalTime.of(11, 40),
    duration: Duration.ofHours(2).plusMinutes(20),
  };

  const appUrl =
    process.env.NODE_ENV === 'development'
      ? 'http://localhost:9000'
      : 'https://api.entrepeques.com.mx';

  export default Vue.extend({
    // Allow this component to know about the existance of certain components.
    components: { ValidationObserver, ValidationProvider },
    data() {
      const today = new Date();

      return {
        Collection,
        acceptTerms: false,
        apparelQuantities: range(4).map(value =>
          this.$t(`ApparelQuantity.${value + 1}`),
        ),
        categories: [] as Array<Category>,
        categorySearchTerm: '',
        collections: range(6).map(value => this.$t(`Collections.${value + 1}`)),
        contactEmail: '',
        contactName: '',
        contactPhone: '',
        dayNames: range(7).map(value => this.$t(`Days.${value}`)),
        eventDate: null as LazyDate,
        loading: true,
        locations: [] as Array<Location>,
        locationId: null as LazyNumber,
        minDate: new Date(
          today.getFullYear(),
          today.getMonth(),
          today.getDate(),
        ),
        monthNames: range(12).map(value => this.$t(`Months.${value}`)),
        notification: '',
        products: [
          {
            categoryId: null,
            collection: null,
            image: null,
            name: '',
            quality: Quality.Good,
            quantity: 1,
            requiresImage: false,
          },
        ] as Array<Product>,
        qualities: range(3).map(value => this.$t(`Quality.${value + 1}`)),
        reservedTimeSlots: [] as Array<ReservedTimeSlot>,
        searchingCategories: false,
        selectedCategories: [] as Array<Category>,
        selectedTimeSlot: null as LazyLocalTime,
        sending: false,
        toyQuantities: range(4).map(value =>
          this.$t(`ToyQuantity.${value + 1}`),
        ),
      };
    },
    computed: {
      appointmentSummary(): Array<KeyValuePair<TranslateResult, string>> {
        const currentLocationId = this.locationId;
        const currentLocation = this.locations.find(
          location => location.id === currentLocationId,
        );
        const timeFormatter = DateTimeFormatter.ofPattern('HH:mm');

        let selectedTimeSlot = '';
        if (this.selectedTimeSlot !== null) {
          selectedTimeSlot = `${this.selectedTimeSlot.format(
            timeFormatter,
          )} - ${this.selectedTimeSlot
            .plusMinutes(this.eventDuration)
            .format(timeFormatter)}`;
        } else if (this.eventDuration <= 0) {
          selectedTimeSlot = this.timeSlotErrorMessage.toString();
        }

        return [
          { key: this.$t('contactName.label'), value: this.contactName },
          { key: this.$t('contactEmail.label'), value: this.contactEmail },
          { key: this.$t('contactPhone.label'), value: this.contactPhone },
          {
            key: this.$t('totalProducts.label'),
            value: this.totalProducts.toString(),
          },
          {
            key: this.$t('locationId.label'),
            value: currentLocation !== undefined ? currentLocation.name : '',
          },
          {
            key: this.$t('eventDate.label'),
            value: this.eventDate !== null ? this.$d(this.eventDate) : '',
          },
          { key: this.$t('timeSlot.label'), value: selectedTimeSlot },
        ];
      },
      categoryGroups(): Array<CategoryGroup> {
        return this.categories.reduce((groups, category) => {
          if (groups[category.collection - 1] === undefined) {
            groups.push({
              collection: category.collection,
              categories: [],
            });
          }
          groups[category.collection - 1]?.categories.push(category);
          return groups;
        }, [] as Array<CategoryGroup>);
      },
      currentReservedTimeSlots(): Array<TimeSlot> {
        if (this.selectedLocation === undefined) return [];
        if (this.eventDate === null) return [];
        const selectedLocation = this.selectedLocation;
        const selectedDate = LocalDate.from(nativeJs(this.eventDate));

        return this.reservedTimeSlots
          .filter(
            slot =>
              slot.locationId === selectedLocation.id &&
              slot.start.toLocalDate().equals(selectedDate),
          )
          .sort((a, b) => a.start.compareTo(b.start));
      },
      eventDuration(): number {
        const products = this.normalizedTotalProducts;

        if (products <= 1) return 0;
        if (products <= 4) return 20;
        if (products <= 8) return 40;
        if (products <= 12) return 60;
        return 0;
      },
      filteredCategories(): Array<Category> {
        const selectedCategories = this.selectedCategories.map(
          category => category.name,
        );
        const categorySearchTerm = this.categorySearchTerm.toLowerCase();

        if (selectedCategories.length === 0 && categorySearchTerm.length === 0)
          return this.categories;

        let categories = this.categories;
        if (selectedCategories.length > 0) {
          categories = categories.filter(
            category => !selectedCategories.includes(category.name),
          );
        }
        if (categorySearchTerm.length > 0) {
          categories = categories.filter(category =>
            category.name.toLowerCase().includes(categorySearchTerm),
          );
        }

        return categories;
      },
      inactiveCategories(): Array<Category> {
        return this.categories.filter(category => !category.active);
      },
      normalizedTotalProducts(): number {
        let products = 0;

        this.products.forEach(product => {
          if (
            products !== ProductError.NoCategory &&
            products !== ProductError.WrongQuality
          ) {
            if (product.categoryId === null) {
              products = ProductError.NoCategory;
            } else if (product.quality === Quality.Used) {
              products = ProductError.WrongQuality;
            } else if (product.collection === Collection.Clothing) {
              products +=
                product.quantity > 1
                  ? (product.quantity - 1) / 10
                  : product.quantity;
            } else if (product.categoryId === 26 || product.categoryId === 27) {
              products +=
                product.quantity > 1
                  ? ((product.quantity - 1) * 2) / 3
                  : product.quantity;
            } else {
              products += product.quantity;
            }
          }
        });

        return products;
      },
      offWorkDays(): Array<Day> {
        if (this.selectedLocation === undefined) return [];
        return this.selectedLocation.businessHours
          .reduce((offWorkDays, hour) => {
            if (offWorkDays[hour.day] !== undefined)
              delete offWorkDays[hour.day];
            return offWorkDays;
          }, range(7) as Array<Day>)
          .filter(value => value !== undefined);
      },
      selectedDayBusinessHours(): Array<BusinessHours> {
        if (this.selectedLocation === undefined) return [];
        const currentDay =
          this.eventDate !== null ? new Date(this.eventDate).getDay() : 0;
        return this.selectedLocation.businessHours.filter(
          hour => hour.day === currentDay,
        );
      },
      selectedLocation(): Location | undefined {
        if (this.locationId === null) return undefined;
        return this.locations.find(location => location.id === this.locationId);
      },
      timeSlots(): Array<[LocalTime, boolean]> {
        if (
          this.selectedLocation === undefined ||
          this.eventDate === null ||
          this.eventDuration <= 0
        ) {
          return [];
        }
        const reservedSlots = [...this.currentReservedTimeSlots].map(slot => {
          return {
            start: slot.start.toLocalTime(),
            duration: slot.duration,
          };
        });
        if (
          this.products.filter(
            product =>
              product.collection === Collection.Clothing ||
              product.categoryId === 26 ||
              product.categoryId === 27,
          ).length > 0 &&
          this.products.filter(
            product =>
              product.collection !== Collection.Clothing &&
              product.categoryId !== 26 &&
              product.categoryId !== 27,
          ).length === 0
        ) {
          reservedSlots.push(toyAndClothesTimeSlot);
        }
        const timeSlots: Array<[LocalTime, boolean]> = [];
        const today = new Date();
        let now: LazyLocalTime = null;
        if (
          this.eventDate.getFullYear() === today.getFullYear() &&
          this.eventDate.getMonth() === today.getMonth() &&
          this.eventDate.getDate() === today.getDate()
        ) {
          now = LocalTime.now();
        }

        this.selectedDayBusinessHours.forEach(hour => {
          const maxTime = hour.start
            .plus(hour.duration)
            .minusMinutes(this.eventDuration - 1);
          let currentTime = hour.start;

          while (currentTime.isBefore(maxTime)) {
            const notNow = now === null || currentTime.isAfter(now);
            let nextTime = currentTime.plusMinutes(this.eventDuration);
            if (
              notNow &&
              (reservedSlots.length === 0 ||
                nextTime.isBefore(reservedSlots[0].start.plusMinutes(1)))
            ) {
              timeSlots.push([currentTime, false]);
            } else if (reservedSlots.length > 0) {
              const [reserved] = reservedSlots.splice(0, 1);
              if (notNow) timeSlots.push([currentTime, true]);
              nextTime = currentTime.plus(reserved.duration);
              const distance = Duration.between(
                nextTime,
                reserved.start.plus(reserved.duration),
              );
              if (!distance.isZero()) nextTime = nextTime.plus(distance);
            }
            currentTime = nextTime;
          }
        });

        return timeSlots;
      },
      timeSlotErrorMessage(): TranslateResult {
        if (this.normalizedTotalProducts === ProductError.WrongQuality)
          return this.$t('products.invalidQuality');
        if (this.normalizedTotalProducts === ProductError.NoCategory)
          return this.$t('products.invalidCategory');
        if (this.normalizedTotalProducts === 0) return this.$t('products.none');
        if (this.normalizedTotalProducts <= 1)
          return this.$t('products.tooFew');
        if (this.normalizedTotalProducts > 12)
          return this.$t('products.tooMany');
        return this.$t('appointmentsNoneLeft');
      },
      totalProducts(): number {
        let products = 0;

        this.products.forEach(product => {
          products += product.quantity;
        });

        return products;
      },
      unselectableDates(): Array<unknown> {
        return [];
      },
    },
    watch: {
      eventDate(): void {
        this.selectedTimeSlot = null;
      },
      locationId(): void {
        this.eventDate = null;
      },
      normalizedTotalProducts(newValue): void {
        if (newValue <= 1 || newValue > 12) this.selectedTimeSlot = null;
      },
    },
    mounted() {
      this.loading = true;
      Promise.all([
        this.$http.get(`${appUrl}/categories`),
        this.$http.get(`${appUrl}/locations/business-hours`),
        this.$http.get(`${appUrl}/notification`),
        this.$http.get(`${appUrl}/events-short`),
      ])
        .then(([categories, locations, notification, reservedTimeSlots]) => {
          this.categories = categories.data;
          this.locations = (locations.data as Array<ServerLocation>).map(
            location => {
              return {
                id: location.id,
                name: location.name,
                address: location.address,
                businessHours: location.businessHours.map(hour => {
                  return {
                    start: LocalTime.parse(hour.start),
                    duration: Duration.parse(hour.duration),
                    day: hour.day,
                  };
                }),
              };
            },
          );
          this.notification = notification.data;
          this.reservedTimeSlots = (
            reservedTimeSlots.data as Array<ServerEventShort>
          ).map(event => {
            return {
              start: ZonedDateTime.parse(event.start).toLocalDateTime(),
              duration: Duration.parse(event.duration),
              locationId: event.locationId,
              confirmed: event.confirmed,
            };
          });
        })
        .catch(exception => console.error(exception))
        .finally(() => (this.loading = false));
    },
    methods: {
      addProduct(
        categoryId: LazyNumber = null,
        collection: null | Collection = null,
      ): void {
        this.products.push({
          categoryId,
          collection,
          image: null,
          name: '',
          quality: Quality.Good,
          quantity: 1,
          requiresImage: false,
        });
      },
      async encodeFileAsURL(file: File): Promise<string | undefined> {
        return new Promise<string | undefined>((resolve, reject) => {
          const reader = new FileReader();
          reader.onloadend = () => {
            const result = reader.result;
            if (result === null) {
              resolve(undefined);
              return;
            }
            if (result instanceof ArrayBuffer) {
              resolve(undefined);
              return;
            }
            resolve(result);
          };
          reader.onerror = error => {
            reject(error);
          };
          reader.readAsDataURL(file);
        });
      },
      productImageName(index: number): TranslateResult | string {
        const image = this.products[index].image;
        if (image === null) return this.$t('addImage');
        return image.name;
      },
      productQuantityLabel(product: Product, index: number): TranslateResult {
        if (
          product.categoryId === 26 ||
          product.categoryId === 27 ||
          product.collection === Collection.Clothing
        ) {
          return `${this.$t('select')} ${this.$t('productQuantity.label')
            .toString()
            .toLowerCase()} #${index + 1}`;
        }
        return `${this.$t('productQuantity.label')} #${index + 1}`;
      },
      removeProductAt(index: number): void {
        this.products.splice(index, 1);
      },
      async parseProducts(): Promise<Array<ServerProduct>> {
        interface CategoryNames {
          [id: number]: string;
        }
        const categories = this.categories.reduce((out, category) => {
          out[category.id] = category.name;
          return out;
        }, {} as CategoryNames);
        const mapProducts = async (product: Product) => {
          let categoryId = 0;
          let categoryName = '';
          if (product.categoryId !== null) {
            categoryId = product.categoryId;
            categoryName = categories[categoryId];
          }
          let collection = 0;
          if (product.collection !== null) {
            collection = product.collection;
          }
          let image = undefined;
          if (product.requiresImage && product.image !== null) {
            image = await this.encodeFileAsURL(product.image);
          }
          return {
            categoryId,
            categoryName,
            collection,
            image,
            quality: product.quality,
            quantity: product.quantity,
          };
        };

        return await Promise.all(
          this.products.map(product => mapProducts(product)),
        );
      },
      async saveAppointment(): Promise<void> {
        const selectedTimeSlot = this.selectedTimeSlot;
        const selectedLocation = this.selectedLocation;
        if (selectedTimeSlot === null) return;
        if (selectedLocation === undefined) return;

        this.sending = true;
        this.parseProducts()
          .then(products => {
            return this.$http.post(`${appUrl}/appointments`, {
              start: ZonedDateTime.of(
                selectedTimeSlot.atDate(
                  LocalDate.from(nativeJs(this.eventDate)),
                ),
                ZoneId.SYSTEM,
              )
                .withFixedOffsetZone()
                .toString(),
              duration: Duration.ofMinutes(this.eventDuration),
              locationId: selectedLocation.id,
              locationName: selectedLocation.name,
              locationAddress: selectedLocation.address,
              contactEmail: this.contactEmail,
              contactName: this.contactName,
              contactPhone: this.contactPhone,
              products,
            });
          })
          .then(response => {
            return this.$http.get(`${appUrl}/verify-appointment`, {
              params: { id: response.data },
            });
          })
          .then(() => {
            delay(
              (toast, options) => {
                toast.open(options);
              },
              200,
              this.$buefy.toast,
              {
                duration: 10000,
                message:
                  '¡Listo! Tu cita ha quedado agendada. Revisa tu correo electrónico para confirmar la información.',
                type: 'is-success',
              },
            );
          })
          .catch(error => {
            console.error(error);
            // Attempt to gracefully resolve error.
            let message = 'Ha occurido un error; favor de intentar de nuevo';
            if (error.response !== undefined) {
              // Handles 400 error, which occurs when the appointment
              // is now in the past.
              if (error.response.status === 400) {
                message =
                  'La fecha y hora de la cita ya ha pasado; favor de seleccionar una fecha y hora mas reciente';
                // Handles 409 error, which occurs when the conflicts
                // with another; this can happen if the user doesn't refresh
                // the page in a while.
              } else if (error.response.status === 409) {
                message =
                  'La fecha y hora seleccionados ya ha sido reservado; favor de seleccionar otra fecha';
              }
            }

            delay(
              (toast, options) => {
                toast.open(options);
              },
              200,
              this.$buefy.toast,
              {
                duration: 3000,
                message,
                type: 'is-danger',
              },
            );
          })
          .finally(() => (this.sending = false));
      },
      updateCollectionForProductAt(categoryId: number, index: number) {
        const category = this.categories.find(
          category => category.id === categoryId,
        );
        if (category !== undefined) {
          this.products[index].collection = category.collection;
          this.products[index].requiresImage = category.requiresImage;
          this.products[index].quantity = 1;
        }
      },
    },
  });
</script>

<style lang="scss">
  #Home form {
    max-width: 420px;
    .column.is-narrow {
      padding: 0.25rem;
    }
  }
</style>
