<script setup>
import { onBeforeUnmount, ref } from "vue";
import { useRouter } from "vue-router";
import ChatPanel from "../components/ChatPanel.vue";
import {
  buildManusSseUrl,
  chatWithManusMcp,
  chatWithManusRag,
  chatWithManusReport,
  chatWithManusSync,
  chatWithManusTools,
} from "../api/chat";

const router = useRouter();
const loading = ref(false);
const eventSourceRef = ref(null);
const messages = ref([]);
const chatId = ref(createChatId());
const currentMode = ref("agent");
const manusModes = [
  { key: "agent", label: "智能体执行" },
  { key: "sync", label: "普通对话" },
  { key: "rag", label: "知识增强" },
  { key: "tools", label: "工具助手" },
  { key: "mcp", label: "MCP 助手" },
  { key: "report", label: "报告生成" },
];
let lineBuffer = "";
let thinkingMessageRef = null;
let finalMessageRef = null;

function createChatId() {
  return `manus-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

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
  if (currentMode.value !== "agent") {
    sendBySyncMode(message);
    return;
  }
  sendByAgentMode(message);
}

function sendByAgentMode(message) {
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

function formatReport(reportData) {
  if (!reportData || typeof reportData !== "object") {
    return "报告生成失败，请稍后重试。";
  }
  const title = reportData.title || "报告";
  const suggestions = Array.isArray(reportData.suggestions)
    ? reportData.suggestions
    : [];
  const lines = suggestions.map((item, index) => `${index + 1}. ${item}`);
  return `${title}\n\n${lines.join("\n")}`;
}

async function sendBySyncMode(message) {
  closeConnection();
  resetStreamState();
  appendUserMessage(message);
  loading.value = true;
  try {
    let result;
    if (currentMode.value === "sync") {
      result = await chatWithManusSync(message, chatId.value);
      appendAiMessage("final", result.data || "暂无响应");
    } else if (currentMode.value === "rag") {
      result = await chatWithManusRag(message, chatId.value);
      appendAiMessage("final", result.data || "暂无响应");
    } else if (currentMode.value === "tools") {
      result = await chatWithManusTools(message, chatId.value);
      appendAiMessage("final", result.data || "暂无响应");
    } else if (currentMode.value === "mcp") {
      result = await chatWithManusMcp(message, chatId.value);
      appendAiMessage("final", result.data || "暂无响应");
    } else if (currentMode.value === "report") {
      result = await chatWithManusReport(message, chatId.value);
      appendAiMessage("final", formatReport(result.data));
    }
  } catch (error) {
    appendAiMessage("final", "请求失败，请检查后端服务是否已启动。");
  } finally {
    loading.value = false;
  }
}

function switchMode(modeKey) {
  currentMode.value = modeKey;
  closeConnection();
  resetStreamState();
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
    <div class="mode-switch-bar mode-switch-bar-cyber">
      <button
        v-for="mode in manusModes"
        :key="mode.key"
        class="mode-switch-btn mode-switch-btn-cyber"
        :class="{ 'mode-switch-btn-active': currentMode === mode.key }"
        @click="switchMode(mode.key)"
      >
        {{ mode.label }}
      </button>
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
