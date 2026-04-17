import axios from "axios";

const http = axios.create({
  baseURL: "http://localhost:8123/api",
  timeout: 30000,
});

export default http;
