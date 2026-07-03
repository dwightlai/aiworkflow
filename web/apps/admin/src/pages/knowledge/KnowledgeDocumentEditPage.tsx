import { ArrowLeftOutlined, CheckCircleOutlined, EditOutlined, FileTextOutlined, MergeCellsOutlined, ScissorOutlined, SearchOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Checkbox, Drawer, Input, InputNumber, List, Space, Switch, Tag, Typography, message } from 'antd';
import type React from 'react';
import { useMemo, useState } from 'react';
import {
  listKnowledgeBases,
  listKnowledgeDocumentChunks,
  listKnowledgeDocuments,
  mergeKnowledgeChunks,
  searchKnowledgeDocument,
  splitKnowledgeChunk,
  adjustKnowledgeChunkStructure,
  updateKnowledgeChunk,
  type KnowledgeChunk
} from '../../api/knowledge';
import { navigateTo, readDatasetIdFromSearch, withDatasetQuery } from '../../navigation';
import { ChunkProfilePanel } from './ChunkProfilePanel';

interface KnowledgeDocumentEditPageProps {
  knowledgeBaseId: string;
  documentId: string;
}

export function KnowledgeDocumentEditPage({ knowledgeBaseId, documentId }: KnowledgeDocumentEditPageProps) {
  const queryClient = useQueryClient();
  const [editingChunk, setEditingChunk] = useState<KnowledgeChunk | null>(null);
  const [editingChunkContent, setEditingChunkContent] = useState('');
  const [editingSectionPath, setEditingSectionPath] = useState('');
  const [editingParentChunkId, setEditingParentChunkId] = useState('');
  const [splitOffset, setSplitOffset] = useState(1);
  const [selectedChunkIds, setSelectedChunkIds] = useState<string[]>([]);
  const [chunkQuery, setChunkQuery] = useState('');
  const [searchTopN, setSearchTopN] = useState(5);

  const basesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const documentsQuery = useQuery({
    queryKey: ['knowledge-documents', knowledgeBaseId],
    queryFn: () => listKnowledgeDocuments(knowledgeBaseId)
  });
  const chunksQuery = useQuery({
    queryKey: ['knowledge-chunks', knowledgeBaseId, documentId],
    queryFn: () => listKnowledgeDocumentChunks(knowledgeBaseId, documentId)
  });
  const documentSearchQuery = useQuery({
    queryKey: ['knowledge-document-search', knowledgeBaseId, documentId, chunkQuery.trim(), searchTopN],
    queryFn: () => searchKnowledgeDocument(knowledgeBaseId, documentId, {
      query: chunkQuery.trim(),
      topK: searchTopN
    }),
    enabled: Boolean(chunkQuery.trim())
  });

  const knowledgeBase = useMemo(
    () => basesQuery.data?.items.find((base) => base.id === knowledgeBaseId) ?? null,
    [basesQuery.data?.items, knowledgeBaseId]
  );
  const document = useMemo(
    () => documentsQuery.data?.items.find((item) => item.id === documentId) ?? null,
    [documentsQuery.data?.items, documentId]
  );
  const chunks = chunksQuery.data?.items ?? [];
  const displayedChunks = useMemo(() => {
    if (!chunkQuery.trim()) {
      return chunks;
    }
    const chunksById = new Map(chunks.map((chunk) => [chunk.id, chunk]));
    return (documentSearchQuery.data ?? [])
      .map((result) => chunksById.get(result.id))
      .filter((chunk): chunk is KnowledgeChunk => Boolean(chunk));
  }, [chunks, chunkQuery, documentSearchQuery.data]);

  const updateChunkMutation = useMutation({
    mutationFn: (payload: { chunk: KnowledgeChunk; content: string; enabled: boolean }) => updateKnowledgeChunk(
      payload.chunk.knowledgeBaseId,
      payload.chunk.id,
      {
        content: payload.content,
        enabled: payload.enabled
      }
    ),
    onSuccess: async () => {
      message.success('分段已更新');
      setEditingChunk(null);
      setEditingChunkContent('');
      await queryClient.invalidateQueries({ queryKey: ['knowledge-chunks', knowledgeBaseId, documentId] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-document-search', knowledgeBaseId, documentId] });
    }
  });

  const refreshChunks = async () => {
    setSelectedChunkIds([]);
    await queryClient.invalidateQueries({ queryKey: ['knowledge-chunks', knowledgeBaseId, documentId] });
    await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId] });
    await queryClient.invalidateQueries({ queryKey: ['knowledge-document-search', knowledgeBaseId, documentId] });
  };

  const splitChunkMutation = useMutation({
    mutationFn: (payload: { chunk: KnowledgeChunk; offset: number }) =>
      splitKnowledgeChunk(knowledgeBaseId, payload.chunk.id, payload.offset),
    onSuccess: async () => {
      message.success('分段已拆分');
      setEditingChunk(null);
      await refreshChunks();
    }
  });

  const mergeChunksMutation = useMutation({
    mutationFn: () => mergeKnowledgeChunks(knowledgeBaseId, selectedChunkIds),
    onSuccess: async () => {
      message.success('分段已合并');
      await refreshChunks();
    }
  });

  const adjustStructureMutation = useMutation({
    mutationFn: (chunk: KnowledgeChunk) => adjustKnowledgeChunkStructure(
      knowledgeBaseId,
      chunk.id,
      editingSectionPath.trim() || null,
      editingParentChunkId.trim() || null
    ),
    onSuccess: async () => {
      message.success('分段结构已更新');
      setEditingChunk(null);
      await refreshChunks();
    }
  });

  function openChunkEditor(chunk: KnowledgeChunk) {
    setEditingChunk(chunk);
    setEditingChunkContent(chunk.content);
    setEditingSectionPath(chunk.sectionPath || '');
    setEditingParentChunkId(chunk.parentChunkId || '');
    setSplitOffset(Math.max(1, Math.floor(chunk.content.length / 2)));
  }

  function navigateBackToDocuments() {
    const datasetId = readDatasetIdFromSearch() ?? document?.datasetId ?? null;
    navigateTo(withDatasetQuery(`/knowledge/${knowledgeBaseId}/documents`, datasetId));
  }

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={8}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={navigateBackToDocuments}
            style={{ width: 'fit-content' }}
          >
            返回资料列表
          </Button>
          <Space wrap>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {document?.name ?? '编辑文档'}
            </Typography.Title>
            <Tag color="blue">{chunks.length} 个分段</Tag>
            {knowledgeBase ? <Tag>{knowledgeBase.name}</Tag> : null}
          </Space>
          <Typography.Text type="secondary">
            搜索仅在当前文档分段内执行，使用后端混合检索排序，可编辑分段内容、启用或停用检索片段。
          </Typography.Text>
        </Space>
      </div>

      <ChunkProfilePanel knowledgeBaseId={knowledgeBaseId} documentId={documentId} />

      <Card
        variant="borderless"
        title={<Space><FileTextOutlined />分段列表</Space>}
        extra={(
          <Typography.Text type="secondary">
            {chunkQuery.trim() ? `匹配 ${displayedChunks.length} / ${chunks.length} 个分段` : `${chunks.length} 个分段`}
          </Typography.Text>
        )}
      >
        <Space.Compact style={{ display: 'flex', marginBottom: 12 }}>
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="搜索相关数据"
            value={chunkQuery}
            onChange={(event) => setChunkQuery(event.target.value)}
            style={{ flex: 1 }}
          />
          <InputNumber
            min={1}
            max={50}
            value={searchTopN}
            onChange={(value) => setSearchTopN(value ?? 5)}
            style={{ width: 140 }}
          />
        </Space.Compact>
        <Button
          icon={<MergeCellsOutlined />}
          disabled={selectedChunkIds.length < 2}
          loading={mergeChunksMutation.isPending}
          onClick={() => mergeChunksMutation.mutate()}
          style={{ marginBottom: 12 }}
        >
          合并所选分段
        </Button>
        <List
          loading={chunksQuery.isLoading || documentsQuery.isLoading || documentSearchQuery.isFetching}
          dataSource={displayedChunks}
          locale={{ emptyText: chunkQuery.trim() ? '当前文档内未找到匹配分段' : '暂无分段' }}
          renderItem={(chunk) => (
            <List.Item
              actions={[
                <Space key="enabled">
                  <Typography.Text type="secondary">启用</Typography.Text>
                  <Switch
                    checked={chunk.enabled}
                    loading={updateChunkMutation.isPending}
                    onChange={(enabled) => updateChunkMutation.mutate({ chunk, content: chunk.content, enabled })}
                  />
                </Space>,
                <Button key="edit" size="small" icon={<EditOutlined />} onClick={() => openChunkEditor(chunk)}>
                  编辑
                </Button>
              ]}
            >
              <Checkbox
                checked={selectedChunkIds.includes(chunk.id)}
                disabled={chunk.chunkLevel === 'PARENT'}
                onChange={(event) => setSelectedChunkIds((current) => event.target.checked
                  ? [...current, chunk.id]
                  : current.filter((id) => id !== chunk.id))}
                style={{ marginRight: 12 }}
              />
              <List.Item.Meta
                title={(
                  <Space>
                    <Tag color={chunk.enabled ? 'green' : 'default'}>{chunk.enabled ? '启用' : '停用'}</Tag>
                    <Tag color="blue">#{chunk.index + 1}</Tag>
                    <Tag color={chunk.chunkLevel === 'PARENT' ? 'purple' : 'cyan'}>
                      {chunk.chunkLevel === 'PARENT' ? '父块' : '子块'}
                    </Tag>
                    <Typography.Text type="secondary">{chunk.tokenEstimate} tokens</Typography.Text>
                  </Space>
                )}
                description={<Typography.Paragraph style={{ marginBottom: 0 }}>{chunk.content}</Typography.Paragraph>}
              />
            </List.Item>
          )}
        />
      </Card>

      <Drawer
        title={editingChunk ? `编辑分段 #${editingChunk.index + 1}` : '编辑分段'}
        open={Boolean(editingChunk)}
        width={680}
        onClose={() => {
          setEditingChunk(null);
          setEditingChunkContent('');
        }}
      >
        <Space direction="vertical" style={{ width: '100%' }} size={16}>
          <Input.TextArea
            aria-label="分段内容"
            autoSize={{ minRows: 12, maxRows: 20 }}
            value={editingChunkContent}
            onChange={(event) => setEditingChunkContent(event.target.value)}
          />
          <Input
            aria-label="章节路径"
            placeholder="章节路径，例如：第二章 > 付款节点"
            value={editingSectionPath}
            onChange={(event) => setEditingSectionPath(event.target.value)}
          />
          <Input
            aria-label="父块ID"
            placeholder="父块 ID，留空表示不绑定父块"
            value={editingParentChunkId}
            onChange={(event) => setEditingParentChunkId(event.target.value)}
          />
          <Space>
            <InputNumber
              aria-label="拆分位置"
              min={1}
              max={Math.max(1, editingChunkContent.length - 1)}
              value={splitOffset}
              onChange={(value) => setSplitOffset(value ?? 1)}
            />
            <Button
              icon={<ScissorOutlined />}
              disabled={!editingChunk || editingChunk.chunkLevel === 'PARENT'}
              loading={splitChunkMutation.isPending}
              onClick={() => editingChunk && splitChunkMutation.mutate({ chunk: editingChunk, offset: splitOffset })}
            >
              按位置拆分
            </Button>
            <Button
              icon={<CheckCircleOutlined />}
              disabled={!editingChunk || editingChunk.chunkLevel === 'PARENT'}
              loading={adjustStructureMutation.isPending}
              onClick={() => editingChunk && adjustStructureMutation.mutate(editingChunk)}
            >
              保存结构
            </Button>
          </Space>
          <Space>
            <Button
              type="primary"
              icon={<CheckCircleOutlined />}
              loading={updateChunkMutation.isPending}
              onClick={() => {
                const content = editingChunkContent.trim();
                if (editingChunk && content) {
                  updateChunkMutation.mutate({
                    chunk: editingChunk,
                    content,
                    enabled: editingChunk.enabled
                  });
                }
              }}
            >
              保存分段
            </Button>
            <Button onClick={() => {
              setEditingChunk(null);
              setEditingChunkContent('');
            }}>
              取消
            </Button>
          </Space>
        </Space>
      </Drawer>
    </section>
  );
}

const pageStyle: React.CSSProperties = {
  background: '#f5f7fb',
  minHeight: '100%',
  padding: 24
};

const headerStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  marginBottom: 16,
  padding: '18px 20px'
};
