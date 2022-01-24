import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

import Buefy from 'buefy';
import { configure as VeeValidateConfig } from 'vee-validate';
import Vue from 'vue';
import VueI18n from 'vue-i18n';
import axios from 'axios';

import App from '@/App.vue';
import router from '@/router';
import store from '@/store';

import es from '@/messages/es.json';
import en from '@/messages/en.json';
import dateTimeFormats from '@/messages/date_time.json';

import '@/assets/img/icons';
import '@/assets/stylesheets/main.sass';

// Registers font awesome (vue) component with vue.
Vue.component('FontAwesomeIcon', FontAwesomeIcon);

// Register use of Buefy plugin,
// responsible for providing components that follow Bulma specification.
Vue.use(Buefy, {
  defaultIconPack: 'fa',
  defaultIconComponent: 'font-awesome-icon',
});

// Registers vuei18n plugin, responsible for i18n in the app, with vue.
Vue.use(VueI18n);

// Registers axios under http namespace.
Vue.prototype.$http = axios;
Vue.config.productionTip = false;

// Initializes vuei18n plugin with custom options.
const i18n = new VueI18n({
  dateTimeFormats, // Add date time formats.
  fallbackLocale: 'en',
  // Retrieve saved lang from settings or default to spanish
  locale: 'es',
  messages: {
    en,
    es,
  },
  silentFallbackWarn: true, // Do not warn when messages fallbacks to locale.
});

// Add additional configuration that is globally applied to vee validate
VeeValidateConfig({
  classes: {
    valid: 'is-success',
    invalid: 'is-danger',
  },
  // Configure vee-validate to use messages from i18n plugin.
  defaultMessage(field, values) {
    values._field_ = i18n.t(`Fields.${field}`);
    return i18n.t(`Validations.${values._rule_}`, values) as string;
  },
});

// Initalizes vue with App as top level component, i18n, router & store.
new Vue({
  i18n,
  render: h => h(App),
  router,
  store,
}).$mount('#app');
