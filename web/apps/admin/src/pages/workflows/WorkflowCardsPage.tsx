import {
  AppstoreOutlined,
  ClockCircleOutlined,
  EditOutlined,
  FileSearchOutlined,
  FolderOutlined,
  MoreOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SettingOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Empty, Input, Segmented, Skeleton, Space, Tag, Tooltip, Typography, message } from 'antd';
import type React from 'react';
import { useMemo, useState } from 'react';
import { archiveWorkflow, listWorkflows, type Workflow } from '../../api/workflows';

const demoDescription = '配置节点、Prompt、模型和工具调用，编排可运行的 AI 自动化流程。';
const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已归档', value: 'ARCHIVED' }
];
const templates = [
  { name: '客服问答助手', scene: '知识库 + LLM', detail: '适合售前咨询、工单预处理' },
  { name: '合同条款抽取', scene: 'Prompt + 结构化输出', detail: '适合法务、采购和风控审核' },
  { name: '线索评分流程', scene: '条件分支 + 工具调用', detail: '适合 CRM 自动化流转' }
];

export function WorkflowCardsPage() {
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const queryClient = useQueryClient();
  const workflowQuery = useQuery({
    queryKey: ['workflows'],
    queryFn: listWorkflows
  });
  const archiveMutation = useMutation({
    mutationFn: (workflow: Workflow) => archiveWorkflow(workflow.id),
    onSuccess: async () => {
      message.success('工作流已归档');
      await queryClient.invalidateQueries({ queryKey: ['workflows'] });
    }
  });

  const workflows = workflowQuery.data?.items ?? [];
  const visibleWorkflows = useMemo(() => {
    const trimmed = keyword.trim().toLowerCase();
    return workflows.filter((workflow) => {
      const matchedKeyword = !trimmed ||
        workflow.name.toLowerCase().includes(trimmed) ||
        (workflow.description ?? '').toLowerCase().includes(trimmed);
      const matchedStatus = statusFilter === 'ALL' || workflow.status === statusFilter;
      return matchedKeyword && matchedStatus;
    });
  }, [keyword, statusFilter, workflows]);
  const publishedCount = workflows.filter((workflow) => workflow.status === 'PUBLISHED').length;
  const draftCount = workflows.filter((workflow) => workflow.status === 'DRAFT').length;
  const latestUpdatedAt = workflows.map((workflow) => workflow.updatedAt).filter(Boolean).sort().at(-1);

  return (
    <div style={pageStyle}>
      <section style={heroStyle}>
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>AI 功能 / 工作流</Typography.Text>
          <Typography.Title level={3} style={{ margin: '4px 0 6px' }}>工作流运营台</Typography.Title>
          <Typography.Paragraph style={heroCopyStyle}>
            管理 AI 工作流资产，从模板创建、编辑编排、发布运行到执行观察都在这里完成。
          </Typography.Paragraph>
        </div>
        <Space>
          <Button icon={<FileSearchOutlined />}>导入 DSL</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigateTo('/workflows/new/designer')}>
            创建工作流
          </Button>
        </Space>
      </section>

      <section style={summaryGridStyle}>
        <SummaryCard title="总工作流" value={String(workflows.length)} detail="当前空间全部流程" icon={<AppstoreOutlined />} />
        <SummaryCard title="已发布" value={String(publishedCount)} detail="可被业务系统调用" icon={<ThunderboltOutlined />} />
        <SummaryCard title="草稿" value={String(draftCount)} detail="待配置或待发布" icon={<EditOutlined />} />
        <SummaryCard title="最近运行" value={latestUpdatedAt ? formatDate(latestUpdatedAt) : '-'} detail="以更新时间近似展示" icon={<ClockCircleOutlined />} />
      </section>

      <section style={mainGridStyle}>
        <div style={{ minWidth: 0 }}>
          <section style={filterBarStyle}>
            <Space size={10} wrap>
              <Typography.Text strong>搜索：</Typography.Text>
              <Input
                allowClear
                placeholder="请输入工作流名称"
                prefix={<SearchOutlined />}
                style={{ width: 300 }}
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
              <Segmented options={statusOptions} value={statusFilter} onChange={(value) => setStatusFilter(String(value))} />
              <Button type="primary" icon={<SearchOutlined />}>
                搜索
              </Button>
              <Button icon={<ReloadOutlined />} onClick={() => { setKeyword(''); setStatusFilter('ALL'); }}>
                重置
              </Button>
            </Space>
          </section>

          {workflowQuery.isError ? (
            <Alert
              type="error"
              showIcon
              message="工作流加载失败"
              description={(workflowQuery.error as Error).message}
              style={{ marginBottom: 16 }}
            />
          ) : null}

          <div style={gridStyle}>
            <button type="button" aria-label="创建空白工作流" style={createCardStyle} onClick={() => navigateTo('/workflows/new/designer')}>
              <PlusOutlined />
              <span>空白工作流</span>
              <small>从空白 DAG 开始搭建</small>
            </button>

            {workflowQuery.isLoading
              ? Array.from({ length: 4 }).map((_, index) => (
                <Card key={index} styles={{ body: { height: 210 } }}>
                  <Skeleton active avatar paragraph={{ rows: 3 }} />
                </Card>
              ))
              : null}

            {!workflowQuery.isLoading ? visibleWorkflows.map((workflow) => (
              <WorkflowCard
                key={workflow.id}
                workflow={workflow}
                archivePending={archiveMutation.isPending}
                onArchive={() => archiveMutation.mutate(workflow)}
              />
            )) : null}
          </div>

          {!workflowQuery.isLoading && workflows.length === 0 ? (
            <Empty description="暂无工作流，先创建一个可运行的 AI 流程" />
          ) : null}
        </div>

        <aside style={templatePanelStyle}>
          <Typography.Title level={5} style={{ marginTop: 0 }}>模板中心</Typography.Title>
          <Typography.Paragraph type="secondary" style={{ fontSize: 13 }}>
            常见智能工作流模板，后续可接入模板市场和一键创建。
          </Typography.Paragraph>
          <div style={templateListStyle}>
            {templates.map((template) => (
              <button key={template.name} type="button" style={templateCardStyle}>
                <span style={templateNameStyle}>{template.name}</span>
                <Tag color="blue" style={{ width: 'fit-content' }}>{template.scene}</Tag>
                <span style={templateDetailStyle}>{template.detail}</span>
              </button>
            ))}
          </div>
        </aside>
      </section>
    </div>
  );
}

