import http from "./http";

function buildSseUrl(path, params) {
  const base = http.defaults.baseURL || "";
  const url = new URL(`${base}${path}`);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.set(key, value);
    }
  });
  return url.toString();
}

export function buildLoveSseUrl(message, chatId) {
  return buildSseUrl("/ai/love_app/chat/sse", { message, chatId });
}

export function buildManusSseUrl(message) {
  return buildSseUrl("/ai/manus/chat", { message });
}

export function chatWithLoveSync(message, chatId) {
  return http.get("/ai/love_app/chat/sync", {
    params: { message, chatId },
  });
}

export function chatWithLoveRag(message, chatId) {
  return http.get("/ai/love_app/chat/rag", {
    params: { message, chatId },
  });
}

export function chatWithLoveTools(message, chatId) {
  return http.get("/ai/love_app/chat/tools", {
    params: { message, chatId },
  });
}

export function chatWithLoveMcp(message, chatId) {
  return http.get("/ai/love_app/chat/mcp", {
    params: { message, chatId },
  });
}

export function chatWithLoveReport(message, chatId) {
  return http.get("/ai/love_app/report", {
    params: { message, chatId },
  });
}

export function chatWithManusSync(message, chatId) {
  return http.get("/ai/manus/chat/sync", {
    params: { message, chatId },
  });
}

export function chatWithManusRag(message, chatId) {
  return http.get("/ai/manus/chat/rag", {
    params: { message, chatId },
  });
}

export function chatWithManusTools(message, chatId) {
  return http.get("/ai/manus/chat/tools", {
    params: { message, chatId },
  });
}

export function chatWithManusMcp(message, chatId) {
  return http.get("/ai/manus/chat/mcp", {
    params: { message, chatId },
  });
}

export function chatWithManusReport(message, chatId) {
  return http.get("/ai/manus/report", {
    params: { message, chatId },
  });
}
