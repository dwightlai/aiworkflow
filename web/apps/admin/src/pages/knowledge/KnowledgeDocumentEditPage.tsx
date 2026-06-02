import { ArrowLeftOutlined, CheckCircleOutlined, EditOutlined, FileTextOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Drawer, Input, List, Space, Switch, Tag, Typography, message } from 'antd';
import type React from 'react';
import { useMemo, useState } from 'react';
import {
  listKnowledgeBases,
  listKnowledgeDocumentChunks,
  listKnowledgeDocuments,
  updateKnowledgeChunk,
  type KnowledgeChunk
} from '../../api/knowledge';
import { navigateTo } from '../../navigation';

interface KnowledgeDocumentEditPageProps {
  knowledgeBaseId: string;
  documentId: string;
}

export function KnowledgeDocumentEditPage({ knowledgeBaseId, documentId }: KnowledgeDocumentEditPageProps) {
  const queryClient = useQueryClient();
  const [editingChunk, setEditingChunk] = useState<KnowledgeChunk | null>(null);
  const [editingChunkContent, setEditingChunkContent] = useState('');

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

  const knowledgeBase = useMemo(
    () => basesQuery.data?.items.find((base) => base.id === knowledgeBaseId) ?? null,
    [basesQuery.data?.items, knowledgeBaseId]
  );
  const document = useMemo(
    () => documentsQuery.data?.items.find((item) => item.id === documentId) ?? null,
    [documentsQuery.data?.items, documentId]
  );
  const chunks = chunksQuery.data?.items ?? [];

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
      message.success('切片已更新');
      setEditingChunk(null);
      setEditingChunkContent('');
      await queryClient.invalidateQueries({ queryKey: ['knowledge-chunks', knowledgeBaseId, documentId] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId] });
    }
  });

  function openChunkEditor(chunk: KnowledgeChunk) {
    setEditingChunk(chunk);
    setEditingChunkContent(chunk.content);
  }

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={8}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigateTo(`/knowledge/${knowledgeBaseId}/documents`)}
            style={{ width: 'fit-content' }}
          >
            返回文档列表
          </Button>
          <Space wrap>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {document?.name ?? '编辑文档'}
            </Typography.Title>
            <Tag color="blue">{chunks.length} 个切片</Tag>
            {knowledgeBase ? <Tag>{knowledgeBase.name}</Tag> : null}
          </Space>
          <Typography.Text type="secondary">
            当前版本支持切片级维护：编辑切片内容、启用或禁用检索片段。
          </Typography.Text>
        </Space>
      </div>

      <Card variant="borderless" title={<Space><FileTextOutlined />切片列表</Space>}>
        <List
          loading={chunksQuery.isLoading || documentsQuery.isLoading}
          dataSource={chunks}
          locale={{ emptyText: '暂无切片' }}
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
              <List.Item.Meta
                title={(
                  <Space>
                    <Tag color={chunk.enabled ? 'green' : 'default'}>{chunk.enabled ? '启用' : '停用'}</Tag>
                    <Tag color="blue">#{chunk.index + 1}</Tag>
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
        title={editingChunk ? `编辑切片 #${editingChunk.index + 1}` : '编辑切片'}
        open={Boolean(editingChunk)}
        width={680}
        onClose={() => {
          setEditingChunk(null);
          setEditingChunkContent('');
        }}
      >
        <Space direction="vertical" style={{ width: '100%' }} size={16}>
          <Input.TextArea
            aria-label="切片内容"
            autoSize={{ minRows: 12, maxRows: 20 }}
            value={editingChunkContent}
            onChange={(event) => setEditingChunkContent(event.target.value)}
          />
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
              保存切片
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
