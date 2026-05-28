import {
  AppstoreOutlined,
  EditOutlined,
  MoreOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SettingOutlined
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Empty, Input, Skeleton, Space, Tag, Tooltip, Typography } from 'antd';
import type React from 'react';
import { useMemo, useState } from 'react';
import { listWorkflows, type Workflow } from '../../api/workflows';

const demoDescription = '配置节点、Prompt、模型和工具调用，编排可运行的 AI 自动化流程。';

export function WorkflowCardsPage() {
  const [keyword, setKeyword] = useState('');
  const workflowQuery = useQuery({
    queryKey: ['workflows'],
    queryFn: listWorkflows
  });

  const workflows = workflowQuery.data?.items ?? [];
  const visibleWorkflows = useMemo(() => {
    const trimmed = keyword.trim().toLowerCase();
    if (!trimmed) {
      return workflows;
    }
    return workflows.filter((workflow) =>
      workflow.name.toLowerCase().includes(trimmed) ||
      (workflow.description ?? '').toLowerCase().includes(trimmed)
    );
  }, [keyword, workflows]);

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
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
          <Button type="primary" icon={<SearchOutlined />}>
            搜索
          </Button>
          <Button icon={<ReloadOutlined />} onClick={() => setKeyword('')}>
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
        />
      ) : null}

      <div style={gridStyle}>
        <button type="button" style={createCardStyle} onClick={() => navigateTo('/workflows/new/designer')}>
          <PlusOutlined />
          <span>创建工作流</span>
        </button>

        {workflowQuery.isLoading
          ? Array.from({ length: 4 }).map((_, index) => (
            <Card key={index} styles={{ body: { height: 188 } }}>
              <Skeleton active avatar paragraph={{ rows: 3 }} />
            </Card>
          ))
          : null}

        {!workflowQuery.isLoading ? visibleWorkflows.map((workflow) => (
            <WorkflowCard key={workflow.id} workflow={workflow} />
        )) : null}
      </div>

      {!workflowQuery.isLoading && workflows.length === 0 ? (
        <Empty description="暂无工作流，先创建一个可运行的 AI 流程" />
      ) : null}
    </Space>
  );
}

function WorkflowCard({ workflow }: { workflow: Workflow }) {
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
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            最新版本：v{workflow.latestVersion?.version ?? 0} · 更新时间：{formatDate(workflow.updatedAt)}
          </Typography.Text>
        </div>
      </div>

      <div style={cardActionsStyle}>
        <ActionButton icon={<SettingOutlined />} label="设置" />
        <ActionButton icon={<PlayCircleOutlined />} label="运行" />
        <ActionButton icon={<EditOutlined />} label="编辑" onClick={() => navigateTo(`/workflows/${workflow.id}/designer`)} />
        <ActionButton icon={<MoreOutlined />} label="更多" />
      </div>
    </Card>
  );
}

function ActionButton({ icon, label, onClick }: { icon: React.ReactNode; label: string; onClick?: () => void }) {
  return (
    <Tooltip title={label}>
      <Button type="text" size="small" icon={icon} onClick={onClick}>
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

const filterBarStyle: React.CSSProperties = {
  background: '#fff',
  borderBottom: '1px solid #edf0f5',
  margin: '-16px -24px 0',
  padding: '18px 24px'
};

const gridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 22,
  gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))'
};

const createCardStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  border: '1px solid #1677ff',
  borderRadius: 8,
  color: '#0958d9',
  cursor: 'pointer',
  display: 'flex',
  fontSize: 15,
  gap: 10,
  height: 188,
  justifyContent: 'center'
};

const cardStyle: React.CSSProperties = {
  borderRadius: 8,
  overflow: 'hidden'
};

const cardBodyStyle: React.CSSProperties = {
  display: 'flex',
  gap: 16,
  minHeight: 144,
  padding: '26px 24px'
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
  marginBottom: 14
};

const cardActionsStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#f7faff',
  borderTop: '1px solid #edf1f7',
  display: 'grid',
  gridTemplateColumns: 'repeat(4, 1fr)',
  minHeight: 42
};
