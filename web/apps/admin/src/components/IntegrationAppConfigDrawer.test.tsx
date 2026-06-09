// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { IntegrationAppConfigDrawer } from './IntegrationAppConfigDrawer';

const identityApiMock = vi.hoisted(() => ({
  listIntegrationAppScopes: vi.fn(async () => ({ items: [], total: 0 })),
  listIntegrationAppSecrets: vi.fn(async () => ({ items: [], total: 0 })),
  createIntegrationAppSecret: vi.fn(async () => ({ id: 'secret_1', secretPrefix: 'agi_demo', apiKey: 'agi_demo_full' })),
  deleteIntegrationAppSecret: vi.fn(async () => undefined),
  createIntegrationAppScope: vi.fn(async () => ({ id: 'scope_1', scopeType: 'BOT', scopeId: 'bot_1', permission: 'USE', enabled: true, appId: 'app_archive' })),
  deleteIntegrationAppScope: vi.fn(async () => undefined)
}));

vi.mock('../api/identity', () => identityApiMock);
vi.mock('../api/bots', () => ({
  listBots: vi.fn(async () => ({ items: [{ id: 'bot_1', name: '客服助手', status: 'ENABLED' }], total: 1 }))
}));
vi.mock('../api/knowledge', () => ({
  listKnowledgeBases: vi.fn(async () => ({ items: [{ id: 'kb_1', name: 'FAQ 库', status: 'READY' }], total: 1 }))
}));

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
});

Object.defineProperty(window, 'getComputedStyle', {
  value: vi.fn(() => ({ getPropertyValue: vi.fn(() => ''), overflowX: 'auto', overflowY: 'auto' }))
});

describe('IntegrationAppConfigDrawer', () => {
  it('shows api headers and generates api key', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <IntegrationAppConfigDrawer
          open
          tenantId="tenant_default"
          app={{ id: 'app_archive', code: 'archive-system', name: '档案系统', appType: 'ARCHIVE_SYSTEM', authType: 'API_KEY', status: 'ACTIVE' }}
          onClose={() => undefined}
        />
      </QueryClientProvider>
    );

    expect(await screen.findByText('X-AGI-App-Code')).toBeInTheDocument();
    expect(screen.getByText('archive-system')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /生成 API Key/ }));
    expect(await screen.findByText('agi_demo_full')).toBeInTheDocument();
    expect(identityApiMock.createIntegrationAppSecret).toHaveBeenCalledWith('app_archive', 'tenant_default');
  });
});
