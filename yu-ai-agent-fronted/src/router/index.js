import { createRouter, createWebHistory } from "vue-router";
import HomePage from "../views/HomePage.vue";
import LoveMasterPage from "../views/LoveMasterPage.vue";
import ManusPage from "../views/ManusPage.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomePage,
    },
    {
      path: "/love-master",
      name: "love-master",
      component: LoveMasterPage,
    },
    {
      path: "/manus",
      name: "manus",
      component: ManusPage,
    },
  ],
});

export default router;
