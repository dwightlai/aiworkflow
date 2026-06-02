import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@aiworkflow/workflow-schema': fileURLToPath(new URL('../../packages/workflow-schema/src/index.ts', import.meta.url)),
      '@aiworkflow/workflow-designer-core': fileURLToPath(new URL('../../packages/workflow-designer-core/src/index.ts', import.meta.url)),
      '@aiworkflow/workflow-designer-react': fileURLToPath(new URL('../../packages/workflow-designer-react/src/index.tsx', import.meta.url))
    }
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/openapi': 'http://localhost:8080'
    }
  }
});
