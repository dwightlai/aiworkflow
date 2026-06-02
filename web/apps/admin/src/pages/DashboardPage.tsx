import {
  AppstoreOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CodeOutlined,
  DeploymentUnitOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, Empty, List, Progress, Space, Tag, Typography } from 'antd';
import type React from 'react';
import { listWorkflowRuns, listWorkflows, type Workflow, type WorkflowExecution } from '../api/workflows';

export function DashboardPage() {
  const workflowQuery = useQuery({
    queryKey: ['workflows'],
    queryFn: listWorkflows
  });
  const runQuery = useQuery({
    queryKey: ['workflow-runs'],
    queryFn: listWorkflowRuns
  });

  const workflows = workflowQuery.data?.items ?? [];
  const runs = runQuery.data?.items ?? [];
  const publishedCount = workflows.filter((workflow) => workflow.status === 'PUBLISHED').length;
  const succeededRuns = runs.filter((run) => run.status === 'SUCCEEDED').length;
  const failedRuns = runs.filter((run) => run.status === 'FAILED').length;
  const runningRuns = runs.filter((run) => run.status === 'RUNNING').length;
  const successRate = runs.length ? Math.round((succeededRuns / runs.length) * 100) : 0;
  const averageDuration = calculateAverageDuration(runs);

  const metrics = [
    {
      title: '总工作流',
      value: String(workflowQuery.data?.total ?? workflows.length),
      detail: `${publishedCount} 个已发布`,
      icon: <DeploymentUnitOutlined />,
      tone: 'blue' as const
    },
    {
      title: '今日执行',
      value: String(runQuery.data?.total ?? runs.length),
      detail: `${runningRuns} 个运行中`,
      icon: <PlayCircleOutlined />,
      tone: 'green' as const
    },
    {
      title: '运行成功率',
      value: `${successRate}%`,
      detail: failedRuns ? `${failedRuns} 个失败待排查` : '当前运行健康',
      icon: <CheckCircleOutlined />,
      tone: successRate >= 80 || runs.length === 0 ? 'green' as const : 'red' as const
    },
    {
      title: '平均耗时',
      value: averageDuration,
      detail: '基于已完成运行',
      icon: <ClockCircleOutlined />,
      tone: 'slate' as const
    }
  ];

  return (
    <div style={pageStyle}>
      <section style={heroStyle}>
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>首页 / AI 功能 / 工作台</Typography.Text>
          <Typography.Title level={3} style={{ margin: '4px 0 6px' }}>AI Studio 工作台</Typography.Title>
          <Typography.Paragraph style={heroCopyStyle}>
            统一查看工作流资产、运行健康度和最近操作入口，下一步可以继续扩展智能体、知识库和模型配置。
          </Typography.Paragraph>
        </div>
        <Space wrap>
          <Button icon={<FileSearchOutlined />} onClick={() => navigateTo('/workflow-runs')}>
            查看运行监控
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigateTo('/workflows/new/designer')}>
            新建工作流
          </Button>
        </Space>
      </section>

      <section style={metricGridStyle}>
        {metrics.map((metric) => (
          <MetricCard key={metric.title} {...metric} />
        ))}
      </section>

      <section style={contentGridStyle}>
        <div style={mainColumnStyle}>
          <section style={panelStyle}>
            <Space align="center" style={panelTitleRowStyle}>
              <Typography.Title level={5} style={{ margin: 0 }}>最近工作流</Typography.Title>
              <Button type="link" onClick={() => navigateTo('/workflows')}>查看全部</Button>
            </Space>
            <List
              loading={workflowQuery.isLoading}
              dataSource={workflows.slice(0, 5)}
              locale={{ emptyText: <Empty description="暂无工作流" /> }}
              renderItem={(workflow) => <WorkflowListItem workflow={workflow} />}
            />
          </section>

          <section style={panelStyle}>
            <Space align="center" style={panelTitleRowStyle}>
              <Typography.Title level={5} style={{ margin: 0 }}>运行态势</Typography.Title>
              <Tag>近期待办</Tag>
            </Space>
            <div style={runHealthStyle}>
              <Progress
                type="circle"
                percent={successRate}
                size={104}
                strokeColor={successRate >= 80 || runs.length === 0 ? '#16a34a' : '#dc2626'}
              />
              <div style={runSplitStyle}>
                <RunStat label="成功" value={succeededRuns} color="#16a34a" />
                <RunStat label="失败" value={failedRuns} color="#dc2626" />
                <RunStat label="运行中" value={runningRuns} color="#1677ff" />
              </div>
            </div>
          </section>
        </div>

        <aside style={sidePanelStyle}>
          <Typography.Title level={5} style={{ marginTop: 0 }}>快捷入口</Typography.Title>
          <div style={quickListStyle}>
            <QuickAction
              icon={<AppstoreOutlined />}
              title="工作流运营台"
              detail="管理流程、模板和发布状态"
              path="/workflows"
            />
            <QuickAction
              icon={<ExperimentOutlined />}
              title="运行监控台"
              detail="查看执行记录与节点日志"
              path="/workflow-runs"
            />
            <QuickAction
              icon={<CodeOutlined />}
              title="集成指南"
              detail="React、Vue、Web Component 接入"
              path="/tools"
            />
          </div>
        </aside>
      </section>
    </div>
  );
}

