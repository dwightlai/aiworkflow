import { SearchOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, Input, Space, Table, Tabs, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useMemo, useState } from 'react';
import type { AssetType } from '../../api/assetGrants';
import { listBots, type Bot } from '../../api/bots';
import { listKnowledgeBases, type KnowledgeBase } from '../../api/knowledge';
import { listModelProviders, type ModelProvider } from '../../api/models';
import { listWorkflows, type Workflow } from '../../api/workflows';
import { AssetGrantDrawer } from '../../components/AssetGrantDrawer';

type GrantTarget = {
  assetType: AssetType;
  assetId: string;
  assetName: string;
  ownerUnitId?: string | null;
  statusLabel: string;
  statusColor: string;
  description?: string | null;
};

const TAB_ITEMS: { key: AssetType; label: string }[] = [
  { key: 'BOT', label: '智能体' },
  { key: 'KNOWLEDGE_BASE', label: '知识库' },
  { key: 'WORKFLOW', label: '工作流' },
  { key: 'MODEL_PROVIDER', label: '大模型' }
];

const TYPE_LABEL: Record<AssetType, string> = {
  BOT: '智能体',
  KNOWLEDGE_BASE: '知识库',
  WORKFLOW: '工作流',
  MODEL_PROVIDER: '大模型'
};

const EMPTY_TEXT: Record<AssetType, string> = {
  BOT: '暂无智能体',
  KNOWLEDGE_BASE: '暂无知识库',
  WORKFLOW: '暂无工作流',
  MODEL_PROVIDER: '暂无大模型'
};

const SEARCH_PLACEHOLDER: Record<AssetType, string> = {
  BOT: '搜索智能体名称',
  KNOWLEDGE_BASE: '搜索知识库名称',
  WORKFLOW: '搜索工作流名称',
  MODEL_PROVIDER: '搜索大模型名称'
};

const pageStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 16 };
const headerStyle: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 };

