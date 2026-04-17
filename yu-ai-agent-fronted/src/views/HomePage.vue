<script setup>
import { useRouter } from "vue-router";

const router = useRouter();

function goTo(path) {
  router.push(path);
}
</script>

<template>
  <div class="home-page">
    <h1>AI 应用中心</h1>
    <p>请选择你要体验的应用</p>
    <div class="home-actions">
      <button @click="goTo('/love-master')">AI 恋爱大师</button>
      <button @click="goTo('/manus')">AI 超级智能体</button>
    </div>
  </div>
</template>
