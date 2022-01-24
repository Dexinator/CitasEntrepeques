import Vue from 'vue';
import VueRouter, { RouteConfig } from 'vue-router';
import Calendar from '@/views/Calendar.vue';
import Home from '@/components/Home.vue';
import Protected from '@/views/Protected.vue';
import Settings from '@/views/Settings.vue';

Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
  {
    path: '/',
    name: 'Home',
    component: Home,
  },
  {
    alias: '/calendario',
    path: '/calendar',
    name: 'Calendar',
    component: Calendar,
  },
  {
    alias: '/calendario-empleados',
    path: '/calendar-employees',
    name: 'Protected',
    component: Protected,
  },
  {
    alias: '/configuracion',
    path: '/settings',
    name: 'Settings',
    component: Settings,
  },
  {
    redirect: { name: 'Home' },
    path: '*',
  },
];

const router = new VueRouter({
  linkExactActiveClass: 'is-active',
  mode: 'history',
  base: process.env.BASE_URL,
  routes,
});

export default router;
