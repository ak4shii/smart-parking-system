import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis',
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://100.67.245.6:8080',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => `/sps${path}`,
      },
      '/ws': {
        target: 'http://100.67.245.6:8080',
        changeOrigin: true,
        ws: true,
        rewrite: (path) => `/sps${path}`,
      },
    },
  },
})
