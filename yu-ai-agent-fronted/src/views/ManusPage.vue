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
let currentThinkingMessage = null;  // 当前思考消息（用于累积）
let currentFinalMessage = null;      // 当前最终结果消息

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
  currentThinkingMessage = null;
  currentFinalMessage = null;
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
    thinkingSteps: [],  // 存储思考步骤
    finalResult: "",     // 存储最终结果
  };
  messages.value.push(aiMessage);
  return aiMessage;
}

// 添加思考步骤（显示为灰色小字）
function addThinkingStep(stepContent) {
  // 如果没有当前思考消息，创建一个新的
  if (!currentThinkingMessage) {
    currentThinkingMessage = appendAiMessage("thinking");
    currentThinkingMessage.thinkingSteps = [];
  }
  // 添加思考步骤
  currentThinkingMessage.thinkingSteps.push(stepContent);
  // 同时更新显示内容（兼容旧的显示方式）
  currentThinkingMessage.content += `💭 ${stepContent}\n`;
}

// 添加最终结果
function setFinalResult(resultContent) {
  // 如果没有当前最终消息，创建一个
  if (!currentFinalMessage) {
    currentFinalMessage = appendAiMessage("final");
  }
  // 设置最终结果
  currentFinalMessage.finalResult = resultContent;
  currentFinalMessage.content = resultContent;
}

function handleStreamLine(line) {
  const cleanLine = line.trim();
  if (!cleanLine) {
    return;
  }

  // 处理 THINK: 开头的内容（思考过程）
  if (cleanLine.startsWith("THINK:")) {
    const thinkContent = cleanLine.slice(6).trim();
    if (thinkContent) {
      addThinkingStep(thinkContent);
    }
    return;
  }

  // 处理 FINAL: 开头的内容（最终结果）
  if (cleanLine.startsWith("FINAL:")) {
    const finalContent = cleanLine.slice(6).trim();
    if (finalContent) {
      setFinalResult(finalContent);
    }
    return;
  }

  // 普通内容也作为最终结果处理
  if (cleanLine) {
    // 如果已经有最终消息，追加内容；否则创建新的
    if (currentFinalMessage) {
      currentFinalMessage.finalResult += `\n${cleanLine}`;
      currentFinalMessage.content += `\n${cleanLine}`;
    } else {
      setFinalResult(cleanLine);
    }
  }
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
    // 如果没有任何消息，显示错误提示
    if (!currentThinkingMessage && !currentFinalMessage) {
      const errorMsg = appendAiMessage("final");
      errorMsg.finalResult = "连接中断，请稍后重试。";
      errorMsg.content = "连接中断，请稍后重试。";
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
      setFinalResult(result.data || "暂无响应");
    } else if (currentMode.value === "rag") {
      result = await chatWithManusRag(message, chatId.value);
      setFinalResult(result.data || "暂无响应");
    } else if (currentMode.value === "tools") {
      result = await chatWithManusTools(message, chatId.value);
      setFinalResult(result.data || "暂无响应");
    } else if (currentMode.value === "mcp") {
      result = await chatWithManusMcp(message, chatId.value);
      setFinalResult(result.data || "暂无响应");
    } else if (currentMode.value === "report") {
      result = await chatWithManusReport(message, chatId.value);
      setFinalResult(formatReport(result.data));
    }
  } catch (error) {
    setFinalResult("请求失败，请检查后端服务是否已启动。");
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
    >
      <!-- 自定义消息渲染模板 -->
      <template #message="{ message }">
        <div v-if="message.role === 'ai'" class="ai-message-custom">
          <!-- 思考过程：灰色小字 -->
          <div v-if="message.thinkingSteps && message.thinkingSteps.length > 0" class="thinking-steps">
            <div class="thinking-label">💭 思考过程</div>
            <div v-for="(step, idx) in message.thinkingSteps" :key="idx" class="thinking-step">
              {{ step }}
            </div>
          </div>
          <!-- 最终结果：正常字体 -->
          <div v-if="message.finalResult" class="final-result">
            <div class="final-label">📝 回答</div>
            <div class="final-content">{{ message.finalResult }}</div>
          </div>
          <!-- 兼容旧格式 -->
          <div v-else-if="message.content" class="final-content">
            {{ message.content }}
          </div>
        </div>
        <div v-else class="user-message">
          {{ message.content }}
        </div>
      </template>
    </ChatPanel>
  </div>
</template>

<style scoped>
/* 思考过程样式 - 灰色小字 */
.thinking-steps {
  background: #f5f5f5;
  border-left: 3px solid #888;
  padding: 8px 12px;
  margin: 8px 0;
  border-radius: 6px;
}

.thinking-label {
  font-size: 11px;
  color: #999;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.thinking-step {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
  margin: 4px 0;
  padding-left: 12px;
  position: relative;
}

.thinking-step::before {
  content: "•";
  position: absolute;
  left: 0;
  color: #999;
}

/* 最终结果样式 - 正常字体 */
.final-result {
  background: white;
  padding: 12px 16px;
  margin: 8px 0;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.final-label {
  font-size: 12px;
  color: #667eea;
  margin-bottom: 8px;
  font-weight: 500;
}

.final-content {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 用户消息样式 */
.user-message {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 10px 16px;
  border-radius: 18px;
  max-width: 70%;
  margin-left: auto;
}

/* AI 消息容器 */
.ai-message-custom {
  max-width: 85%;
  margin-right: auto;
}
</style>