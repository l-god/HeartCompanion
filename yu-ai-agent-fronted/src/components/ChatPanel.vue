<script setup>
import { nextTick, ref, watch } from "vue";

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  icon: {
    type: String,
    default: "",
  },
  chatId: {
    type: String,
    default: "",
  },
  messages: {
    type: Array,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  theme: {
    type: String,
    default: "default",
  },
});

const emit = defineEmits(["send"]);

const inputValue = ref("");
const messageListRef = ref(null);
const chatPageStyle = ref({
  "--mx": "50%",
  "--my": "50%",
});

const heartDecorations = Array.from({ length: 7 }, (_, index) => ({
  id: `heart-${index}`,
  left: `${8 + Math.random() * 84}%`,
  delay: `${(Math.random() * 3.2).toFixed(2)}s`,
  duration: `${(4 + Math.random() * 3).toFixed(2)}s`,
  size: `${14 + Math.round(Math.random() * 10)}px`,
}));

function handleSend() {
  const value = inputValue.value.trim();
  if (!value || props.loading) {
    return;
  }
  emit("send", value);
  inputValue.value = "";
}

function handleMouseMove(event) {
  if (props.theme !== "cyber") {
    return;
  }
  const target = event.currentTarget;
  const rect = target.getBoundingClientRect();
  const x = event.clientX - rect.left;
  const y = event.clientY - rect.top;
  chatPageStyle.value = {
    "--mx": `${x}px`,
    "--my": `${y}px`,
  };
}

function handleMouseLeave() {
  if (props.theme !== "cyber") {
    return;
  }
  chatPageStyle.value = {
    "--mx": "50%",
    "--my": "50%",
  };
}

watch(
  () => props.messages.length,
  async () => {
    await nextTick();
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  }
);
</script>

<template>
  <div
    class="chat-page"
    :class="`theme-${theme}`"
    :style="chatPageStyle"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
  >
    <div v-if="theme === 'love'" class="love-hearts">
      <span
        v-for="heart in heartDecorations"
        :key="heart.id"
        class="love-heart"
        :style="{
          left: heart.left,
          animationDelay: heart.delay,
          animationDuration: heart.duration,
          fontSize: heart.size,
        }"
      >
        ❤
      </span>
    </div>
    <div class="chat-header">
      <h2 class="chat-title">
        <img v-if="icon" :src="icon" :alt="title" class="chat-title-icon" />
        <span>{{ title }}</span>
      </h2>
    </div>

    <div ref="messageListRef" class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-item"
        :class="[
          msg.role === 'user' ? 'message-user' : 'message-ai',
          msg.kind ? `message-${msg.kind}` : '',
        ]"
      >
        <div class="message-bubble">
          {{ msg.content }}
        </div>
      </div>
    </div>

    <div class="input-panel">
      <input
        v-model="inputValue"
        type="text"
        placeholder="请输入你的问题..."
        @keyup.enter="handleSend"
      />
      <button :disabled="loading" @click="handleSend">
        {{ loading ? "回复中..." : "发送" }}
      </button>
    </div>
  </div>
</template>
