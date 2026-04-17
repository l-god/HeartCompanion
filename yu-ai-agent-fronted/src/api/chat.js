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
