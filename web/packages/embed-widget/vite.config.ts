import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    lib: {
      entry: 'src/index.ts',
      name: 'AgiChatWidget',
      formats: ['iife'],
      fileName: () => 'agi-chat-widget.js'
    },
    outDir: '../../apps/chat/public/embed',
    emptyOutDir: true
  }
});
