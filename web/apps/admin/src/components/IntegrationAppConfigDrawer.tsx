import { DeleteOutlined, KeyOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Descriptions, Drawer, Form, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import { listBots } from '../api/bots';
import {
  createIntegrationAppScope,
  createIntegrationAppSecret,
  deleteIntegrationAppScope,
  deleteIntegrationAppSecret,
  listIntegrationAppScopes,
  listIntegrationAppSecrets,
  type IntegrationApp,
  type IntegrationAppScope,
  type IntegrationAppSecret,
  type PageResponse
} from '../api/identity';
import { listKnowledgeBases } from '../api/knowledge';
import { listModelProviders } from '../api/models';
import { listWorkflows } from '../api/workflows';

const SCOPE_BOT = 'BOT';
const SCOPE_KNOWLEDGE_BASE = 'KNOWLEDGE_BASE';
const SCOPE_WORKFLOW = 'WORKFLOW';
const SCOPE_MODEL_PROVIDER = 'MODEL_PROVIDER';
const SCOPE_TENANT = 'TENANT';
const ASSET_LIST_STALE_MS = 5 * 60 * 1000;
const APP_CONFIG_STALE_MS = 30 * 1000;
const EMPTY_PAGE = { items: [], total: 0 };

async function loadScopes(appId: string, tenantId: string) {
  try {
    return await listIntegrationAppScopes(appId, tenantId);
  } catch {
    return EMPTY_PAGE;
  }
}

async function loadSecrets(appId: string, tenantId: string) {
  try {
    return await listIntegrationAppSecrets(appId, tenantId);
  } catch {
    return EMPTY_PAGE;
  }
}

type ScopeFormValues = {
  scopeType: string;
  assetIds: string[];
};

export interface IntegrationAppConfigDrawerProps {
  open: boolean;
  app: IntegrationApp | null;
  tenantId: string;
  onClose: () => void;
}

export function IntegrationAppConfigDrawer({ open, app, tenantId, onClose }: IntegrationAppConfigDrawerProps) {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<ScopeFormValues>();
  const [generatedKey, setGeneratedKey] = useState<string | null>(null);
  const scopeType = Form.useWatch('scopeType', form) ?? SCOPE_BOT;

  const scopesQuery = useQuery({
    queryKey: ['integration-app-scopes', tenantId, app?.id],
    queryFn: () => loadScopes(app!.id, tenantId),
    enabled: open && Boolean(app?.id),
    staleTime: APP_CONFIG_STALE_MS,
    retry: false
  });
  const secretsQuery = useQuery({
    queryKey: ['integration-app-secrets', tenantId, app?.id],
    queryFn: () => loadSecrets(app!.id, tenantId),
    enabled: open && Boolean(app?.id),
    staleTime: APP_CONFIG_STALE_MS,
    retry: false
  });
  const scopes = scopesQuery.data?.items ?? [];
  const secrets = secretsQuery.data?.items ?? [];
  const needsBots = open && (scopeType === SCOPE_BOT || scopes.some((scope) => scope.scopeType === SCOPE_BOT));
  const needsKnowledgeBases = open && (scopeType === SCOPE_KNOWLEDGE_BASE || scopes.some((scope) => scope.scopeType === SCOPE_KNOWLEDGE_BASE));
  const needsWorkflows = open && (scopeType === SCOPE_WORKFLOW || scopes.some((scope) => scope.scopeType === SCOPE_WORKFLOW));
  const needsModels = open && (scopeType === SCOPE_MODEL_PROVIDER || scopes.some((scope) => scope.scopeType === SCOPE_MODEL_PROVIDER));
  const botsQuery = useQuery({
    queryKey: ['bots'],
    queryFn: listBots,
    enabled: needsBots,
    staleTime: ASSET_LIST_STALE_MS,
    retry: 1
  });
  const knowledgeBasesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases,
    enabled: needsKnowledgeBases,
    staleTime: ASSET_LIST_STALE_MS,
    retry: 1
  });
  const workflowsQuery = useQuery({
    queryKey: ['workflows'],
    queryFn: listWorkflows,
    enabled: needsWorkflows,
    staleTime: ASSET_LIST_STALE_MS,
    retry: 1
  });
  const modelsQuery = useQuery({
    queryKey: ['model-providers'],
    queryFn: listModelProviders,
    enabled: needsModels,
    staleTime: ASSET_LIST_STALE_MS,
    retry: 1
  });

  const bots = botsQuery.data?.items ?? [];
  const knowledgeBases = knowledgeBasesQuery.data?.items ?? [];
  const workflows = workflowsQuery.data?.items ?? [];
  const models = modelsQuery.data?.items ?? [];

  const botOptions = useMemo(
    () => bots.filter((item) => item.status === 'ENABLED').map((item) => ({ value: item.id, label: item.name })),
    [bots]
  );
  const knowledgeBaseOptions = useMemo(
    () => knowledgeBases.filter((item) => item.status !== 'DISABLED' && item.status !== 'DELETED').map((item) => ({ value: item.id, label: item.name })),
    [knowledgeBases]
  );
  const workflowOptions = useMemo(
    () => workflows.filter((item) => item.status !== 'ARCHIVED').map((item) => ({ value: item.id, label: item.name })),
    [workflows]
  );
  const modelOptions = useMemo(
    () => models.filter((item) => item.enabled).map((item) => ({ value: item.id, label: `${item.name} / ${item.model}` })),
    [models]
  );
  const scopedAssetIds = useMemo(() => {
    const ids = new Set<string>();
    scopes.forEach((scope) => {
      if (scope.scopeType === scopeType) {
        ids.add(scope.scopeId);
      }
    });
    return ids;
  }, [scopes, scopeType]);
  const selectableBotOptions = useMemo(
    () => botOptions.filter((item) => !scopedAssetIds.has(item.value)),
    [botOptions, scopedAssetIds]
  );
  const selectableKnowledgeBaseOptions = useMemo(
    () => knowledgeBaseOptions.filter((item) => !scopedAssetIds.has(item.value)),
    [knowledgeBaseOptions, scopedAssetIds]
  );
  const selectableWorkflowOptions = useMemo(
    () => workflowOptions.filter((item) => !scopedAssetIds.has(item.value)),
    [workflowOptions, scopedAssetIds]
  );
  const selectableModelOptions = useMemo(
    () => modelOptions.filter((item) => !scopedAssetIds.has(item.value)),
    [modelOptions, scopedAssetIds]
  );

  useEffect(() => {
    if (open) {
      form.setFieldsValue({ scopeType: SCOPE_BOT, assetIds: [] });
    }
  }, [open, app?.id, form]);

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['integration-app-scopes', tenantId, app?.id] });
    await queryClient.invalidateQueries({ queryKey: ['integration-app-secrets', tenantId, app?.id] });
  };

  const createSecretMutation = useMutation({
    mutationFn: () => createIntegrationAppSecret(app!.id, tenantId),
    onSuccess: async (secret) => {
      setGeneratedKey(secret.apiKey);
      queryClient.setQueryData<PageResponse<IntegrationAppSecret>>(
        ['integration-app-secrets', tenantId, app?.id],
        (current) => {
          const items = current?.items ?? [];
          const nextItem: IntegrationAppSecret = {
            id: secret.id,
            secretPrefix: secret.secretPrefix,
            enabled: true
          };
          const filtered = items.filter((item) => item.id !== secret.id);
          return { items: [nextItem, ...filtered], total: filtered.length + 1 };
        }
      );
      await queryClient.refetchQueries({ queryKey: ['integration-app-secrets', tenantId, app?.id] });
    }
  });

  const deleteSecretMutation = useMutation({
    mutationFn: (secretId: string) => deleteIntegrationAppSecret(app!.id, secretId, tenantId),
    onSuccess: async () => {
      message.success('密钥已删除');
      await invalidate();
    }
  });

  const addScopesMutation = useMutation({
    mutationFn: async (values: ScopeFormValues) => {
      const assetIds = values.scopeType === SCOPE_TENANT ? [tenantId] : values.assetIds ?? [];
      for (const assetId of assetIds) {
        await createIntegrationAppScope(app!.id, {
          scopeType: values.scopeType,
          scopeId: assetId,
          permission: 'USE'
        }, tenantId);
      }
    },
    onSuccess: async () => {
      message.success('白名单已更新');
      form.setFieldsValue({ assetIds: [] });
      await queryClient.refetchQueries({ queryKey: ['integration-app-scopes', tenantId, app?.id] });
    },
    onError: (error: Error) => {
      message.error(error.message || '添加失败');
    }
  });

  const deleteScopeMutation = useMutation({
    mutationFn: (scopeId: string) => deleteIntegrationAppScope(app!.id, scopeId, tenantId),
    onSuccess: async () => {
      message.success('已移除范围');
      await invalidate();
    }
  });

  const resolveScopeLabel = (scope: IntegrationAppScope) => {
    if (scope.scopeType === SCOPE_BOT) {
      return bots.find((item) => item.id === scope.scopeId)?.name ?? scope.scopeId;
    }
    if (scope.scopeType === SCOPE_KNOWLEDGE_BASE) {
      return knowledgeBases.find((item) => item.id === scope.scopeId)?.name ?? scope.scopeId;
    }
    if (scope.scopeType === SCOPE_WORKFLOW) {
      return workflows.find((item) => item.id === scope.scopeId)?.name ?? scope.scopeId;
    }
    if (scope.scopeType === SCOPE_MODEL_PROVIDER) {
      const model = models.find((item) => item.id === scope.scopeId);
      return model ? `${model.name} / ${model.model}` : scope.scopeId;
    }
    if (scope.scopeType === SCOPE_TENANT) {
      return scope.scopeId === tenantId ? '当前租户（全部资产）' : scope.scopeId;
    }
    return scope.scopeId;
  };

  const scopeColumns: ColumnsType<IntegrationAppScope> = [
    { title: '类型', dataIndex: 'scopeType', width: 120, render: (value: string) => scopeTypeLabel(value) },
    { title: '资源', render: (_, scope) => resolveScopeLabel(scope) },
    { title: '权限', dataIndex: 'permission', width: 90, render: (value: string) => <Tag>{value}</Tag> },
    {
      title: '操作',
      width: 90,
      render: (_, scope) => (
        <Button
          danger
          size="small"
          icon={<DeleteOutlined />}
          onClick={() => deleteScopeMutation.mutate(scope.id)}
          loading={deleteScopeMutation.isPending}
        >
          移除
        </Button>
      )
    }
  ];

  return (
    <>
      <Drawer
        title={app ? `应用配置：${app.name}` : '应用配置'}
        open={open}
        width="min(960px, 94vw)"
        styles={{ body: { paddingTop: 16 } }}
        onClose={onClose}
      >
        {app ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              type="info"
              showIcon
              message="第三方系统调用开放 API 时使用以下请求头"
              description={(
                <Typography.Paragraph copyable={{ text: `X-AGI-App-Code: ${app.code}\nX-AGI-Api-Key: <生成的密钥>` }} style={{ marginBottom: 0 }}>
                  <div><Typography.Text code>X-AGI-App-Code</Typography.Text> = {app.code}</div>
                  <div><Typography.Text code>X-AGI-Api-Key</Typography.Text> = 下方生成的密钥</div>
                  <div><Typography.Text code>X-AGI-User-Id</Typography.Text> = 第三方用户 ID（可选，用于审计）</div>
                </Typography.Paragraph>
              )}
            />

            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="应用编码">{app.code}</Descriptions.Item>
              <Descriptions.Item label="认证方式">{app.authType}</Descriptions.Item>
              <Descriptions.Item label="状态">{app.status}</Descriptions.Item>
              <Descriptions.Item label="类型">{app.appType}</Descriptions.Item>
            </Descriptions>

            <Card title="API Key" size="small">
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Space wrap>
                  <Button type="primary" icon={<KeyOutlined />} loading={createSecretMutation.isPending} onClick={() => createSecretMutation.mutate()}>
                    生成 API Key
                  </Button>
                  <Typography.Text type="secondary">密钥只展示一次，请立即保存。</Typography.Text>
                </Space>
                <Table
                  rowKey="id"
                  size="small"
                  pagination={false}
                  loading={secretsQuery.isPending}
                  dataSource={secrets}
                  locale={{ emptyText: '尚未生成密钥' }}
                  columns={[
                    { title: '前缀', dataIndex: 'secretPrefix', render: (value: string) => <Typography.Text code>{value}…</Typography.Text> },
                    { title: '状态', dataIndex: 'enabled', width: 90, render: (value: boolean) => <Tag color={value ? 'green' : 'default'}>{value ? '启用' : '停用'}</Tag> },
                    { title: '创建时间', dataIndex: 'createdAt', render: (value?: string) => value ?? '-' },
                    {
                      title: '操作',
                      width: 90,
                      render: (_, secret) => (
                        <Button
                          danger
                          size="small"
                          icon={<DeleteOutlined />}
                          onClick={() => deleteSecretMutation.mutate(secret.id)}
                          loading={deleteSecretMutation.isPending}
                        >
                          删除
                        </Button>
                      )
                    }
                  ]}
                />
              </Space>
            </Card>

            <Card title="资产白名单" size="small">
              <Typography.Paragraph type="secondary" style={{ marginTop: 0 }}>
                配置本应用可调用的智能体、知识库、工作流、大模型。未配置时无法调用开放 API；配置租户范围则允许访问租户内全部资产。
              </Typography.Paragraph>
              <Form
                form={form}
                layout="vertical"
                onFinish={(values) => addScopesMutation.mutate(values)}
                style={{ marginBottom: 16 }}
              >
                <Form.Item name="scopeType" label="范围类型" rules={[{ required: true }]}>
                  <Select
                    style={{ maxWidth: 240 }}
                    onChange={() => form.setFieldValue('assetIds', [])}
                    options={[
                      { value: SCOPE_BOT, label: '智能体' },
                      { value: SCOPE_KNOWLEDGE_BASE, label: '知识库' },
                      { value: SCOPE_WORKFLOW, label: '工作流' },
                      { value: SCOPE_MODEL_PROVIDER, label: '大模型' },
                      { value: SCOPE_TENANT, label: '租户（全部）' }
                    ]}
                  />
                </Form.Item>
                {scopeType === SCOPE_BOT ? (
                  <Form.Item name="assetIds" label="选择智能体" rules={[{ required: true, message: '请选择智能体' }]}>
                    <Select
                      key="scope-bot"
                      mode="multiple"
                      showSearch
                      allowClear
                      maxTagCount="responsive"
                      listHeight={320}
                      options={selectableBotOptions}
                      optionLabelProp="label"
                      optionFilterProp="label"
                      placeholder="搜索并选择智能体，可多选"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                ) : null}
                {scopeType === SCOPE_KNOWLEDGE_BASE ? (
                  <Form.Item name="assetIds" label="选择知识库" rules={[{ required: true, message: '请选择知识库' }]}>
                    <Select
                      key="scope-kb"
                      mode="multiple"
                      showSearch
                      allowClear
                      maxTagCount="responsive"
                      listHeight={320}
                      options={selectableKnowledgeBaseOptions}
                      optionLabelProp="label"
                      optionFilterProp="label"
                      placeholder="搜索并选择知识库，可多选"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                ) : null}
                {scopeType === SCOPE_WORKFLOW ? (
                  <Form.Item name="assetIds" label="选择工作流" rules={[{ required: true, message: '请选择工作流' }]}>
                    <Select
                      key="scope-workflow"
                      mode="multiple"
                      showSearch
                      allowClear
                      maxTagCount="responsive"
                      listHeight={320}
                      options={selectableWorkflowOptions}
                      optionLabelProp="label"
                      optionFilterProp="label"
                      placeholder="搜索并选择工作流，可多选"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                ) : null}
                {scopeType === SCOPE_MODEL_PROVIDER ? (
                  <Form.Item name="assetIds" label="选择大模型" rules={[{ required: true, message: '请选择大模型' }]}>
                    <Select
                      key="scope-model"
                      mode="multiple"
                      showSearch
                      allowClear
                      maxTagCount="responsive"
                      listHeight={320}
                      options={selectableModelOptions}
                      optionLabelProp="label"
                      optionFilterProp="label"
                      placeholder="搜索并选择大模型，可多选"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                ) : null}
                {scopeType === SCOPE_TENANT ? (
                  <Form.Item label="租户范围">
                    <Typography.Text code>{tenantId}</Typography.Text>
                    <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>允许访问当前租户内全部资产。</Typography.Paragraph>
                  </Form.Item>
                ) : null}
                <Form.Item style={{ marginBottom: 0 }}>
                  <Button type="primary" htmlType="submit" icon={<PlusOutlined />} loading={addScopesMutation.isPending}>
                    添加到白名单
                  </Button>
                </Form.Item>
              </Form>
              <Table
                rowKey="id"
                size="small"
                loading={scopesQuery.isPending}
                pagination={{ pageSize: 8, hideOnSinglePage: true, showSizeChanger: false }}
                dataSource={scopes}
                locale={{ emptyText: '尚未配置白名单' }}
                columns={scopeColumns}
              />
            </Card>
          </Space>
        ) : null}
      </Drawer>

      <Modal
        title="一次性 API Key"
        open={Boolean(generatedKey)}
        onCancel={() => setGeneratedKey(null)}
        footer={<Button type="primary" onClick={() => setGeneratedKey(null)}>我已保存</Button>}
      >
        <Alert type="warning" showIcon message="密钥只展示一次，请立即复制保存。" style={{ marginBottom: 12 }} />
        <Typography.Text code style={{ userSelect: 'all', wordBreak: 'break-all' }}>{generatedKey}</Typography.Text>
      </Modal>
    </>
  );
}

function scopeTypeLabel(value: string) {
  if (value === SCOPE_BOT) return '智能体';
  if (value === SCOPE_KNOWLEDGE_BASE) return '知识库';
  if (value === SCOPE_WORKFLOW) return '工作流';
  if (value === SCOPE_MODEL_PROVIDER) return '大模型';
  if (value === SCOPE_TENANT) return '租户';
  return value;
}
