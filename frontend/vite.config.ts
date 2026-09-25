import path from "path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    host: true, // Allow external connections
    port: 3000,
    allowedHosts: [
      '.ngrok-free.dev',
      '.ngrok.io',
      '.ngrok.app',
    ],
  },
})