export function AssetGrantsPage() {
  const [activeType, setActiveType] = useState<AssetType>('BOT');
  const [keyword, setKeyword] = useState('');
  const [grantTarget, setGrantTarget] = useState<GrantTarget | null>(null);

  const botsQuery = useQuery({ queryKey: ['bots'], queryFn: listBots });
  const knowledgeQuery = useQuery({ queryKey: ['knowledge-bases'], queryFn: listKnowledgeBases });
  const workflowsQuery = useQuery({ queryKey: ['workflows'], queryFn: listWorkflows });
  const modelsQuery = useQuery({ queryKey: ['model-providers'], queryFn: listModelProviders });

  const botTargets = useMemo<GrantTarget[]>(() => (botsQuery.data?.items ?? []).map((bot: Bot) => ({
    assetType: 'BOT',
    assetId: bot.id,
    assetName: bot.name,
    ownerUnitId: bot.ownerUnitId,
    statusLabel: bot.status === 'ENABLED' ? '启用' : '停用',
    statusColor: bot.status === 'ENABLED' ? 'green' : 'default',
    description: bot.description
  })), [botsQuery.data?.items]);

  const knowledgeTargets = useMemo<GrantTarget[]>(() => (knowledgeQuery.data?.items ?? []).map((base: KnowledgeBase) => ({
    assetType: 'KNOWLEDGE_BASE',
    assetId: base.id,
    assetName: base.name,
    ownerUnitId: base.ownerUnitId,
    statusLabel: base.status === 'READY' || !base.status ? '工作流可用' : base.status,
    statusColor: 'geekblue',
    description: base.description
  })), [knowledgeQuery.data?.items]);

  const workflowTargets = useMemo<GrantTarget[]>(() => (workflowsQuery.data?.items ?? []).map((workflow: Workflow) => ({
    assetType: 'WORKFLOW',
    assetId: workflow.id,
    assetName: workflow.name,
    ownerUnitId: workflow.ownerUnitId,
    statusLabel: workflow.status === 'PUBLISHED' ? '已发布' : workflow.status === 'DRAFT' ? '草稿' : workflow.status,
    statusColor: workflow.status === 'PUBLISHED' ? 'green' : workflow.status === 'DRAFT' ? 'gold' : 'default',
    description: workflow.description
  })), [workflowsQuery.data?.items]);

  const modelTargets = useMemo<GrantTarget[]>(() => (modelsQuery.data?.items ?? []).map((provider: ModelProvider) => ({
    assetType: 'MODEL_PROVIDER',
    assetId: provider.id,
    assetName: provider.name,
    ownerUnitId: provider.ownerUnitId,
    statusLabel: provider.enabled ? '启用' : '停用',
    statusColor: provider.enabled ? 'green' : 'default',
    description: `${provider.modelType} / ${provider.model}`
  })), [modelsQuery.data?.items]);

  const targetsByType: Record<AssetType, GrantTarget[]> = {
    BOT: botTargets,
    KNOWLEDGE_BASE: knowledgeTargets,
    WORKFLOW: workflowTargets,
    MODEL_PROVIDER: modelTargets
  };
  const loadingByType: Record<AssetType, boolean> = {
    BOT: botsQuery.isLoading,
    KNOWLEDGE_BASE: knowledgeQuery.isLoading,
    WORKFLOW: workflowsQuery.isLoading,
    MODEL_PROVIDER: modelsQuery.isLoading
  };

  const targets = targetsByType[activeType];
  const visibleTargets = targets.filter((item) => {
    const text = `${item.assetName} ${item.description ?? ''}`.toLowerCase();
    return !keyword.trim() || text.includes(keyword.trim().toLowerCase());
  });

  const columns: ColumnsType<GrantTarget> = [
    {
      title: TYPE_LABEL[activeType],
      dataIndex: 'assetName',
      render: (_, item) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{item.assetName}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{item.description || '暂无描述'}</Typography.Text>
        </Space>
      )
    },
    {
      title: '状态',
      width: 120,
      render: (_, item) => <Tag color={item.statusColor}>{item.statusLabel}</Tag>
    },
    {
      title: '操作',
      width: 120,
      render: (_, item) => (
        <Button size="small" type="primary" ghost onClick={() => setGrantTarget(item)}>配置授权</Button>
      )
    }
  ];

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>资产授权</Typography.Title>
          <Typography.Text type="secondary">
            配置智能体、知识库、工作流、大模型的单位与部门使用范围。业务页面供第三方 iframe 集成时不包含此功能。
          </Typography.Text>
        </Space>
      </div>

      <Card variant="borderless">
        <Tabs
          activeKey={activeType}
          onChange={(key) => {
            setActiveType(key as AssetType);
            setKeyword('');
          }}
          items={TAB_ITEMS}
        />
        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder={SEARCH_PLACEHOLDER[activeType]}
            style={{ width: 320 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
          <Button onClick={() => setKeyword('')}>重置</Button>
        </Space>
        <Table
          rowKey="assetId"
          size="small"
          loading={loadingByType[activeType]}
          columns={columns}
          dataSource={visibleTargets}
          pagination={{ pageSize: 10, showSizeChanger: false }}
          locale={{ emptyText: EMPTY_TEXT[activeType] }}
        />
      </Card>

      <AssetGrantDrawer
        open={Boolean(grantTarget)}
        assetType={grantTarget?.assetType ?? activeType}
        assetId={grantTarget?.assetId}
        assetName={grantTarget?.assetName}
        ownerUnitId={grantTarget?.ownerUnitId}
        onSaved={(savedOwnerUnitId) => {
          if (grantTarget && savedOwnerUnitId) {
            setGrantTarget({ ...grantTarget, ownerUnitId: savedOwnerUnitId });
          }
        }}
        onClose={() => setGrantTarget(null)}
      />
    </section>
  );
}
