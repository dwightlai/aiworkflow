import { Card, Col, Row, Space, Typography } from 'antd';

const stats = [
  { label: '工作流', value: '0' },
  { label: '今日执行', value: '0' },
  { label: '运行成功率', value: '0%' },
  { label: '模型调用', value: '0' }
];

export function DashboardPage() {
  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Row gutter={16}>
        {stats.map((item) => (
          <Col key={item.label} span={6}>
            <Card styles={{ body: { padding: 18 } }}>
              <Typography.Text type="secondary">{item.label}</Typography.Text>
              <div style={{ color: '#1f2937', fontSize: 28, fontWeight: 750, marginTop: 8 }}>
                {item.value}
              </div>
            </Card>
          </Col>
        ))}
      </Row>
      <Card title="最近工作流">
        <Typography.Text type="secondary">
          完整 AI Studio 正在建设中，工作流列表和设计器将在后续任务接入。
        </Typography.Text>
      </Card>
    </Space>
  );
}
