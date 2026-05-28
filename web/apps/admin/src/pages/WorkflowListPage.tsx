import { WorkflowDesignerReact } from '@aiworkflow/workflow-designer-react';
import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message
} from 'antd';
import type { TableColumnsType } from 'antd';
import { useMemo, useState } from 'react';
import {
  createWorkflow,
  listWorkflows,
  publishWorkflow,
  runWorkflow,
  type Workflow,
  type WorkflowExecution
} from '../api/workflows';

const defaultDefinition: WorkflowDefinition = {
  nodes: [
    { id: 'start', type: 'START', name: '开始', config: {} },
    {
      id: 'transform',
      type: 'TEXT_TRANSFORM',
      name: '文本处理',
      config: {
        outputKey: 'message',
        template: 'Hello {{name}}'
      }
    },
    {
      id: 'end',
      type: 'END',
      name: '结束',
      config: {
        outputKeys: ['message']
      }
    }
  ],
  edges: [
    { id: 'edge-1', sourceNodeId: 'start', targetNodeId: 'transform', condition: null },
    { id: 'edge-2', sourceNodeId: 'transform', targetNodeId: 'end', condition: null }
  ],
  variables: [{ name: 'name', type: 'STRING', required: true }]
};

const defaultRunInput = {
  name: 'Ada'
};

