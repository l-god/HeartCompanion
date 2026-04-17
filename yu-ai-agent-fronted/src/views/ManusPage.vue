<script setup>
import { onBeforeUnmount, ref } from "vue";
import { useRouter } from "vue-router";
import ChatPanel from "../components/ChatPanel.vue";
import { buildManusSseUrl } from "../api/chat";

const router = useRouter();
const loading = ref(false);
const eventSourceRef = ref(null);
const messages = ref([]);
let lineBuffer = "";
let thinkingMessageRef = null;
let finalMessageRef = null;

function closeConnection() {
  if (eventSourceRef.value) {
    eventSourceRef.value.close();
    eventSourceRef.value = null;
  }
}

function resetStreamState() {
  lineBuffer = "";
  thinkingMessageRef = null;
  finalMessageRef = null;
}

function appendUserMessage(content) {
  messages.value.push({
    id: `u-${Date.now()}-${Math.random()}`,
    role: "user",
    content,
  });
}

function appendAiMessage(kind, content = "") {
  const aiMessage = {
    id: `a-${Date.now()}-${Math.random()}`,
    role: "ai",
    kind,
    content,
  };
  messages.value.push(aiMessage);
  return aiMessage;
}

function appendThinking(text) {
  if (!thinkingMessageRef) {
    thinkingMessageRef = appendAiMessage("thinking");
  }
  thinkingMessageRef.content += `${text}\n`;
}

function appendFinal(text) {
  if (!finalMessageRef) {
    finalMessageRef = appendAiMessage("final");
  }
  finalMessageRef.content += `${text}\n`;
}

function handleStreamLine(line) {
  const cleanLine = line.trim();
  if (!cleanLine) {
    return;
  }
  if (cleanLine.startsWith("THINK:")) {
    appendThinking(cleanLine.slice(6).trim());
    return;
  }
  if (cleanLine.startsWith("FINAL:")) {
    appendFinal(cleanLine.slice(6).trim());
    return;
  }
  appendFinal(cleanLine);
}

function sendMessage(message) {
  closeConnection();
  resetStreamState();
  appendUserMessage(message);
  loading.value = true;

  const url = buildManusSseUrl(message);
  const eventSource = new EventSource(url);
  eventSourceRef.value = eventSource;

  eventSource.onmessage = (event) => {
    if (!event.data) {
      return;
    }
    for (const char of event.data) {
      if (char === "\n") {
        handleStreamLine(lineBuffer);
        lineBuffer = "";
      } else {
        lineBuffer += char;
      }
    }
  };

  eventSource.onerror = () => {
    closeConnection();
    if (lineBuffer.trim()) {
      handleStreamLine(lineBuffer);
      lineBuffer = "";
    }
    if (!thinkingMessageRef && !finalMessageRef) {
      appendAiMessage("final", "连接中断，请稍后重试。");
    }
    loading.value = false;
    resetStreamState();
  };
}

onBeforeUnmount(() => {
  closeConnection();
  resetStreamState();
});
</script>

<template>
  <div>
    <div class="page-top-bar">
      <button @click="router.push('/')">返回首页</button>
    </div>
    <ChatPanel
      title="AI 超级智能体"
      icon="/icons/super-agent.png"
      :messages="messages"
      :loading="loading"
      theme="cyber"
      @send="sendMessage"
    />
  </div>
</template>
