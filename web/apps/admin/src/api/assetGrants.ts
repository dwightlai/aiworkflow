import { requestJson } from './auth';

export type AssetType = 'KNOWLEDGE_BASE' | 'BOT';
export type UnitScope = 'SELF' | 'SUBTREE';
export type DepartmentScope = 'SELF' | 'SUBTREE';

export interface AssetGrant {
  id: string;
  assetType: AssetType;
  assetId: string;
  permission: 'USE';
  unitId?: string | null;
  unitScope: UnitScope;
  departmentId?: string | null;
  departmentScope?: DepartmentScope;
  enabled: boolean;
  createdBy?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveAssetGrantsRequest {
  assetType: AssetType;
  assetId: string;
  ownerUnitId?: string | null;
  unitIds: string[];
  unitScope: UnitScope;
  departmentIds?: string[];
  departmentScope?: DepartmentScope;
}

export interface AssetGrantListResponse {
  ownerUnitId?: string | null;
  grants: AssetGrant[];
}

export async function listAssetGrants(assetType: AssetType, assetId: string): Promise<AssetGrantListResponse> {
  return requestJson<AssetGrantListResponse>(`/api/asset-grants?assetType=${assetType}&assetId=${assetId}`);
}

export async function saveAssetGrants(request: SaveAssetGrantsRequest): Promise<AssetGrantListResponse> {
  return requestJson<AssetGrantListResponse>('/api/asset-grants', {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}
