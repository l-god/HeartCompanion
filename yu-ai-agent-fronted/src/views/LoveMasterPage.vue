<script setup>
import { onBeforeUnmount, ref } from "vue";
import { useRouter } from "vue-router";
import ChatPanel from "../components/ChatPanel.vue";
import {
  buildLoveSseUrl,
  chatWithLoveMcp,
  chatWithLoveRag,
  chatWithLoveReport,
  chatWithLoveSync,
  chatWithLoveTools,
} from "../api/chat";

const router = useRouter();
const loading = ref(false);
const eventSourceRef = ref(null);
const messages = ref([]);
const chatId = ref(createChatId());
const typingTimerRef = ref(null);
const currentMode = ref("chat");
let pendingChars = [];
let streamEnded = false;
const loveModes = [
  { key: "chat", label: "陪伴对话" },
  { key: "rag", label: "知识增强" },
  { key: "tools", label: "工具助手" },
  { key: "mcp", label: "MCP 助手" },
  { key: "report", label: "恋爱报告" },
];

function createChatId() {
  return `chat-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

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

function appendAiMessage(content) {
  messages.value.push({
    id: `a-${Date.now()}-${Math.random()}`,
    role: "ai",
    content,
  });
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
  if (currentMode.value === "chat") {
    sendBySse(message);
    return;
  }
  sendBySyncMode(message);
}

function sendBySse(message) {
  closeConnection();
  resetTypingState();
  appendUserMessage(message);
  const currentAiMessage = appendAiPlaceholder();
  loading.value = true;

  const url = buildLoveSseUrl(message, chatId.value);
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

function formatReport(reportData) {
  if (!reportData || typeof reportData !== "object") {
    return "报告生成失败，请稍后重试。";
  }
  const title = reportData.title || "恋爱报告";
  const suggestions = Array.isArray(reportData.suggestions)
    ? reportData.suggestions
    : [];
  const lines = suggestions.map((item, index) => `${index + 1}. ${item}`);
  return `${title}\n\n${lines.join("\n")}`;
}

async function sendBySyncMode(message) {
  closeConnection();
  resetTypingState();
  appendUserMessage(message);
  loading.value = true;
  try {
    let result;
    if (currentMode.value === "rag") {
      result = await chatWithLoveRag(message, chatId.value);
      appendAiMessage(result.data || "暂无响应");
    } else if (currentMode.value === "tools") {
      result = await chatWithLoveTools(message, chatId.value);
      appendAiMessage(result.data || "暂无响应");
    } else if (currentMode.value === "mcp") {
      result = await chatWithLoveMcp(message, chatId.value);
      appendAiMessage(result.data || "暂无响应");
    } else if (currentMode.value === "report") {
      result = await chatWithLoveReport(message, chatId.value);
      appendAiMessage(formatReport(result.data));
    } else {
      result = await chatWithLoveSync(message, chatId.value);
      appendAiMessage(result.data || "暂无响应");
    }
  } catch (error) {
    appendAiMessage("请求失败，请检查后端服务是否已启动。");
  } finally {
    loading.value = false;
  }
}

function switchMode(modeKey) {
  currentMode.value = modeKey;
  closeConnection();
  resetTypingState();
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
    <div class="mode-switch-bar">
      <button
        v-for="mode in loveModes"
        :key="mode.key"
        class="mode-switch-btn"
        :class="{ 'mode-switch-btn-active': currentMode === mode.key }"
        @click="switchMode(mode.key)"
      >
        {{ mode.label }}
      </button>
    </div>
    <ChatPanel
      title="心语伴"
      icon="/icons/heart-chat.png"
      :chat-id="chatId"
      :messages="messages"
      :loading="loading"
      theme="love"
      @send="sendMessage"
    />
  </div>
</template>
