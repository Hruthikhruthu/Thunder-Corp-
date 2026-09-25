import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    // sockjs-client references Node.js `global` — polyfill it for the browser.
    global: 'globalThis',
  },
  build: {
    rollupOptions: {
      output: {
        // Split stable vendor bundles for faster browser caching in production.
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          charts: ['chart.js', 'react-chartjs-2'],
          realtime: ['@stomp/stompjs', 'sockjs-client'],
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Local development proxy mirrors the Docker Nginx /api route.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Proxy SockJS/STOMP development traffic to the Spring Boot backend.
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})
