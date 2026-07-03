// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ChunkProfilePanel } from './ChunkProfilePanel';

vi.mock('../../api/knowledge', () => ({
  listChunkProfiles: vi.fn().mockResolvedValue([]),
  createChunkProfile: vi.fn(),
  activateChunkProfile: vi.fn(),
  compareChunkProfiles: vi.fn()
}));

describe('ChunkProfilePanel', () => {
  it('shows semantic similarity threshold only for semantic profiles', async () => {
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } }
    });
    render(
      <QueryClientProvider client={client}>
        <ChunkProfilePanel knowledgeBaseId="kb_1" documentId="doc_1" />
      </QueryClientProvider>
    );

    expect(screen.queryByLabelText('语义相似度阈值')).toBeNull();
    fireEvent.mouseDown(screen.getByText('结构感知').closest('.ant-select-selector')!);
    fireEvent.click(await screen.findByText('语义分块'));

    expect((screen.getByLabelText('语义相似度阈值') as HTMLInputElement).value).toBe('0.78');
  });
});
