import type { WorkflowNode } from '@aiworkflow/workflow-schema';
import { Alert, Checkbox, Collapse, Empty, Form, Input, InputNumber, Select, Space, Switch, Tag, Typography } from 'antd';
import type React from 'react';
import type { KnowledgeBase } from '../../../api/knowledge';
import type { ModelProvider } from '../../../api/models';
import type { PromptTemplate } from '../../../api/prompts';

export interface NodeConfigPanelProps {
  node: WorkflowNode | null;
  onChange: (nodeId: string, patch: Partial<WorkflowNode>) => void;
  modelProviders?: ModelProvider[];
  promptTemplates?: PromptTemplate[];
  knowledgeBases?: KnowledgeBase[];
}

const splitterOptions = [
  { value: 'NONE', label: '不处理，直接透传' },
  { value: 'JSON', label: 'JSON 解析' },
  { value: 'TEXT', label: '文本' }
];

export function NodeConfigPanel({ node, onChange, modelProviders = [], promptTemplates = [], knowledgeBases = [] }: NodeConfigPanelProps) {
  if (!node) {
    return (
      <aside style={panelStyle}>
        <Typography.Title level={5} style={titleStyle}>节点属性</Typography.Title>
        <Empty description="请选择画布上的节点" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </aside>
    );
  }

  const config = node.config ?? {};
  const setConfig = (patch: Record<string, unknown>) => onChange(node.id, { config: { ...config, ...patch } });

  return (
    <aside style={panelStyle}>
      <Space direction="vertical" size={2} style={{ width: '100%', marginBottom: 12 }}>
        <Typography.Title level={5} style={titleStyle}>节点属性</Typography.Title>
        <Space wrap>
          <Tag color="blue">{node.type}</Tag>
          <Typography.Text type="secondary">{node.id}</Typography.Text>
        </Space>
      </Space>

      <Form layout="vertical" size="small">
        <Collapse
          defaultActiveKey={['basic', 'config']}
          bordered={false}
          items={[
            {
              key: 'basic',
              label: '基础信息',
              children: (
                <>
                  <Form.Item label="节点名称">
                    <Input value={node.name} onChange={(event) => onChange(node.id, { name: event.target.value })} />
                  </Form.Item>
                  <Form.Item label="节点说明">
                    <Input.TextArea
                      autoSize={{ minRows: 2, maxRows: 4 }}
                      placeholder="说明该节点在业务流程里的职责"
                      value={String(config.description ?? '')}
                      onChange={(event) => setConfig({ description: event.target.value })}
                    />
                  </Form.Item>
                  <Space size={8} align="start" wrap>
                    <Form.Item label="超时(ms)">
                      <InputNumber min={1000} max={600000} step={1000} value={numberValue(config.timeoutMs, 30000)} onChange={(value) => setConfig({ timeoutMs: value ?? 30000 })} />
                    </Form.Item>
                    <Form.Item label="重试次数">
                      <InputNumber min={0} max={10} value={numberValue(config.retryCount, 0)} onChange={(value) => setConfig({ retryCount: value ?? 0 })} />
                    </Form.Item>
                    <Form.Item label="失败策略">
                      <Select
                        style={{ width: 138 }}
                        value={stringValue(config.failPolicy, 'FAIL_WORKFLOW')}
                        options={[
                          { value: 'FAIL_WORKFLOW', label: '终止流程' },
                          { value: 'CONTINUE', label: '继续执行' },
                          { value: 'USE_FALLBACK', label: '使用兜底值' }
                        ]}
                        onChange={(failPolicy) => setConfig({ failPolicy })}
                      />
                    </Form.Item>
                  </Space>
                </>
              )
            },
            {
              key: 'config',
              label: '节点配置',
              children: (
                <ConfigFields
                  node={node}
                  setConfig={setConfig}
                  modelProviders={modelProviders}
                  promptTemplates={promptTemplates}
                  knowledgeBases={knowledgeBases}
                />
              )
            },
            {
              key: 'io',
              label: '输入输出约定',
              children: (
                <>
                  <Form.Item label="输入变量映射">
                    <Input.TextArea
                      autoSize={{ minRows: 3, maxRows: 6 }}
                      placeholder={'JSON，例如：\n{"question":"input.question","userId":"input.userId"}'}
                      value={String(config.inputMappingJson ?? '')}
                      onChange={(event) => setConfig({ inputMappingJson: event.target.value })}
                    />
                  </Form.Item>
                  <Form.Item label="输出变量说明">
                    <Input.TextArea
                      autoSize={{ minRows: 2, maxRows: 5 }}
                      placeholder="描述该节点会写入哪些上下文变量，便于下游引用"
                      value={String(config.outputSchema ?? '')}
                      onChange={(event) => setConfig({ outputSchema: event.target.value })}
                    />
                  </Form.Item>
                  <Alert
                    type="info"
                    showIcon
                    message="变量引用约定"
                    description="当前引擎上下文以 key/value 形式传递，上游 outputKey 会写入全局上下文。模板中可使用 {{question}}、{{contexts}} 一类变量。"
                  />
                </>
              )
            }
          ]}
        />
      </Form>
    </aside>
  );
}

