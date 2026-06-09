// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { IntegrationAppsPage } from './IntegrationAppsPage';

vi.mock('../../api/identity', () => ({
  listIntegrationApps: vi.fn(async () => ({
    items: [{ id: 'app_archive', code: 'archive-system', name: '档案系统', appType: 'ARCHIVE_SYSTEM', authType: 'API_KEY', status: 'ACTIVE' }],
    total: 1
  })),
  createIntegrationApp: vi.fn(),
  updateIntegrationAppStatus: vi.fn(),
  deleteIntegrationApp: vi.fn()
}));

vi.mock('../../api/auth', () => ({
  resolveIdentityTenantId: (tenantId?: string) => tenantId ?? 'tenant_default'
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

describe('IntegrationAppsPage', () => {
  it('renders integration apps list', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <IntegrationAppsPage />
      </QueryClientProvider>
    );

    expect(await screen.findByText('第三方应用')).toBeInTheDocument();
    expect(await screen.findByText('档案系统')).toBeInTheDocument();
  });
});