export function WorkflowListPage() {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [runOpen, setRunOpen] = useState(false);
  const [selectedWorkflow, setSelectedWorkflow] = useState<Workflow | null>(null);
  const [selectedExecution, setSelectedExecution] = useState<WorkflowExecution | null>(null);
  const [createForm] = Form.useForm();
  const [runForm] = Form.useForm();

  const workflowQuery = useQuery({
    queryKey: ['workflows'],
    queryFn: listWorkflows
  });

  const workflows = workflowQuery.data?.items ?? [];
  const activeWorkflow = selectedWorkflow ?? workflows[0] ?? null;
  const activeDefinition = activeWorkflow?.latestVersion?.definition ?? defaultDefinition;

  const createMutation = useMutation({
    mutationFn: createWorkflow,
    onSuccess: async (workflow) => {
      message.success('工作流已创建');
      setCreateOpen(false);
      setSelectedWorkflow(workflow);
      await queryClient.invalidateQueries({ queryKey: ['workflows'] });
    }
  });

  const publishMutation = useMutation({
    mutationFn: publishWorkflow,
    onSuccess: async (workflow) => {
      message.success('工作流已发布');
      setSelectedWorkflow(workflow);
      await queryClient.invalidateQueries({ queryKey: ['workflows'] });
    }
  });

  const runMutation = useMutation({
    mutationFn: ({ workflowId, input }: { workflowId: string; input: Record<string, unknown> }) =>
      runWorkflow(workflowId, input),
    onSuccess: (execution) => {
      message.success('运行完成');
      setSelectedExecution(execution);
      setRunOpen(false);
    }
  });

  const columns: TableColumnsType<Workflow> = useMemo(() => [
    {
      title: '名称',
      dataIndex: 'name',
      render: (name: string, record) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => setSelectedWorkflow(record)}>
          {name}
        </Button>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (status: Workflow['status']) => <StatusTag status={status} />
    },
    {
      title: '版本',
      width: 90,
      render: (_, record) => record.latestVersion?.version ?? '-'
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 190,
      render: (value?: string) => value ? new Date(value).toLocaleString() : '-'
    },
    {
      title: '操作',
      width: 180,
      render: (_, record) => (
        <Space size={8}>
          <Button size="small" onClick={() => publishMutation.mutate(record.id)} loading={publishMutation.isPending}>
            发布
          </Button>
          <Button
            size="small"
            type="primary"
            disabled={record.status !== 'PUBLISHED'}
            onClick={() => openRun(record)}
          >
            运行
          </Button>
        </Space>
      )
    }
  ], [publishMutation.isPending]);

  function openCreate() {
    createForm.setFieldsValue({
      name: 'Greeting workflow',
      description: '文本处理示例流程',
      definition: JSON.stringify(defaultDefinition, null, 2)
    });
    setCreateOpen(true);
  }

  function openRun(workflow: Workflow) {
    setSelectedWorkflow(workflow);
    runForm.setFieldsValue({ input: JSON.stringify(defaultRunInput, null, 2) });
    setRunOpen(true);
  }

  async function submitCreate() {
    const values = await createForm.validateFields();
    createMutation.mutate({
      name: values.name,
      description: values.description || null,
      definition: parseJson(values.definition)
    });
  }

  async function submitRun() {
    if (!selectedWorkflow) {
      return;
    }
    const values = await runForm.validateFields();
    runMutation.mutate({
      workflowId: selectedWorkflow.id,
      input: parseJson(values.input)
    });
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
        <div>
          <Typography.Title level={3} style={{ margin: 0 }}>
            工作流
          </Typography.Title>
          <Typography.Text type="secondary">定义、发布、运行和排查 AI 工作流</Typography.Text>
        </div>
        <Space>
          <Button onClick={() => workflowQuery.refetch()} loading={workflowQuery.isFetching}>
            刷新
          </Button>
          <Button type="primary" onClick={openCreate}>
            新建工作流
          </Button>
        </Space>
      </Space>

      {workflowQuery.isError ? (
        <Alert type="error" message="工作流加载失败" description={(workflowQuery.error as Error).message} />
      ) : null}

      <div style={{ display: 'grid', gap: 16, gridTemplateColumns: 'minmax(520px, 1.15fr) minmax(420px, 0.85fr)' }}>
        <section style={panelStyle}>
          <Table
            rowKey="id"
            loading={workflowQuery.isLoading}
            pagination={false}
            dataSource={workflows}
            columns={columns}
            onRow={(record) => ({ onClick: () => setSelectedWorkflow(record) })}
          />
        </section>

        <section style={panelStyle}>
          <Tabs
            items={[
              {
                key: 'definition',
                label: '定义',
                children: (
                  <Space direction="vertical" size={12} style={{ width: '100%' }}>
                    <Descriptions size="small" column={1} bordered>
                      <Descriptions.Item label="工作流">
                        {activeWorkflow?.name ?? '-'}
                      </Descriptions.Item>
                      <Descriptions.Item label="状态">
                        {activeWorkflow ? <StatusTag status={activeWorkflow.status} /> : '-'}
                      </Descriptions.Item>
                    </Descriptions>
                    <div style={{ border: '1px solid #d9dee8', height: 220 }}>
                      <WorkflowDesignerReact value={activeDefinition} readonly />
                    </div>
                    <Input.TextArea
                      value={JSON.stringify(activeDefinition, null, 2)}
                      readOnly
                      autoSize={{ minRows: 12, maxRows: 18 }}
                    />
                  </Space>
                )
              },
              {
                key: 'execution',
                label: '最近执行',
                children: selectedExecution ? (
                  <ExecutionDetail execution={selectedExecution} />
                ) : (
                  <Alert type="info" message="暂无执行记录" />
                )
              }
            ]}
          />
        </section>
      </div>

      <Drawer
        title="新建工作流"
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        width={720}
        extra={<Button type="primary" loading={createMutation.isPending} onClick={submitCreate}>保存</Button>}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input />
          </Form.Item>
          <Form.Item name="definition" label="DAG JSON" rules={[{ required: true, message: '请输入 DAG JSON' }]}>
            <Input.TextArea autoSize={{ minRows: 18, maxRows: 24 }} />
          </Form.Item>
        </Form>
      </Drawer>

      <Modal
        title="运行工作流"
        open={runOpen}
        onCancel={() => setRunOpen(false)}
        onOk={submitRun}
        confirmLoading={runMutation.isPending}
        okText="运行"
      >
        <Form form={runForm} layout="vertical">
          <Form.Item name="input" label="输入 JSON" rules={[{ required: true, message: '请输入运行参数' }]}>
            <Input.TextArea autoSize={{ minRows: 8, maxRows: 14 }} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

function ExecutionDetail({ execution }: { execution: WorkflowExecution }) {
  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <Descriptions size="small" column={1} bordered>
        <Descriptions.Item label="运行 ID">
          <Typography.Text copyable>{execution.id}</Typography.Text>
        </Descriptions.Item>
        <Descriptions.Item label="状态">
          <StatusTag status={execution.status} />
        </Descriptions.Item>
        <Descriptions.Item label="输出">
          <pre style={preStyle}>{JSON.stringify(execution.output, null, 2)}</pre>
        </Descriptions.Item>
      </Descriptions>
      <Table
        size="small"
        rowKey="id"
        pagination={false}
        dataSource={execution.nodeExecutions}
        columns={[
          { title: '节点', dataIndex: 'nodeId' },
          { title: '类型', dataIndex: 'nodeType', width: 140 },
          { title: '状态', dataIndex: 'status', width: 110, render: (status) => <StatusTag status={status} /> },
          { title: '错误', dataIndex: 'errorMessage', render: (value) => value ?? '-' }
        ]}
      />
    </Space>
  );
}

function StatusTag({ status }: { status: string }) {
  const color = status === 'PUBLISHED' || status === 'SUCCEEDED'
    ? 'green'
    : status === 'FAILED'
      ? 'red'
      : 'blue';
  return <Tag color={color}>{status}</Tag>;
}

function parseJson(value: string) {
  try {
    return JSON.parse(value);
  } catch {
    throw new Error('JSON 格式不正确');
  }
}

const panelStyle = {
  background: '#fff',
  border: '1px solid #e4e8f0',
  borderRadius: 8,
  padding: 16
};

const preStyle = {
  background: '#f7f8fa',
  border: '1px solid #e4e8f0',
  borderRadius: 6,
  margin: 0,
  maxHeight: 180,
  overflow: 'auto',
  padding: 12
};
