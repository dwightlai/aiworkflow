import { requestJson } from './auth';

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export type ConnectorType = 'OA' | 'ARCHIVE' | 'WORKFLOW' | 'KB' | 'GENERIC';
export type ConnectorAccessType = 'HTTP' | 'MCP' | 'WEBHOOK';
export type ConnectorAuthMode = 'USER_TOKEN' | 'API_KEY' | 'FIXED_HEADER' | 'NONE';

export interface Connector {
  id: string;
  tenantId?: string;
  name: string;
  code: string;
  type: ConnectorType;
  accessType: ConnectorAccessType;
  baseUrl: string | null;
  authMode: ConnectorAuthMode | null;
  authConfig: string | null;
  enabled: boolean;
  description: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export type OperationType = 'QUERY' | 'ACTION';
export type RiskLevel = 'LOW' | 'HIGH';

export interface ConnectorOperation {
  id: string;
  tenantId?: string;
  connectorId: string;
  name: string;
  code: string;
  method: string | null;
  path: string | null;
  operationType: OperationType;
  riskLevel: RiskLevel;
  needConfirm: boolean;
  confirmSummaryTemplate: string | null;
  requestTemplate: string | null;
  enabled: boolean;
  description: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveConnectorRequest {
  name: string;
  code: string;
  type: ConnectorType;
  accessType: ConnectorAccessType;
  baseUrl?: string | null;
  authMode?: ConnectorAuthMode | null;
  authConfig?: string | null;
  enabled: boolean;
  description?: string | null;
}

export interface SaveConnectorOperationRequest {
  name: string;
  code: string;
  method?: string | null;
  path?: string | null;
  operationType: OperationType;
  riskLevel: RiskLevel;
  needConfirm: boolean;
  confirmSummaryTemplate?: string | null;
  requestTemplate?: string | null;
  enabled: boolean;
  description?: string | null;
}

export interface TestConnectorOperationResponse {
  success: boolean;
  statusCode?: number;
  body?: string;
  errorMessage?: string;
  traceId?: string | null;
  requestUrl?: string | null;
  durationMs?: number | null;
}

export async function listConnectors(): Promise<PageResponse<Connector>> {
  return requestJson<PageResponse<Connector>>('/api/connectors');
}

export async function createConnector(request: SaveConnectorRequest): Promise<Connector> {
  return requestJson<Connector>('/api/connectors', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateConnector(id: string, request: SaveConnectorRequest): Promise<Connector> {
  return requestJson<Connector>(`/api/connectors/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteConnector(id: string): Promise<void> {
  await requestJson<void>(`/api/connectors/${id}`, { method: 'DELETE' });
}

export async function listConnectorOperations(connectorId: string): Promise<PageResponse<ConnectorOperation>> {
  return requestJson<PageResponse<ConnectorOperation>>(`/api/connectors/${connectorId}/operations`);
}

export async function createConnectorOperation(
  connectorId: string,
  request: SaveConnectorOperationRequest
): Promise<ConnectorOperation> {
  return requestJson<ConnectorOperation>(`/api/connectors/${connectorId}/operations`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateConnectorOperation(
  connectorId: string,
  operationId: string,
  request: SaveConnectorOperationRequest
): Promise<ConnectorOperation> {
  return requestJson<ConnectorOperation>(`/api/connectors/${connectorId}/operations/${operationId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteConnectorOperation(connectorId: string, operationId: string): Promise<void> {
  await requestJson<void>(`/api/connectors/${connectorId}/operations/${operationId}`, { method: 'DELETE' });
}

export async function testConnectorOperation(
  connectorId: string,
  operationId: string,
  payload?: Record<string, unknown>
): Promise<TestConnectorOperationResponse> {
  return requestJson<TestConnectorOperationResponse>(
    `/api/connectors/${connectorId}/operations/${operationId}/test`,
    {
      method: 'POST',
      body: JSON.stringify(payload ?? {})
    }
  );
}

export interface ConnectorExportBundle {
  connector: {
    name: string;
    code: string;
    type: string;
    accessType: string;
    baseUrl: string | null;
    authMode: string | null;
    authConfig: string | null;
    enabled: boolean;
    description: string | null;
  };
  operations: Array<{
    name: string;
    code: string;
    method: string | null;
    path: string | null;
    operationType: string;
    riskLevel: string;
    needConfirm: boolean;
    confirmSummaryTemplate: string | null;
    requestTemplate: string | null;
    enabled: boolean;
    description: string | null;
  }>;
}

export interface ConnectorCallStat {
  connectorCode: string;
  operationCode: string;
  totalCalls: number;
  successCalls: number;
  failedCalls: number;
}

export async function exportConnector(connectorId: string): Promise<ConnectorExportBundle> {
  return requestJson<ConnectorExportBundle>(`/api/connectors/${connectorId}/export`);
}

export async function importConnector(bundle: ConnectorExportBundle, overwrite = true): Promise<Connector> {
  return requestJson<Connector>('/api/connectors/import', {
    method: 'POST',
    body: JSON.stringify({ bundle, overwrite })
  });
}

export async function listConnectorStats(): Promise<PageResponse<ConnectorCallStat>> {
  return requestJson<PageResponse<ConnectorCallStat>>('/api/connectors/stats');
}
