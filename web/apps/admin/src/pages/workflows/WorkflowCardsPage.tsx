import {
  AppstoreOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
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
import { Alert, Button, Card, Drawer, Empty, Input, Popconfirm, Segmented, Skeleton, Space, Tag, Tooltip, Typography, message } from 'antd';
import type React from 'react';
import { useMemo, useState } from 'react';
import { archiveWorkflow, deleteWorkflow, getWorkflow, listWorkflows, runWorkflow, updateWorkflowMetadata, buildWorkflowRunInput, type Workflow } from '../../api/workflows';

const demoDescription = '配置节点、Prompt、模型和工具调用，编排可运行的 AI 自动化流程。';
const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已归档', value: 'ARCHIVED' }
];

export function WorkflowCardsPage() {
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [runningWorkflow, setRunningWorkflow] = useState<Workflow | null>(null);
  const [settingWorkflow, setSettingWorkflow] = useState<Workflow | null>(null);
  const [settingName, setSettingName] = useState('');
  const [settingDescription, setSettingDescription] = useState('');
  const [runInput, setRunInput] = useState('{\n  "input": "请在这里填写运行参数"\n}');
  const [runInputError, setRunInputError] = useState<string | null>(null);
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
  const deleteMutation = useMutation({
    mutationFn: (workflow: Workflow) => deleteWorkflow(workflow.id),
    onSuccess: async () => {
      message.success('工作流已删除');
      await queryClient.invalidateQueries({ queryKey: ['workflows'] });
    }
  });
  const runMutation = useMutation({
    mutationFn: (payload: { workflow: Workflow; input: Record<string, unknown> }) => runWorkflow(payload.workflow.id, payload.input),
    onSuccess: (execution) => {
      message.success('工作流运行已完成');
      setRunningWorkflow(null);
      setRunInputError(null);
      navigateTo(`/workflow-runs/${execution.id}`);
    },
    onError: (error) => {
      setRunInputError((error as Error).message);
    }
  });
  const metadataMutation = useMutation({
    mutationFn: () => {
      if (!settingWorkflow) {
        throw new Error('请选择工作流');
      }
      return updateWorkflowMetadata(settingWorkflow.id, {
        name: settingName.trim() || settingWorkflow.name,
        description: settingDescription.trim() || null
      });
    },
    onSuccess: async () => {
      message.success('工作流设置已保存');
      setSettingWorkflow(null);
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
            管理 AI 工作流资产，从编辑编排、发布运行到执行观察都在这里完成。
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

      <section>
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
                deletePending={deleteMutation.isPending}
                onArchive={() => archiveMutation.mutate(workflow)}
                onDelete={() => deleteMutation.mutate(workflow)}
                onSettings={() => openSettingsDrawer(workflow)}
                onRun={() => openRunDrawer(workflow)}
              />
            )) : null}
          </div>

        {!workflowQuery.isLoading && workflows.length === 0 ? (
          <Empty description="暂无工作流，先创建一个可运行的 AI 流程" />
        ) : null}
      </section>

      <Drawer
        title={runningWorkflow ? `运行工作流 - ${runningWorkflow.name}` : '运行工作流'}
        open={Boolean(runningWorkflow)}
        width={560}
        onClose={() => {
          setRunningWorkflow(null);
          setRunInputError(null);
        }}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setRunningWorkflow(null)}>取消</Button>
            <Button type="primary" icon={<PlayCircleOutlined />} loading={runMutation.isPending} onClick={submitRun}>
              开始运行
            </Button>
          </Space>
        )}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message="运行参数"
            description="请输入 JSON 对象。系统会把它作为工作流本次运行的 input 传入开始节点。"
          />
          {runningWorkflow?.status !== 'PUBLISHED' ? (
            <Alert type="warning" showIcon message="当前工作流尚未发布，后端可能拒绝运行。" />
          ) : null}
          {runInputError ? <Alert type="error" showIcon message={runInputError} /> : null}
          <Input.TextArea
            aria-label="运行输入 JSON"
            value={runInput}
            onChange={(event) => {
              setRunInput(event.target.value);
              setRunInputError(null);
            }}
            autoSize={{ minRows: 10, maxRows: 18 }}
            style={{ fontFamily: 'Consolas, monospace' }}
          />
        </Space>
      </Drawer>

      <Drawer
        title={settingWorkflow ? `工作流设置 - ${settingWorkflow.name}` : '工作流设置'}
        open={Boolean(settingWorkflow)}
        width={520}
        onClose={() => setSettingWorkflow(null)}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setSettingWorkflow(null)}>取消</Button>
            <Button type="primary" loading={metadataMutation.isPending} onClick={() => metadataMutation.mutate()}>
              保存设置
            </Button>
          </Space>
        )}
      >
        <Space direction="vertical" size={14} style={{ width: '100%' }}>
          <label style={settingFieldStyle}>
            <span>工作流名称</span>
            <Input
              aria-label="工作流名称"
              value={settingName}
              maxLength={200}
              onChange={(event) => setSettingName(event.target.value)}
              placeholder="请输入工作流名称"
            />
          </label>
          <label style={settingFieldStyle}>
            <span>工作流描述</span>
            <Input.TextArea
              aria-label="工作流描述"
              value={settingDescription}
              onChange={(event) => setSettingDescription(event.target.value)}
              placeholder="请输入工作流描述"
              autoSize={{ minRows: 4, maxRows: 8 }}
            />
          </label>
        </Space>
      </Drawer>
    </div>
  );

  function openSettingsDrawer(workflow: Workflow) {
    setSettingWorkflow(workflow);
    setSettingName(workflow.name);
    setSettingDescription(workflow.description ?? '');
  }

  function openRunDrawer(workflow: Workflow) {
    setRunningWorkflow(workflow);
    setRunInputError(null);
    void getWorkflow(workflow.id)
      .then((detail) => setRunInput(buildWorkflowRunInput(detail.latestVersion?.definition ?? null)))
      .catch(() => setRunInput('{\n  "input": "请在这里填写运行参数"\n}'));
  }

  function submitRun() {
    if (!runningWorkflow) {
      return;
    }
    const parsed = parseRunInput(runInput);
    if (!parsed.ok) {
      setRunInputError(parsed.message);
      return;
    }
    runMutation.mutate({ workflow: runningWorkflow, input: parsed.value });
  }
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
  deletePending,
  onArchive,
  onDelete,
  onSettings,
  onRun
}: {
  workflow: Workflow;
  archivePending: boolean;
  deletePending: boolean;
  onArchive: () => void;
  onDelete: () => void;
  onSettings: () => void;
  onRun: () => void;
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
        <ActionButton icon={<SettingOutlined />} label="设置" onClick={onSettings} />
        <ActionButton icon={<PlayCircleOutlined />} label="运行" onClick={onRun} />
        <ActionButton icon={<EditOutlined />} label="编辑" onClick={() => navigateTo(`/workflows/${workflow.id}/designer`)} />
        {workflow.status === 'ARCHIVED' ? (
          <ActionButton icon={<MoreOutlined />} label="更多" />
        ) : (
          <ActionButton icon={<FolderOutlined />} label="归档工作流" loading={archivePending} onClick={onArchive} />
        )}
        <Popconfirm title="确定删除该工作流？" description="删除后不可在列表中查看，历史运行记录仍保留。" onConfirm={onDelete}>
          <span>
            <ActionButton icon={<DeleteOutlined />} label="删除" loading={deletePending} />
          </span>
        </Popconfirm>
      </div>
    </Card>
  );
}

function parseRunInput(value: string): { ok: true; value: Record<string, unknown> } | { ok: false; message: string } {
  try {
    const parsed = JSON.parse(value) as unknown;
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return { ok: false, message: '运行输入必须是 JSON 对象。' };
    }
    return { ok: true, value: parsed as Record<string, unknown> };
  } catch {
    return { ok: false, message: '运行输入不是合法 JSON，请检查引号、逗号和括号。' };
  }
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
  gridTemplateColumns: 'repeat(5, 1fr)',
  minHeight: 42
};

const settingFieldStyle: React.CSSProperties = {
  color: '#344054',
  display: 'grid',
  fontSize: 13,
  fontWeight: 600,
  gap: 8
};
