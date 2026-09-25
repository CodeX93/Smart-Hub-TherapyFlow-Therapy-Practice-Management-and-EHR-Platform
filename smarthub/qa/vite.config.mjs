import { createRequire } from 'node:module';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { lifecyclePlugin } from './lifecycle-plugin.mjs';
const frontend = process.env.QA_FRONTEND_ROOT;
if (!frontend) throw new Error('QA_FRONTEND_ROOT is required.');
const requireFrontend = createRequire(path.join(frontend, 'package.json'));
const react = (await import(pathToFileURL(requireFrontend.resolve('@vitejs/plugin-react')).href)).default;
const tailwind = (await import(pathToFileURL(requireFrontend.resolve('@tailwindcss/vite')).href)).default;
export default {
  root: frontend, envDir: false, plugins: [lifecyclePlugin(frontend), react(), tailwind()],
  cacheDir: path.join(process.env.QA_RUN_DIR, 'vite-cache'),
  resolve: { alias: { '@': path.join(frontend, 'src') } },
  server: { host: '127.0.0.1', port: Number(process.env.QA_FRONTEND_PORT), strictPort: true,
    proxy: { '/api': { target: process.env.QA_BACKEND_URL, changeOrigin: true }, '/ws': { target: process.env.QA_BACKEND_URL, ws: true } } },
};
