import { Button, Card, Space, Table, Typography } from 'antd';

export function WorkflowListPage() {
  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space style={{ justifyContent: 'space-between', width: '100%' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          工作流
        </Typography.Title>
        <Button type="primary">新建工作流</Button>
      </Space>
      <Card>
        <Table
          rowKey="id"
          pagination={false}
          dataSource={[]}
          columns={[
            { title: '名称', dataIndex: 'name' },
            { title: '状态', dataIndex: 'status' },
            { title: '更新时间', dataIndex: 'updatedAt' }
          ]}
        />
      </Card>
    </Space>
  );
}
