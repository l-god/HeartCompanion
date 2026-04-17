<script setup>
import { useRouter } from "vue-router";

const router = useRouter();
const appCards = [
  {
    title: "心语伴",
    subtitle: "温柔陪伴式恋爱咨询",
    icon: "/icons/heart-chat.png",
    path: "/love-master",
    type: "love",
  },
  {
    title: "AI 超级智能体",
    subtitle: "赛博风多工具任务助手",
    icon: "/icons/super-agent.png",
    path: "/manus",
    type: "cyber",
  },
];
const particles = Array.from({ length: 26 }, (_, index) => ({
  id: `p-${index}`,
  left: `${Math.random() * 100}%`,
  delay: `${(Math.random() * 6).toFixed(2)}s`,
  duration: `${(10 + Math.random() * 9).toFixed(2)}s`,
  size: `${2 + Math.round(Math.random() * 4)}px`,
  opacity: (0.2 + Math.random() * 0.4).toFixed(2),
}));

function goTo(path) {
  router.push(path);
}
</script>

<template>
  <div class="home-page">
    <div class="home-particles" aria-hidden="true">
      <span
        v-for="particle in particles"
        :key="particle.id"
        class="home-particle"
        :style="{
          left: particle.left,
          animationDelay: particle.delay,
          animationDuration: particle.duration,
          width: particle.size,
          height: particle.size,
          opacity: particle.opacity,
        }"
      />
    </div>
    <h1>AI 应用中心</h1>
    <p>请选择你要体验的应用</p>
    <div class="home-card-grid">
      <button
        v-for="card in appCards"
        :key="card.path"
        class="home-card"
        :class="`home-card-${card.type}`"
        @click="goTo(card.path)"
      >
        <img :src="card.icon" :alt="card.title" class="home-card-icon" />
        <span class="home-card-title">{{ card.title }}</span>
        <span class="home-card-subtitle">{{ card.subtitle }}</span>
      </button>
    </div>
  </div>
</template>
