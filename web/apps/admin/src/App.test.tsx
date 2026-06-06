// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';

vi.mock('./pages/DashboardPage', () => ({
  DashboardPage: () => <div>工作台内容</div>
}));

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
});

describe('App authentication', () => {
  afterEach(() => {
    localStorage.clear();
    vi.unstubAllGlobals();
    window.history.pushState(null, '', '/');
  });

  it('requires login and allows logout', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/auth/login') {
        return jsonResponse({
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
          expiresIn: 3600,
          user: { id: 'user_admin', username: 'admin', displayName: '管理员', roleIds: ['platform_admin'] }
        });
      }
      if (url === '/api/auth/logout') {
        return jsonResponse(null);
      }
      return jsonResponse(null, 404);
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    expect(await screen.findByText('登录 AIFlow 管理端')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('用户名'), 'admin');
    await userEvent.type(screen.getByLabelText('密码'), 'admin123');
    await userEvent.click(screen.getByRole('button', { name: '登录' }));

    expect(await screen.findByText('工作台内容')).toBeInTheDocument();
    expect(screen.getByText('管理员')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /退出登录/ }));

    await waitFor(() => expect(screen.getByText('登录 AIFlow 管理端')).toBeInTheDocument());
  });
});

function jsonResponse(data: unknown, status = 200) {
  return new Response(JSON.stringify({
    success: status >= 200 && status < 300,
    data,
    error: status >= 200 && status < 300 ? null : { message: 'missing mock' }
  }), {
    status,
    headers: { 'Content-Type': 'application/json' }
  });
}
