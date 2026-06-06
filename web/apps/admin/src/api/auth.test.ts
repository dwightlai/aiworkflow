// @vitest-environment jsdom

import { afterEach, describe, expect, it, vi } from 'vitest';
import { clearAuthSession, getAuthSession, login, logout, me } from './auth';
import { listUsers } from './identity';

describe('auth api', () => {
  afterEach(() => {
    localStorage.clear();
    vi.unstubAllGlobals();
  });

  it('stores login tokens and sends bearer token on admin requests', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === '/api/auth/login') {
        return jsonResponse({
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
          expiresIn: 3600,
          user: { id: 'user_admin', username: 'admin', displayName: '管理员', roleIds: ['platform_admin'] }
        });
      }
      if (url === '/api/auth/admin/users') {
        expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer access-token');
        return jsonResponse({ items: [], total: 0 });
      }
      return jsonResponse(null, 404);
    });
    vi.stubGlobal('fetch', fetchMock);

    await login('admin', 'admin123');
    expect(getAuthSession()?.accessToken).toBe('access-token');

    await listUsers();

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/users', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer access-token' })
    }));
  });

  it('loads current user and clears session on logout', async () => {
    localStorage.setItem('agi_admin_auth', JSON.stringify({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      user: { id: 'user_admin', username: 'admin', displayName: '管理员', roleIds: ['platform_admin'] }
    }));
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/auth/me') {
        return jsonResponse({ id: 'user_admin', username: 'admin', displayName: '管理员', roleIds: ['platform_admin'] });
      }
      if (url === '/api/auth/logout') {
        return jsonResponse(null);
      }
      return jsonResponse(null, 404);
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(me()).resolves.toMatchObject({ username: 'admin' });
    await logout();

    expect(getAuthSession()).toBeNull();
  });

  it('can clear a malformed stored session', () => {
    localStorage.setItem('agi_admin_auth', '{bad json');
    expect(getAuthSession()).toBeNull();
    clearAuthSession();
    expect(localStorage.getItem('agi_admin_auth')).toBeNull();
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
