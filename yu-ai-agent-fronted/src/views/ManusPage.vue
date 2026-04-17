<script setup>
import { onBeforeUnmount, ref } from "vue";
import { useRouter } from "vue-router";
import ChatPanel from "../components/ChatPanel.vue";
import { buildManusSseUrl } from "../api/chat";

const router = useRouter();
const loading = ref(false);
const eventSourceRef = ref(null);
const messages = ref([]);
const typingTimerRef = ref(null);
let pendingChars = [];
let streamEnded = false;

function closeConnection() {
  if (eventSourceRef.value) {
    eventSourceRef.value.close();
    eventSourceRef.value = null;
  }
}

function resetTypingState() {
  if (typingTimerRef.value) {
    clearInterval(typingTimerRef.value);
    typingTimerRef.value = null;
  }
  pendingChars = [];
  streamEnded = false;
}

function appendUserMessage(content) {
  messages.value.push({
    id: `u-${Date.now()}-${Math.random()}`,
    role: "user",
    content,
  });
}

function appendAiPlaceholder() {
  const aiMessage = {
    id: `a-${Date.now()}-${Math.random()}`,
    role: "ai",
    content: "",
  };
  messages.value.push(aiMessage);
  return aiMessage;
}

function startTyping(aiMessage) {
  if (typingTimerRef.value) {
    return;
  }
  typingTimerRef.value = setInterval(() => {
    if (pendingChars.length === 0) {
      if (streamEnded) {
        loading.value = false;
        resetTypingState();
      }
      return;
    }
    aiMessage.content += pendingChars.shift();
  }, 32);
}

function sendMessage(message) {
  closeConnection();
  resetTypingState();
  appendUserMessage(message);
  const currentAiMessage = appendAiPlaceholder();
  loading.value = true;

  const url = buildManusSseUrl(message);
  const eventSource = new EventSource(url);
  eventSourceRef.value = eventSource;
  startTyping(currentAiMessage);

  eventSource.onmessage = (event) => {
    if (!event.data) {
      return;
    }
    pendingChars.push(...event.data.split(""));
  };

  eventSource.onerror = () => {
    closeConnection();
    streamEnded = true;
    if (!currentAiMessage.content) {
      currentAiMessage.content = "连接中断，请稍后重试。";
      loading.value = false;
      resetTypingState();
    }
  };
}

onBeforeUnmount(() => {
  closeConnection();
  resetTypingState();
});
</script>

<template>
  <div>
    <div class="page-top-bar">
      <button @click="router.push('/')">返回首页</button>
    </div>
    <ChatPanel
      title="AI 超级智能体"
      :messages="messages"
      :loading="loading"
      @send="sendMessage"
    />
  </div>
</template>
