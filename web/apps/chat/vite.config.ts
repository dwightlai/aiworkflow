import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

export default defineConfig({
  base: '/chat/',
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:18080'
    }
  }
});