interface ConfigFieldsProps {
  node: WorkflowNode;
  setConfig: (patch: Record<string, unknown>) => void;
  modelProviders: ModelProvider[];
  promptTemplates: PromptTemplate[];
  knowledgeBases: KnowledgeBase[];
}

function ConfigFields({ node, setConfig, modelProviders, promptTemplates, knowledgeBases }: ConfigFieldsProps) {
  const config = node.config ?? {};

  if (node.type === 'START') {
    return (
      <>
        <Form.Item label="入参变量">
          <Input
            placeholder="question,userId,sessionId"
            value={arrayOrStringValue(config.inputKeys)}
            onChange={(event) => setConfig({ inputKeys: splitCsv(event.target.value) })}
          />
        </Form.Item>
        <Form.Item label="调试默认输入">
          <Input.TextArea
            autoSize={{ minRows: 5, maxRows: 10 }}
            placeholder={'{\n  "question": "请介绍退款政策"\n}'}
            value={String(config.defaultInputJson ?? '')}
            onChange={(event) => setConfig({ defaultInputJson: event.target.value })}
          />
        </Form.Item>
        <Form.Item label="入参格式">
          <Select
            value={stringValue(config.inputMode, 'JSON')}
            options={splitterOptions}
            onChange={(inputMode) => setConfig({ inputMode })}
          />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'PROMPT') {
    const selectedPromptId = typeof config.promptTemplateId === 'string' ? config.promptTemplateId : undefined;
    return (
      <>
        <Form.Item label="已保存 Prompt">
          <Select
            allowClear
            aria-label="选择已保存 Prompt"
            placeholder="选择 Prompt 模板"
            value={selectedPromptId}
            options={promptTemplates.map((prompt) => ({ value: prompt.id, label: prompt.name }))}
            onChange={(promptTemplateId) => {
              const prompt = promptTemplates.find((item) => item.id === promptTemplateId);
              setConfig({
                promptTemplateId,
                template: prompt?.template ?? String(config.template ?? '')
              });
            }}
          />
        </Form.Item>
        <Form.Item label="Prompt 模板">
          <Input.TextArea
            autoSize={{ minRows: 7, maxRows: 14 }}
            placeholder="你是企业客服助手。请结合 {{contexts}} 回答：{{question}}"
            value={String(config.template ?? '')}
            onChange={(event) => setConfig({ template: event.target.value })}
          />
        </Form.Item>
        <Space size={8} align="start" wrap>
          <Form.Item label="输出变量">
            <Input value={String(config.outputKey ?? 'prompt')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
          </Form.Item>
          <Form.Item label="缺失变量">
            <Select
              style={{ width: 138 }}
              value={stringValue(config.missingVariablePolicy, 'EMPTY')}
              options={[
                { value: 'EMPTY', label: '置为空' },
                { value: 'KEEP_TOKEN', label: '保留占位符' },
                { value: 'FAIL', label: '报错' }
              ]}
              onChange={(missingVariablePolicy) => setConfig({ missingVariablePolicy })}
            />
          </Form.Item>
        </Space>
      </>
    );
  }

  if (node.type === 'LLM') {
    const enabledProviders = modelProviders.filter((provider) => provider.enabled && provider.modelUsage !== 'EMBEDDING');
    const selectedProvider = enabledProviders.find((provider) => provider.id === config.providerId);
    return (
      <>
        <Form.Item label="模型配置">
          <Select
            allowClear
            aria-label="选择已保存模型"
            placeholder="选择模型配置"
            value={typeof config.providerId === 'string' && config.providerId ? String(config.providerId) : undefined}
            options={enabledProviders.map((provider) => ({
              value: provider.id,
              label: `${provider.name} / ${provider.model}`
            }))}
            onChange={(providerId) => {
              const provider = enabledProviders.find((item) => item.id === providerId);
              setConfig({ providerId, model: provider?.model ?? '' });
            }}
          />
        </Form.Item>
        {selectedProvider ? (
          <Alert
            type="info"
            showIcon
            message={`${selectedProvider.modelType} / ${selectedProvider.model}`}
            description={`Base URL: ${selectedProvider.baseUrl}，密钥引用：${selectedProvider.apiKeyRef}`}
            style={{ marginBottom: 12 }}
          />
        ) : null}
        <Form.Item label="模型标识">
          <Input value={String(config.model ?? selectedProvider?.model ?? '')} onChange={(event) => setConfig({ model: event.target.value })} />
        </Form.Item>
        <Form.Item label="系统提示词">
          <Input.TextArea
            autoSize={{ minRows: 3, maxRows: 8 }}
            placeholder="定义模型角色、回答边界、安全规则"
            value={String(config.systemPrompt ?? '')}
            onChange={(event) => setConfig({ systemPrompt: event.target.value })}
          />
        </Form.Item>
        <Space size={8} align="start" wrap>
          <Form.Item label="Prompt 变量">
            <Input value={String(config.promptKey ?? 'prompt')} onChange={(event) => setConfig({ promptKey: event.target.value })} />
          </Form.Item>
          <Form.Item label="输出变量">
            <Input value={String(config.outputKey ?? 'answer')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
          </Form.Item>
        </Space>
        <Space size={8} align="start" wrap>
          <Form.Item label="温度">
            <InputNumber min={0} max={2} step={0.1} value={numberValue(config.temperature, 0.7)} onChange={(temperature) => setConfig({ temperature: temperature ?? 0 })} />
          </Form.Item>
          <Form.Item label="Top P">
            <InputNumber min={0} max={1} step={0.05} value={numberValue(config.topP, 1)} onChange={(topP) => setConfig({ topP: topP ?? 1 })} />
          </Form.Item>
          <Form.Item label="最大 Token">
            <InputNumber min={1} max={32000} value={numberValue(config.maxTokens, 1024)} onChange={(maxTokens) => setConfig({ maxTokens: maxTokens ?? 1024 })} />
          </Form.Item>
        </Space>
        <Space size={16} wrap>
          <Form.Item label="响应格式">
            <Select
              style={{ width: 130 }}
              value={stringValue(config.responseFormat, 'TEXT')}
              options={[
                { value: 'TEXT', label: '文本' },
                { value: 'JSON', label: 'JSON' }
              ]}
              onChange={(responseFormat) => setConfig({ responseFormat })}
            />
          </Form.Item>
          <Form.Item label="流式输出">
            <Switch checked={Boolean(config.stream)} onChange={(stream) => setConfig({ stream })} />
          </Form.Item>
        </Space>
      </>
    );
  }

  if (node.type === 'KNOWLEDGE_RETRIEVAL') {
    const selectedKnowledgeBaseId = typeof config.knowledgeBaseId === 'string' ? config.knowledgeBaseId : undefined;
    return (
      <>
        <Form.Item label="知识库">
          <Select
            allowClear
            aria-label="选择已保存知识库"
            placeholder="选择知识库"
            value={selectedKnowledgeBaseId}
            options={knowledgeBases.map((knowledgeBase) => ({
              value: knowledgeBase.id,
              label: `${knowledgeBase.name} (${knowledgeBase.documentCount} 文档 / ${knowledgeBase.chunkCount} 切片)`
            }))}
            onChange={(knowledgeBaseId) => setConfig({ knowledgeBaseId })}
          />
        </Form.Item>
        <Space size={8} align="start" wrap>
          <Form.Item label="查询变量">
            <Input value={String(config.queryKey ?? 'question')} onChange={(event) => setConfig({ queryKey: event.target.value })} />
          </Form.Item>
          <Form.Item label="输出变量">
            <Input value={String(config.outputKey ?? 'contexts')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
          </Form.Item>
        </Space>
        <Space size={8} align="start" wrap>
          <Form.Item label="Top K">
            <InputNumber min={1} max={20} value={numberValue(config.topK, 3)} onChange={(topK) => setConfig({ topK: topK ?? 3 })} />
          </Form.Item>
          <Form.Item label="最低分">
            <InputNumber min={0} max={1} step={0.05} value={numberValue(config.minScore, 0)} onChange={(minScore) => setConfig({ minScore: minScore ?? 0 })} />
          </Form.Item>
          <Form.Item label="重排">
            <Switch checked={Boolean(config.rerank)} onChange={(rerank) => setConfig({ rerank })} />
          </Form.Item>
        </Space>
        <Form.Item label="结果模板">
          <Input.TextArea
            autoSize={{ minRows: 3, maxRows: 8 }}
            placeholder="可选：把召回片段整理成给 LLM 的上下文"
            value={String(config.resultTemplate ?? '')}
            onChange={(event) => setConfig({ resultTemplate: event.target.value })}
          />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'HTTP_TOOL') {
    return (
      <>
        <Space size={8} align="start" wrap>
          <Form.Item label="请求方法">
            <Select
              style={{ width: 118 }}
              value={stringValue(config.method, 'POST')}
              options={['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].map((method) => ({ value: method, label: method }))}
              onChange={(method) => setConfig({ method })}
            />
          </Form.Item>
          <Form.Item label="输出变量">
            <Input value={String(config.outputKey ?? 'toolResult')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
          </Form.Item>
        </Space>
        <Form.Item label="请求地址">
          <Input placeholder="https://api.example.com/orders/{{orderId}}" value={String(config.url ?? '')} onChange={(event) => setConfig({ url: event.target.value })} />
        </Form.Item>
        <Form.Item label="认证方式">
          <Select
            value={stringValue(config.authType, 'NONE')}
            options={[
              { value: 'NONE', label: '无' },
              { value: 'BEARER', label: 'Bearer Token' },
              { value: 'API_KEY', label: 'API Key' }
            ]}
            onChange={(authType) => setConfig({ authType })}
          />
        </Form.Item>
        <Form.Item label="请求头 JSON">
          <Input.TextArea
            autoSize={{ minRows: 3, maxRows: 8 }}
            placeholder={'{\n  "Content-Type": "application/json"\n}'}
            value={String(config.headersJson ?? '')}
            onChange={(event) => setConfig({ headersJson: event.target.value })}
          />
        </Form.Item>
        <Form.Item label="请求体模板">
          <Input.TextArea
            autoSize={{ minRows: 5, maxRows: 12 }}
            placeholder={'{\n  "question": "{{question}}"\n}'}
            value={String(config.bodyTemplate ?? '')}
            onChange={(event) => setConfig({ bodyTemplate: event.target.value })}
          />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'CONDITION') {
    return (
      <>
        <Form.Item label="JavaScript 条件表达式">
          <Input.TextArea
            autoSize={{ minRows: 3, maxRows: 7 }}
            placeholder={'例如：_state.score >= 0.8 或 answer.includes("通过")'}
            value={String(config.expression ?? '')}
            onChange={(event) => setConfig({ expression: event.target.value })}
          />
        </Form.Item>
        <Alert
          type="warning"
          showIcon
          message="当前后端会优先使用下方兼容条件执行"
          description="表达式先作为产品配置保存；当前轻量引擎运行时使用 contextKey + operator/equals 来选择 True/False 目标。"
          style={{ marginBottom: 12 }}
        />
        <Space size={8} align="start" wrap>
          <Form.Item label="判断变量">
            <Input value={String(config.contextKey ?? '')} onChange={(event) => setConfig({ contextKey: event.target.value })} />
          </Form.Item>
          <Form.Item label="操作符">
            <Select
              style={{ width: 150 }}
              value={stringValue(config.operator, 'EQUALS')}
              options={['EQUALS', 'NOT_EQUALS', 'CONTAINS', 'IS_EMPTY', 'IS_NOT_EMPTY'].map((value) => ({ value, label: value }))}
              onChange={(operator) => setConfig({ operator })}
            />
          </Form.Item>
          <Form.Item label="比较值">
            <Input
              value={String(config.compareValue ?? config.equals ?? '')}
              onChange={(event) => setConfig({ compareValue: event.target.value, equals: event.target.value })}
            />
          </Form.Item>
        </Space>
        <Space size={8} align="start" wrap>
          <Form.Item label="True 目标节点">
            <Input value={String(config.trueTargetNodeId ?? '')} onChange={(event) => setConfig({ trueTargetNodeId: event.target.value })} />
          </Form.Item>
          <Form.Item label="False 目标节点">
            <Input value={String(config.falseTargetNodeId ?? '')} onChange={(event) => setConfig({ falseTargetNodeId: event.target.value })} />
          </Form.Item>
        </Space>
      </>
    );
  }

  if (node.type === 'TEXT_TRANSFORM') {
    return (
      <>
        <Form.Item label="转换类型">
          <Select
            value={stringValue(config.transformType, 'TEMPLATE')}
            options={[
              { value: 'TEMPLATE', label: '模板渲染' },
              { value: 'JSON_EXTRACT', label: 'JSON 提取' },
              { value: 'REGEX_EXTRACT', label: '正则提取' }
            ]}
            onChange={(transformType) => setConfig({ transformType })}
          />
        </Form.Item>
        <Form.Item label="文本模板">
          <Input.TextArea
            autoSize={{ minRows: 6, maxRows: 12 }}
            placeholder="客户问题：{{question}}\n知识片段：{{contexts}}"
            value={String(config.template ?? '')}
            onChange={(event) => setConfig({ template: event.target.value })}
          />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? 'text')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'END') {
    return (
      <>
        <Form.Item label="输出变量列表">
          <Input
            placeholder="answer,contexts"
            value={arrayOrStringValue(config.outputKeys)}
            onChange={(event) => setConfig({ outputKeys: splitCsv(event.target.value) })}
          />
        </Form.Item>
        <Form.Item label="响应模板">
          <Input.TextArea
            autoSize={{ minRows: 4, maxRows: 10 }}
            placeholder={'{\n  "answer": "{{answer}}"\n}'}
            value={String(config.responseTemplate ?? '')}
            onChange={(event) => setConfig({ responseTemplate: event.target.value })}
          />
        </Form.Item>
        <Checkbox checked={Boolean(config.includeExecutionMeta)} onChange={(event) => setConfig({ includeExecutionMeta: event.target.checked })}>
          返回执行元数据
        </Checkbox>
      </>
    );
  }

  return <Typography.Text type="secondary">该节点暂无额外配置。</Typography.Text>;
}

function stringValue(value: unknown, fallback: string) {
  return typeof value === 'string' && value ? value : fallback;
}

function numberValue(value: unknown, fallback: number) {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function arrayOrStringValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.join(',');
  }
  return typeof value === 'string' ? value : '';
}

function splitCsv(value: string) {
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

const panelStyle: React.CSSProperties = {
  background: '#fff',
  borderLeft: '0',
  height: '100%',
  overflow: 'auto',
  padding: 16,
  width: '100%'
};

const titleStyle: React.CSSProperties = {
  margin: 0
};