function SummaryCard({ title, value, detail, icon }: { title: string; value: string; detail: string; icon: React.ReactNode }) {
  return (
    <Card styles={{ body: { padding: 16 } }} style={summaryCardStyle}>
      <Space size={12} align="start">
        <div style={summaryIconStyle}>{icon}</div>
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{title}</Typography.Text>
          <Typography.Title level={4} style={{ margin: '2px 0' }}>{value}</Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{detail}</Typography.Text>
        </div>
      </Space>
    </Card>
  );
}

function WorkflowCard({
  workflow,
  archivePending,
  onArchive
}: {
  workflow: Workflow;
  archivePending: boolean;
  onArchive: () => void;
}) {
  return (
    <Card
      hoverable
      styles={{ body: { padding: 0 } }}
      style={cardStyle}
    >
      <div style={cardBodyStyle}>
        <div style={avatarStyle}>
          <AppstoreOutlined />
        </div>
        <div style={{ minWidth: 0, flex: 1 }}>
          <Space size={8} style={{ marginBottom: 8 }} wrap>
            <Typography.Title level={5} style={titleStyle}>
              {workflow.name}
            </Typography.Title>
            <StatusTag status={workflow.status} />
          </Space>
          <Typography.Paragraph ellipsis={{ rows: 2 }} style={descriptionStyle}>
            {workflow.description || demoDescription}
          </Typography.Paragraph>
          <div style={metaGridStyle}>
            <span>最新版本：v{workflow.latestVersion?.version ?? 0}</span>
            <span>更新时间：{formatDate(workflow.updatedAt)}</span>
          </div>
        </div>
      </div>

      <div style={cardActionsStyle}>
        <ActionButton icon={<SettingOutlined />} label="设置" />
        <ActionButton icon={<PlayCircleOutlined />} label="运行" />
        <ActionButton icon={<EditOutlined />} label="编辑" onClick={() => navigateTo(`/workflows/${workflow.id}/designer`)} />
        {workflow.status === 'ARCHIVED' ? (
          <ActionButton icon={<MoreOutlined />} label="更多" />
        ) : (
          <ActionButton icon={<FolderOutlined />} label="归档工作流" loading={archivePending} onClick={onArchive} />
        )}
      </div>
    </Card>
  );
}

