import type { WorkflowNode } from '@aiworkflow/workflow-schema';
import { Empty, Form, Input, InputNumber, Select, Space, Typography } from 'antd';
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

export function NodeConfigPanel({ node, onChange, modelProviders = [], promptTemplates = [], knowledgeBases = [] }: NodeConfigPanelProps) {
  if (!node) {
    return (
      <aside style={panelStyle}>
        <Typography.Title level={5} style={titleStyle}>节点配置</Typography.Title>
        <Empty description="请选择一个节点" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </aside>
    );
  }

  return (
    <aside style={panelStyle}>
      <Typography.Title level={5} style={titleStyle}>节点配置</Typography.Title>
      <Form layout="vertical" size="small">
        <Form.Item label="节点名称">
          <Input value={node.name} onChange={(event) => onChange(node.id, { name: event.target.value })} />
        </Form.Item>
        <Form.Item label="节点类型">
          <Input value={node.type} readOnly />
        </Form.Item>
        <ConfigFields node={node} onChange={onChange} modelProviders={modelProviders} promptTemplates={promptTemplates} knowledgeBases={knowledgeBases} />
      </Form>
    </aside>
  );
}

function ConfigFields({ node, onChange, modelProviders = [], promptTemplates = [], knowledgeBases = [] }: NodeConfigPanelProps & { node: WorkflowNode }) {
  const config = node.config;
  const setConfig = (patch: Record<string, unknown>) => onChange(node.id, { config: { ...config, ...patch } });

  if (node.type === 'PROMPT') {
    const selectedPromptId = typeof config.promptTemplateId === 'string' ? config.promptTemplateId : undefined;
    return (
      <>
        {promptTemplates.length > 0 ? (
          <Form.Item label="已保存 Prompt">
            <Select
              aria-label="选择已保存 Prompt"
              placeholder="选择 Prompt 模板"
              value={selectedPromptId}
              options={promptTemplates.map((prompt) => ({
                value: prompt.id,
                label: prompt.name
              }))}
              onChange={(promptId) => {
                const prompt = promptTemplates.find((item) => item.id === promptId);
                setConfig({
                  promptTemplateId: promptId,
                  template: prompt?.template ?? String(config.template ?? '')
                });
              }}
            />
          </Form.Item>
        ) : (
          <Typography.Text type="secondary" style={hintStyle}>
            请先在 Prompt 模块保存模板，或在下方临时手填。
          </Typography.Text>
        )}
        <Form.Item label="Prompt 模板">
          <Input.TextArea
            autoSize={{ minRows: 5, maxRows: 9 }}
            value={String(config.template ?? '')}
            onChange={(event) => setConfig({ template: event.target.value })}
          />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? '')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'LLM') {
    const enabledProviders = modelProviders.filter((provider) => provider.enabled);
    const selectedProvider = enabledProviders.find((provider) => provider.id === config.providerId);
    return (
      <>
        {enabledProviders.length > 0 ? (
          <>
            <Form.Item label="已保存模型">
              <Select
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
            <Form.Item label="模型标识">
              <Input value={String(selectedProvider?.model ?? config.model ?? '')} onChange={(event) => setConfig({ model: event.target.value })} />
            </Form.Item>
            {selectedProvider ? (
              <Typography.Text type="secondary" style={hintStyle}>
                Base URL：{selectedProvider.baseUrl}，密钥引用：{selectedProvider.apiKeyRef}
              </Typography.Text>
            ) : null}
          </>
        ) : (
          <>
            <Form.Item label="模型供应商">
              <Input value={String(config.providerId ?? '')} onChange={(event) => setConfig({ providerId: event.target.value })} />
            </Form.Item>
            <Form.Item label="模型">
              <Input value={String(config.model ?? '')} onChange={(event) => setConfig({ model: event.target.value })} />
            </Form.Item>
            <Typography.Text type="secondary" style={hintStyle}>
              请先在“大模型配置”中保存模型，或临时手填供应商与模型标识。
            </Typography.Text>
          </>
        )}
        <Form.Item label="Prompt 变量">
          <Input value={String(config.promptKey ?? '')} onChange={(event) => setConfig({ promptKey: event.target.value })} />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? '')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
        <Space size={8}>
          <Form.Item label="温度">
            <InputNumber min={0} max={2} step={0.1} value={Number(config.temperature ?? 0.7)} onChange={(value) => setConfig({ temperature: value ?? 0 })} />
          </Form.Item>
          <Form.Item label="最大 Token">
            <InputNumber min={1} max={32000} value={Number(config.maxTokens ?? 1024)} onChange={(value) => setConfig({ maxTokens: value ?? 1024 })} />
          </Form.Item>
        </Space>
      </>
    );
  }

  if (node.type === 'KNOWLEDGE_RETRIEVAL') {
    const selectedKnowledgeBaseId = typeof config.knowledgeBaseId === 'string' ? config.knowledgeBaseId : undefined;
    return (
      <>
        {knowledgeBases.length > 0 ? (
          <Form.Item label="已保存知识库">
            <Select
              aria-label="选择已保存知识库"
              placeholder="选择知识库"
              value={selectedKnowledgeBaseId}
              options={knowledgeBases.map((knowledgeBase) => ({
                value: knowledgeBase.id,
                label: knowledgeBase.name
              }))}
              onChange={(knowledgeBaseId) => setConfig({ knowledgeBaseId })}
            />
          </Form.Item>
        ) : (
          <Typography.Text type="secondary" style={hintStyle}>
            请先在知识库中心创建知识库并完成文档入库。
          </Typography.Text>
        )}
        <Form.Item label="查询变量">
          <Input value={String(config.queryKey ?? '')} onChange={(event) => setConfig({ queryKey: event.target.value })} />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? '')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
        <Form.Item label="Top K">
          <InputNumber min={1} max={10} value={Number(config.topK ?? 3)} onChange={(value) => setConfig({ topK: value ?? 3 })} />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'CONDITION') {
    return (
      <>
        <Form.Item label="判断变量">
          <Input value={String(config.contextKey ?? '')} onChange={(event) => setConfig({ contextKey: event.target.value })} />
        </Form.Item>
        <Form.Item label="操作符">
          <Select
            value={String(config.operator ?? 'EQUALS')}
            options={['EQUALS', 'NOT_EQUALS', 'CONTAINS', 'IS_EMPTY', 'IS_NOT_EMPTY'].map((value) => ({ value, label: value }))}
            onChange={(value) => setConfig({ operator: value })}
          />
        </Form.Item>
        <Form.Item label="比较值">
          <Input value={String(config.compareValue ?? '')} onChange={(event) => setConfig({ compareValue: event.target.value })} />
        </Form.Item>
        <Form.Item label="True 目标节点">
          <Input value={String(config.trueTargetNodeId ?? '')} onChange={(event) => setConfig({ trueTargetNodeId: event.target.value })} />
        </Form.Item>
        <Form.Item label="False 目标节点">
          <Input value={String(config.falseTargetNodeId ?? '')} onChange={(event) => setConfig({ falseTargetNodeId: event.target.value })} />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'TEXT_TRANSFORM') {
    return (
      <>
        <Form.Item label="文本模板">
          <Input.TextArea
            autoSize={{ minRows: 5, maxRows: 9 }}
            value={String(config.template ?? '')}
            onChange={(event) => setConfig({ template: event.target.value })}
          />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? '')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'END') {
    return (
      <Form.Item label="输出变量列表">
        <Input
          value={Array.isArray(config.outputKeys) ? config.outputKeys.join(',') : ''}
          onChange={(event) => setConfig({ outputKeys: event.target.value.split(',').map((item) => item.trim()).filter(Boolean) })}
        />
      </Form.Item>
    );
  }

  return <Typography.Text type="secondary">该节点暂无额外配置。</Typography.Text>;
}

const panelStyle: React.CSSProperties = {
  background: '#fff',
  borderLeft: '0',
  padding: 16,
  width: '100%'
};

const titleStyle: React.CSSProperties = {
  marginBottom: 14
};

const hintStyle: React.CSSProperties = {
  display: 'block',
  fontSize: 12,
  marginBottom: 12
};
