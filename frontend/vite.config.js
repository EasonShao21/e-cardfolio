import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  // [AI assisted chat-015] 讓瀏覽器與 Spring API 共用來源的 Session／CSRF Cookie。
  server: {
    host: '0.0.0.0',
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
  },
})
