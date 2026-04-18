import axios from "axios";

// 从环境变量中读取 API 地址，如果环境变量没有，再回退到 localhost（方便本地开发）
const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8123/api";

const http = axios.create({
  baseURL: baseURL,
  timeout: 30000,
});

export default http;