function ActionButton({
  icon,
  label,
  loading,
  onClick
}: {
  icon: React.ReactNode;
  label: string;
  loading?: boolean;
  onClick?: () => void;
}) {
  return (
    <Tooltip title={label}>
      <Button aria-label={label} type="text" size="small" icon={icon} loading={loading} onClick={onClick}>
        {label}
      </Button>
    </Tooltip>
  );
}

function StatusTag({ status }: { status: Workflow['status'] }) {
  const color = status === 'PUBLISHED' ? 'green' : status === 'ARCHIVED' ? 'default' : 'blue';
  const label = status === 'PUBLISHED' ? '已发布' : status === 'ARCHIVED' ? '已归档' : '草稿';
  return <Tag color={color}>{label}</Tag>;
}

function formatDate(value?: string) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

const pageStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 16
};

const heroStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  borderBottom: '1px solid #edf0f5',
  display: 'flex',
  justifyContent: 'space-between',
  margin: '-16px -24px 0',
  padding: '20px 24px'
};

const heroCopyStyle: React.CSSProperties = {
  color: '#667085',
  margin: 0,
  maxWidth: 680
};

const summaryGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 14,
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))'
};

const summaryCardStyle: React.CSSProperties = {
  borderRadius: 8
};

const summaryIconStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#eef6ff',
  borderRadius: 8,
  color: '#1677ff',
  display: 'flex',
  fontSize: 18,
  height: 36,
  justifyContent: 'center',
  width: 36
};

const mainGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(0, 1fr) 310px'
};

const filterBarStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #edf0f5',
  borderRadius: 8,
  marginBottom: 16,
  padding: '14px 16px'
};

const gridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 18,
  gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))'
};

const createCardStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  border: '1px dashed #1677ff',
  borderRadius: 8,
  color: '#0958d9',
  cursor: 'pointer',
  display: 'flex',
  flexDirection: 'column',
  fontSize: 15,
  gap: 8,
  height: 210,
  justifyContent: 'center'
};

const cardStyle: React.CSSProperties = {
  borderRadius: 8,
  overflow: 'hidden'
};

const cardBodyStyle: React.CSSProperties = {
  display: 'flex',
  gap: 16,
  minHeight: 166,
  padding: '24px 22px'
};

const avatarStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#e8f1ff',
  borderRadius: 24,
  color: '#1677ff',
  display: 'flex',
  flex: '0 0 48px',
  fontSize: 22,
  height: 48,
  justifyContent: 'center',
  width: 48
};

const titleStyle: React.CSSProperties = {
  lineHeight: '22px',
  margin: 0
};

const descriptionStyle: React.CSSProperties = {
  color: '#667085',
  fontSize: 14,
  lineHeight: '23px',
  marginBottom: 16
};

const metaGridStyle: React.CSSProperties = {
  color: '#667085',
  display: 'grid',
  fontSize: 12,
  gap: 6
};

const cardActionsStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#f7faff',
  borderTop: '1px solid #edf1f7',
  display: 'grid',
  gridTemplateColumns: 'repeat(4, 1fr)',
  minHeight: 42
};

const templatePanelStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #edf0f5',
  borderRadius: 8,
  height: 'fit-content',
  padding: 16
};

const templateListStyle: React.CSSProperties = {
  display: 'grid',
  gap: 10
};

const templateCardStyle: React.CSSProperties = {
  background: '#fbfdff',
  border: '1px solid #e5ebf4',
  borderRadius: 8,
  cursor: 'pointer',
  display: 'grid',
  gap: 8,
  padding: 12,
  textAlign: 'left'
};

const templateNameStyle: React.CSSProperties = {
  color: '#0f172a',
  fontWeight: 700
};

const templateDetailStyle: React.CSSProperties = {
  color: '#667085',
  fontSize: 12,
  lineHeight: '18px'
};
