import { requestJson } from './auth';

export interface StorageSettings {
  knowledgeDocumentDir: string;
  knowledgeDocumentSaveOriginal: boolean;
  researchOutputDir: string;
  researchDocxMasterDir: string;
  customized: boolean;
}

export interface SaveStorageSettingsRequest {
  knowledgeDocumentDir: string;
  knowledgeDocumentSaveOriginal: boolean;
  researchOutputDir: string;
  researchDocxMasterDir: string;
}

export async function getStorageSettings(): Promise<StorageSettings> {
  return requestJson<StorageSettings>('/api/system/storage-settings');
}

export async function saveStorageSettings(request: SaveStorageSettingsRequest): Promise<StorageSettings> {
  return requestJson<StorageSettings>('/api/system/storage-settings', {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function resetStorageSettings(): Promise<StorageSettings> {
  return requestJson<StorageSettings>('/api/system/storage-settings/reset', {
    method: 'POST'
  });
}
