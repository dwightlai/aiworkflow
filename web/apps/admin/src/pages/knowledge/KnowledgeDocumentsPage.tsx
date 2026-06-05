import {
  ArrowLeftOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  EditOutlined,
  FileAddOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Input, InputNumber, List, Space, Statistic, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useMemo, useState } from 'react';
import {
  deleteKnowledgeDocument,
  listKnowledgeBases,
  listKnowledgeDocuments,
  searchKnowledgeBase,
  type KnowledgeDocument,
  type KnowledgeSearchResult
} from '../../api/knowledge';
import { navigateTo } from '../../navigation';

interface KnowledgeDocumentsPageProps {
  knowledgeBaseId: string;
}

export function KnowledgeDocumentsPage({ knowledgeBaseId }: KnowledgeDocumentsPageProps) {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState('');
  const [topK, setTopK] = useState(3);
  const [searchResults, setSearchResults] = useState<KnowledgeSearchResult[]>([]);

  const basesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const documentsQuery = useQuery({
    queryKey: ['knowledge-documents', knowledgeBaseId],
    queryFn: () => listKnowledgeDocuments(knowledgeBaseId)
  });

  const knowledgeBase = useMemo(
    () => basesQuery.data?.items.find((base) => base.id === knowledgeBaseId) ?? null,
    [basesQuery.data?.items, knowledgeBaseId]
  );
  const documents = documentsQuery.data?.items ?? [];
  const chunkTotal = documents.reduce((sum, document) => sum + document.chunkCount, 0);
  const navigateToCreate = (type: 'manual' | 'text' | 'table') => {
    navigateTo(`/knowledge/${knowledgeBaseId}/documents/new?type=${type}`);
  };

  const deleteMutation = useMutation({
    mutationFn: (document: KnowledgeDocument) => deleteKnowledgeDocument(document.knowledgeBaseId, document.id),
    onSuccess: async () => {
      message.success('文档已删除');
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId] });
    }
  });

  const searchMutation = useMutation({
    mutationFn: () => searchKnowledgeBase(knowledgeBaseId, { query, topK }),
    onSuccess: setSearchResults
  });

  const columns: ColumnsType<KnowledgeDocument> = [
    {
      title: '文档名称',
      dataIndex: 'name',
      render: (_, document) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{document.name}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {document.createdAt ? `创建于 ${document.createdAt}` : '已入库文档'}
          </Typography.Text>
        </Space>
      )
    },
    {
      title: '切片数',
      dataIndex: 'chunkCount',
      width: 120,
      render: (value) => <Tag color="blue">{value} 个切片</Tag>
    },
    {
      title: '状态',
      width: 140,
      render: () => <Tag color="green">可检索</Tag>
    },
    {
      title: '操作',
      width: 310,
      render: (_, document) => (
        <Space size={6} wrap>
          <Button
            size="small"
            icon={<FileSearchOutlined />}
            onClick={() => navigateTo(`/knowledge/${knowledgeBaseId}/documents/${document.id}/edit`)}
          >
            查看切片
          </Button>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => navigateTo(`/knowledge/${knowledgeBaseId}/documents/${document.id}/edit`)}
          >
            编辑
          </Button>
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            loading={deleteMutation.isPending}
            onClick={() => deleteMutation.mutate(document)}
          >
            删除
          </Button>
        </Space>
      )
    }
  ];

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={8}>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigateTo('/knowledge')} style={{ width: 'fit-content' }}>
            返回知识库
          </Button>
          <Space wrap>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {knowledgeBase?.name ?? '知识库文档'}
            </Typography.Title>
            {knowledgeBase ? <Tag color="geekblue">{knowledgeBase.vectorDimension ?? 1536} 维</Tag> : null}
            {knowledgeBase?.embeddingModelId ? <Tag>{knowledgeBase.embeddingModelId}</Tag> : null}
          </Space>
          <Typography.Text type="secondary">
            管理该知识库下的文档集合，新增文档和切片编辑已拆分到独立页面。
          </Typography.Text>
        </Space>
        <Space wrap>
          <Button icon={<FileAddOutlined />} onClick={() => navigateToCreate('manual')}>
            手动数据集
          </Button>
          <Button type="primary" icon={<FileTextOutlined />} onClick={() => navigateToCreate('text')}>
            文本文档
          </Button>
          <Button icon={<DatabaseOutlined />} onClick={() => navigateToCreate('table')}>
            表格文档
          </Button>
        </Space>
      </div>

      {basesQuery.isError ? (
        <Alert
          type="error"
          showIcon
          message="知识库加载失败"
          description={(basesQuery.error as Error).message}
          style={{ marginBottom: 12 }}
        />
      ) : null}

      <div style={metricRowStyle}>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="文档数" value={documents.length} prefix={<FileTextOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="切片数" value={chunkTotal} prefix={<FileSearchOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="默认 Top K" value={knowledgeBase?.topK ?? topK} prefix={<SearchOutlined />} />
        </Card>
      </div>

      <Card
        variant="borderless"
        title={<Space><FileTextOutlined />文档列表</Space>}
      >
        <Table
          rowKey="id"
          loading={documentsQuery.isLoading}
          columns={columns}
          dataSource={documents}
          pagination={{ pageSize: 8, showSizeChanger: false }}
        />
      </Card>

      <Card variant="borderless" title={<Space><SearchOutlined />命中测试</Space>} style={{ marginTop: 16 }}>
        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            style={{ width: 360 }}
            placeholder="输入用户问题或关键词"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <InputNumber min={1} max={20} value={topK} onChange={(value) => setTopK(value ?? 3)} />
          <Button
            icon={<SearchOutlined />}
            loading={searchMutation.isPending}
            disabled={!query.trim()}
            onClick={() => searchMutation.mutate()}
          >
            检索
          </Button>
        </Space>
        <List
          dataSource={searchResults}
          locale={{ emptyText: '暂无检索结果' }}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={<Space><Tag color="blue">{item.documentName}</Tag><Typography.Text type="secondary">score {item.score}</Typography.Text></Space>}
                description={<Typography.Text>{item.content}</Typography.Text>}
              />
            </List.Item>
          )}
        />
      </Card>
    </section>
  );
}

const pageStyle: React.CSSProperties = {
  background: '#f5f7fb',
  minHeight: '100%',
  padding: 24
};

const headerStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  display: 'flex',
  justifyContent: 'space-between',
  marginBottom: 16,
  padding: '18px 20px'
};

const metricRowStyle: React.CSSProperties = {
  display: 'grid',
  gap: 12,
  gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
  marginBottom: 16
};

const metricCardStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3'
};
