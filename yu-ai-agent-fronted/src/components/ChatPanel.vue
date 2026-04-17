<script setup>
import { nextTick, ref, watch } from "vue";

const props = defineProps({
  title: {
    type: String,
    required: true,
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
});

const emit = defineEmits(["send"]);

const inputValue = ref("");
const messageListRef = ref(null);

function handleSend() {
  const value = inputValue.value.trim();
  if (!value || props.loading) {
    return;
  }
  emit("send", value);
  inputValue.value = "";
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
  <div class="chat-page">
    <div class="chat-header">
      <h2>{{ title }}</h2>
      <p v-if="chatId">聊天室 ID：{{ chatId }}</p>
    </div>

    <div ref="messageListRef" class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-item"
        :class="msg.role === 'user' ? 'message-user' : 'message-ai'"
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