function MetricCard({
  title,
  value,
  detail,
  icon,
  tone
}: {
  title: string;
  value: string;
  detail: string;
  icon: React.ReactNode;
  tone: 'blue' | 'green' | 'red' | 'slate';
}) {
  return (
    <Card styles={{ body: { padding: 16 } }} style={cardStyle}>
      <Space align="start" size={12}>
        <div style={metricIconStyle(tone)}>{icon}</div>
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{title}</Typography.Text>
          <Typography.Title level={4} style={{ margin: '2px 0' }}>{value}</Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{detail}</Typography.Text>
        </div>
      </Space>
    </Card>
  );
}

function WorkflowListItem({ workflow }: { workflow: Workflow }) {
  return (
    <List.Item
      actions={[
        <Button key="edit" type="link" onClick={() => navigateTo(`/workflows/${workflow.id}/designer`)}>
          编辑
        </Button>,
        <Button key="run" type="link" onClick={() => navigateTo(`/workflows/${workflow.id}/designer`)}>
          调试
        </Button>
      ]}
    >
      <List.Item.Meta
        title={<Typography.Text strong>{workflow.name}</Typography.Text>}
        description={workflow.description || 'AI 工作流'}
      />
      <Space size={8}>
        <Typography.Text type="secondary">v{workflow.latestVersion?.version ?? 0}</Typography.Text>
        <WorkflowStatusTag status={workflow.status} />
      </Space>
    </List.Item>
  );
}

function WorkflowStatusTag({ status }: { status: Workflow['status'] }) {
  const color = status === 'PUBLISHED' ? 'green' : status === 'ARCHIVED' ? 'default' : 'blue';
  const label = status === 'PUBLISHED' ? '已发布' : status === 'ARCHIVED' ? '已归档' : '草稿';
  return <Tag color={color}>{label}</Tag>;
}

function RunStat({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={runStatStyle}>
      <span style={{ ...runDotStyle, background: color }} />
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Typography.Text strong>{value}</Typography.Text>
    </div>
  );
}

function QuickAction({
  icon,
  title,
  detail,
  path
}: {
  icon: React.ReactNode;
  title: string;
  detail: string;
  path: string;
}) {
  return (
    <button type="button" style={quickActionStyle} onClick={() => navigateTo(path)}>
      <span style={quickIconStyle}>{icon}</span>
      <span style={{ minWidth: 0 }}>
        <span style={quickTitleStyle}>{title}</span>
        <span style={quickDetailStyle}>{detail}</span>
      </span>
      <ThunderboltOutlined style={{ color: '#98a2b3' }} />
    </button>
  );
}

function calculateAverageDuration(runs: WorkflowExecution[]) {
  const durations = runs
    .map((run) => {
      if (!run.finishedAt) {
        return null;
      }
      return Math.max(new Date(run.finishedAt).getTime() - new Date(run.startedAt).getTime(), 0);
    })
    .filter((value): value is number => typeof value === 'number');
  if (!durations.length) {
    return '-';
  }
  const average = Math.round(durations.reduce((sum, value) => sum + value, 0) / durations.length);
  return `${average}ms`;
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
  maxWidth: 720
};

const metricGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 14,
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))'
};

const cardStyle: React.CSSProperties = {
  borderRadius: 8
};

const contentGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(0, 1fr) 320px'
};

const mainColumnStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  minWidth: 0
};

const panelStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #edf0f5',
  borderRadius: 8,
  padding: 16
};

const panelTitleRowStyle: React.CSSProperties = {
  justifyContent: 'space-between',
  marginBottom: 12,
  width: '100%'
};

const sidePanelStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #edf0f5',
  borderRadius: 8,
  height: 'fit-content',
  padding: 16
};

const quickListStyle: React.CSSProperties = {
  display: 'grid',
  gap: 10
};

const quickActionStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fbfdff',
  border: '1px solid #e5ebf4',
  borderRadius: 8,
  cursor: 'pointer',
  display: 'grid',
  gap: 10,
  gridTemplateColumns: '36px minmax(0, 1fr) 16px',
  padding: 12,
  textAlign: 'left',
  width: '100%'
};

const quickIconStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#eef6ff',
  borderRadius: 8,
  color: '#1677ff',
  display: 'flex',
  height: 36,
  justifyContent: 'center',
  width: 36
};

const quickTitleStyle: React.CSSProperties = {
  color: '#0f172a',
  display: 'block',
  fontWeight: 700
};

const quickDetailStyle: React.CSSProperties = {
  color: '#667085',
  display: 'block',
  fontSize: 12,
  marginTop: 3
};

const runHealthStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  gap: 24,
  minHeight: 132
};

const runSplitStyle: React.CSSProperties = {
  display: 'grid',
  flex: 1,
  gap: 10
};

const runStatStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#f8fafc',
  border: '1px solid #edf0f5',
  borderRadius: 8,
  display: 'grid',
  gridTemplateColumns: '10px minmax(0, 1fr) auto',
  padding: '10px 12px'
};

const runDotStyle: React.CSSProperties = {
  borderRadius: 5,
  height: 10,
  width: 10
};

function metricIconStyle(tone: 'blue' | 'green' | 'red' | 'slate'): React.CSSProperties {
  const colorMap = {
    blue: ['#eef6ff', '#1677ff'],
    green: ['#ecfdf3', '#16a34a'],
    red: ['#fef2f2', '#dc2626'],
    slate: ['#f1f5f9', '#475569']
  } satisfies Record<typeof tone, [string, string]>;
  const [background, color] = colorMap[tone];
  return {
    alignItems: 'center',
    background,
    borderRadius: 8,
    color,
    display: 'flex',
    fontSize: 18,
    height: 36,
    justifyContent: 'center',
    width: 36
  };
}
