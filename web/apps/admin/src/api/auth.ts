import type { ApiEnvelope } from './identity';

export const AUTH_STORAGE_KEY = 'agi_admin_auth';
export const DEFAULT_TENANT_ID = 'tenant_default';

export interface AuthUser {
  id: string;
  username: string;
  tenantId?: string;
  displayName: string;
  userType: string;
  sortOrder?: number;
  organizationIds: string[];
  activeOrganizationId?: string | null;
  unitIds: string[];
  activeUnitId?: string | null;
  departmentIds: string[];
  roleIds: string[];
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  expiresIn?: number;
  expiresAt?: number;
  user: AuthUser;
}

const memoryStorage = new Map<string, string>();

export const AUTH_UNAUTHORIZED_EVENT = 'agi:auth:unauthorized';

let unauthorizedHandler: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

export function notifyUnauthorized() {
  clearAuthSession();
  unauthorizedHandler?.();
  window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT));
}

function isUnauthorizedResponse(status: number, code?: string | null) {
  if (status === 401 || status === 403) {
    return true;
  }
  return code === 'API_AUTH_REQUIRED'
    || code === 'CHAT_AUTH_REQUIRED'
    || code === 'AUTH_TOKEN_INVALID'
    || code === 'AUTH_TOKEN_EXPIRED';
}

export function isPlatformOperator(user?: AuthUser | null): boolean {
  return user?.tenantId === DEFAULT_TENANT_ID && (user.roleIds?.includes('platform_admin') ?? false);
}

export function resolveIdentityTenantId(explicitTenantId?: string): string {
  if (explicitTenantId) {
    return explicitTenantId;
  }
  return getAuthSession()?.user.tenantId ?? DEFAULT_TENANT_ID;
}

export function getAuthSession(): AuthSession | null {
  const raw = getStorageItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<AuthSession>;
    if (!parsed.accessToken || !parsed.refreshToken || !parsed.user) {
      clearAuthSession();
      return null;
    }
    const expiresAt = resolveAccessTokenExpiry(parsed);
    if (expiresAt != null && Date.now() >= expiresAt) {
      clearAuthSession();
      return null;
    }
    return parsed as AuthSession;
  } catch {
    clearAuthSession();
    return null;
  }
}

export function setAuthSession(session: AuthSession) {
  const expiresAt = session.expiresAt
    ?? (session.expiresIn ? Date.now() + session.expiresIn * 1000 : resolveAccessTokenExpiry(session));
  setStorageItem(AUTH_STORAGE_KEY, JSON.stringify({
    ...session,
    ...(expiresAt != null ? { expiresAt } : {})
  }));
}

export function clearAuthSession() {
  removeStorageItem(AUTH_STORAGE_KEY);
}

export async function login(username: string, password: string, tenantCode?: string): Promise<AuthSession> {
  const session = await requestJson<AuthSession>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password, tenantCode: tenantCode || undefined })
  }, { skipAuth: true });
  setAuthSession(session);
  return session;
}

export async function me(): Promise<AuthUser> {
  return requestJson<AuthUser>('/api/auth/me');
}

export async function logout(): Promise<void> {
  const session = getAuthSession();
  try {
    if (session?.refreshToken) {
      await requestJson<void>('/api/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken: session.refreshToken })
      });
    }
  } finally {
    clearAuthSession();
  }
}

export async function requestJson<T>(
  url: string,
  init?: RequestInit,
  options: { jsonHeaders?: boolean; skipAuth?: boolean } = {}
): Promise<T> {
  const requestInit = withRequestHeaders(init, options);
  const response = requestInit ? await fetch(url, requestInit) : await fetch(url);
  const envelope = await response.json() as ApiEnvelope<T>;
  if (!options.skipAuth && isUnauthorizedResponse(response.status, envelope.error?.code)) {
    notifyUnauthorized();
    throw new Error(envelope.error?.message ?? '登录已失效，请重新登录');
  }
  if (!response.ok || !envelope.success) {
    throw new Error(envelope.error?.message ?? `Request failed: ${response.status}`);
  }
  return envelope.data;
}

function withRequestHeaders(
  init?: RequestInit,
  options: { jsonHeaders?: boolean; skipAuth?: boolean } = {}
): RequestInit | undefined {
  const headers = normalizeHeaders(init?.headers);
  if (options.jsonHeaders !== false && init?.body !== undefined && !hasHeader(headers, 'Content-Type')) {
    headers['Content-Type'] = 'application/json';
  }
  const session = options.skipAuth ? null : getAuthSession();
  if (session?.accessToken && !hasHeader(headers, 'Authorization')) {
    headers.Authorization = `Bearer ${session.accessToken}`;
  }
  if (!init && Object.keys(headers).length === 0) {
    return undefined;
  }
  return {
    ...init,
    headers
  };
}

function normalizeHeaders(headers?: HeadersInit): Record<string, string> {
  if (!headers) {
    return {};
  }
  if (headers instanceof Headers) {
    const normalized: Record<string, string> = {};
    headers.forEach((value, key) => {
      normalized[key] = value;
    });
    return normalized;
  }
  if (Array.isArray(headers)) {
    return Object.fromEntries(headers);
  }
  return { ...headers };
}

function hasHeader(headers: Record<string, string>, name: string) {
  const normalizedName = name.toLowerCase();
  return Object.keys(headers).some((key) => key.toLowerCase() === normalizedName);
}

function resolveAccessTokenExpiry(session: Partial<AuthSession>): number | null {
  if (typeof session.expiresAt === 'number') {
    return session.expiresAt;
  }
  if (typeof session.expiresIn === 'number' && session.expiresIn > 0) {
    return Date.now() + session.expiresIn * 1000;
  }
  return readJwtExpiry(session.accessToken);
}

function readJwtExpiry(accessToken?: string): number | null {
  if (!accessToken) {
    return null;
  }
  const parts = accessToken.split('.');
  if (parts.length < 2) {
    return null;
  }
  try {
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))) as { exp?: number };
    return typeof payload.exp === 'number' ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

function getStorage(): Storage | null {
  return typeof globalThis.localStorage === 'undefined' ? null : globalThis.localStorage;
}

function getStorageItem(key: string): string | null {
  const storage = getStorage();
  return storage ? storage.getItem(key) : memoryStorage.get(key) ?? null;
}

function setStorageItem(key: string, value: string) {
  const storage = getStorage();
  if (storage) {
    storage.setItem(key, value);
    return;
  }
  memoryStorage.set(key, value);
}

function removeStorageItem(key: string) {
  const storage = getStorage();
  if (storage) {
    storage.removeItem(key);
    return;
  }
  memoryStorage.delete(key);
}
