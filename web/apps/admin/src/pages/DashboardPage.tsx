import { useQuery } from '@tanstack/react-query';
import { Button, Card, Col, List, Row, Space, Tag, Typography } from 'antd';
import { listWorkflows, type Workflow } from '../api/workflows';

export function DashboardPage() {
  const workflowQuery = useQuery({
    queryKey: ['workflows'],
    queryFn: listWorkflows
  });
  const workflows = workflowQuery.data?.items ?? [];

  const stats = [
    { label: '工作流', value: String(workflowQuery.data?.total ?? workflows.length) },
    { label: '今日执行', value: '0' },
    { label: '运行成功率', value: '0%' },
    { label: '模型调用', value: '0' }
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Row gutter={[16, 16]}>
        {stats.map((item) => (
          <Col key={item.label} xs={24} sm={12} lg={6}>
            <Card styles={{ body: { padding: 18 } }}>
              <Typography.Text type="secondary">{item.label}</Typography.Text>
              <div style={{ color: '#1f2937', fontSize: 28, fontWeight: 750, marginTop: 8 }}>
                {item.value}
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Card
        title="最近工作流"
        extra={<Button type="link" onClick={() => navigateTo('/workflows')}>查看全部</Button>}
      >
        <List
          loading={workflowQuery.isLoading}
          dataSource={workflows.slice(0, 5)}
          locale={{ emptyText: '暂无工作流' }}
          renderItem={(workflow) => <WorkflowListItem workflow={workflow} />}
        />
      </Card>
    </Space>
  );
}

function WorkflowListItem({ workflow }: { workflow: Workflow }) {
  return (
    <List.Item
      actions={[
        <Button key="edit" type="link" onClick={() => navigateTo(`/workflows/${workflow.id}/designer`)}>
          编辑
        </Button>,
        <Button key="run" type="link">
          运行
        </Button>
      ]}
    >
      <List.Item.Meta
        title={<Typography.Text strong>{workflow.name}</Typography.Text>}
        description={workflow.description || 'AI 工作流'}
      />
      <Tag color={workflow.status === 'PUBLISHED' ? 'green' : 'blue'}>
        {workflow.status === 'PUBLISHED' ? '已发布' : '草稿'}
      </Tag>
    </List.Item>
  );
}

function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}
