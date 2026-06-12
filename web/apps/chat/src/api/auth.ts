export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
}

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
  user: AuthUser;
}

const memoryStorage = new Map<string, string>();

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
    return parsed as AuthSession;
  } catch {
    clearAuthSession();
    return null;
  }
}

export function setAuthSession(session: AuthSession) {
  setStorageItem(AUTH_STORAGE_KEY, JSON.stringify(session));
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
  const raw = await response.text();
  const envelope = raw
    ? JSON.parse(raw) as ApiEnvelope<T>
    : ({ success: response.ok, data: undefined as T, error: null } satisfies ApiEnvelope<T>);
  if (!response.ok || !envelope.success) {
    throw new Error(envelope.error?.message ?? `Request failed: ${response.status}`);
  }
  return envelope.data;
}

export function withRequestHeaders(
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
