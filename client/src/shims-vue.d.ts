import axios from 'axios';
import Vue, { VueConstructor } from 'vue';

declare module 'vue/types/vue' {
  interface Vue {
    $http: typeof axios;
  }

  interface VueConstructor {
    $http: typeof axios;
  }
}

declare module 'vue/type/options' {
  interface ComponentOptions<V extends Vue> {
    $http?: typeof axios;
  }
}
