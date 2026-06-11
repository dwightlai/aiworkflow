import { requestJson } from './auth';

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export interface SysMenu {
  id: string;
  tenantId?: string;
  groupTitle: string;
  menuKey: string;
  title: string;
  path: string;
  sortOrder: number;
  visible: boolean;
  platformOnly: boolean;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MenuGroup {
  title: string;
  items: SysMenu[];
}

export interface SaveMenuRequest {
  groupTitle: string;
  menuKey: string;
  title: string;
  path: string;
  sortOrder?: number;
  visible?: boolean;
  platformOnly?: boolean;
  status?: string;
}

export interface DataDictionary {
  id: string;
  tenantId?: string;
  code: string;
  name: string;
  description?: string | null;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DataDictionaryItem {
  id: string;
  dictionaryId: string;
  label: string;
  value: string;
  description?: string | null;
  sortOrder: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveDictionaryRequest {
  code: string;
  name: string;
  description?: string;
  status?: string;
}

export interface UpdateDictionaryRequest {
  name: string;
  description?: string;
  status?: string;
}

export interface SaveDictionaryItemRequest {
  label: string;
  value: string;
  description?: string;
  sortOrder?: number;
  status?: string;
}

export interface AuthAuditLog {
  id: string;
  tenantId?: string;
  eventType: string;
  userId?: string | null;
  appId?: string | null;
  unitId?: string | null;
  departmentIds?: string | null;
  roleIds?: string | null;
  clientIp?: string | null;
  userAgent?: string | null;
  result: string;
  errorCode?: string | null;
  occurredAt: string;
}

export interface AuditLogSearchParams {
  eventType?: string;
  userId?: string;
  result?: string;
  from?: string;
  to?: string;
  page?: number;
  pageSize?: number;
}

export async function listMenus(): Promise<PageResponse<SysMenu>> {
  return requestJson<PageResponse<SysMenu>>('/api/system/menus');
}

export async function listMenuNavigation(): Promise<MenuGroup[]> {
  return requestJson<MenuGroup[]>('/api/system/menus/navigation');
}

export async function createMenu(request: SaveMenuRequest): Promise<SysMenu> {
  return requestJson<SysMenu>('/api/system/menus', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateMenu(id: string, request: SaveMenuRequest): Promise<SysMenu> {
  return requestJson<SysMenu>(`/api/system/menus/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteMenu(id: string): Promise<void> {
  return requestJson<void>(`/api/system/menus/${id}`, { method: 'DELETE' });
}

export async function listDictionaries(): Promise<PageResponse<DataDictionary>> {
  return requestJson<PageResponse<DataDictionary>>('/api/system/dictionaries');
}

export async function listDictionaryItems(dictionaryId: string): Promise<PageResponse<DataDictionaryItem>> {
  return requestJson<PageResponse<DataDictionaryItem>>(`/api/system/dictionaries/${dictionaryId}/items`);
}

export async function createDictionary(request: SaveDictionaryRequest): Promise<DataDictionary> {
  return requestJson<DataDictionary>('/api/system/dictionaries', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateDictionary(id: string, request: UpdateDictionaryRequest): Promise<DataDictionary> {
  return requestJson<DataDictionary>(`/api/system/dictionaries/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteDictionary(id: string): Promise<void> {
  return requestJson<void>(`/api/system/dictionaries/${id}`, { method: 'DELETE' });
}

export async function createDictionaryItem(dictionaryId: string, request: SaveDictionaryItemRequest): Promise<DataDictionaryItem> {
  return requestJson<DataDictionaryItem>(`/api/system/dictionaries/${dictionaryId}/items`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateDictionaryItem(itemId: string, request: SaveDictionaryItemRequest): Promise<DataDictionaryItem> {
  return requestJson<DataDictionaryItem>(`/api/system/dictionaries/items/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteDictionaryItem(itemId: string): Promise<void> {
  return requestJson<void>(`/api/system/dictionaries/items/${itemId}`, { method: 'DELETE' });
}

export async function searchAuditLogs(params: AuditLogSearchParams = {}): Promise<PageResponse<AuthAuditLog>> {
  const query = new URLSearchParams();
  if (params.eventType) query.set('eventType', params.eventType);
  if (params.userId) query.set('userId', params.userId);
  if (params.result) query.set('result', params.result);
  if (params.from) query.set('from', params.from);
  if (params.to) query.set('to', params.to);
  query.set('page', String(params.page ?? 1));
  query.set('pageSize', String(params.pageSize ?? 20));
  return requestJson<PageResponse<AuthAuditLog>>(`/api/system/audit-logs?${query.toString()}`);
}

export async function listAuditEventTypes(): Promise<string[]> {
  return requestJson<string[]>('/api/system/audit-logs/event-types');
}
