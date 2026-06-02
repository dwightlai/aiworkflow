import { PlusOutlined, RobotOutlined, SearchOutlined } from '@ant-design/icons';
import type { WorkflowNode } from '@aiworkflow/workflow-schema';
import { Button, Empty, Form, Input, InputNumber, Select, Slider, Space, Typography } from 'antd';
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

interface ParamRow {
  name: string;
  value?: string;
  type: string;
}

export function NodeConfigPanel({
  node,
  onChange,
  modelProviders = [],
  promptTemplates = [],
  knowledgeBases = []
}: NodeConfigPanelProps) {
  if (!node) {
    return (
      <aside style={panelStyle}>
        <TinyHeader />
        <Empty description="请选择画布上的节点" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </aside>
    );
  }

  const setConfig = (patch: Record<string, unknown>) => onChange(node.id, { config: { ...(node.config ?? {}), ...patch } });

  if (node.type === 'LLM') {
    return (
      <aside style={panelStyle}>
        <TinyHeader />
        <NodeTitle icon={<RobotOutlined />} title="大模型" description="使用大模型处理问题" node={node} onChange={onChange} />
        <LlmConfig node={node} setConfig={setConfig} modelProviders={modelProviders} />
      </aside>
    );
  }

  if (node.type === 'KNOWLEDGE_RETRIEVAL') {
    return (
      <aside style={panelStyle}>
        <TinyHeader />
        <NodeTitle icon={<SearchOutlined />} title="知识库" description="通过知识库获取内容" node={node} onChange={onChange} />
        <KnowledgeConfig node={node} setConfig={setConfig} knowledgeBases={knowledgeBases} />
      </aside>
    );
  }

  return (
    <aside style={panelStyle}>
      <TinyHeader />
      <NodeTitle title={node.name} description={node.type} node={node} onChange={onChange} />
      <GenericConfig node={node} setConfig={setConfig} promptTemplates={promptTemplates} />
    </aside>
  );
}

function TinyHeader() {
  return <div style={tinyHeaderStyle}>TinyFlow.ai</div>;
}

function NodeTitle({
  icon,
  title,
  description,
  node,
  onChange
}: {
  icon?: React.ReactNode;
  title: string;
  description: string;
  node: WorkflowNode;
  onChange: (nodeId: string, patch: Partial<WorkflowNode>) => void;
}) {
  return (
    <div style={nodeTitleStyle}>
      <div style={nodeTitleMainStyle}>
        {icon ? <span style={nodeIconStyle}>{icon}</span> : null}
        <Input
          value={node.name || title}
          style={nodeNameInputStyle}
          onChange={(event) => onChange(node.id, { name: event.target.value })}
        />
      </div>
      <Typography.Text type="secondary">{description}</Typography.Text>
    </div>
  );
}

function LlmConfig({
  node,
  setConfig,
  modelProviders
}: {
  node: WorkflowNode;
  setConfig: (patch: Record<string, unknown>) => void;
  modelProviders: ModelProvider[];
}) {
  const config = node.config ?? {};
  const inputParams = readParams(config.inputParams);
  const outputParams = readParams(config.outputParams, [{ name: String(config.outputKey ?? 'output'), type: 'String' }]);
  const enabledModels = modelProviders.filter((provider) => provider.enabled && provider.modelUsage !== 'EMBEDDING');
  const outputName = outputParams[0]?.name || 'output';

  function updateOutputParam(patch: Partial<ParamRow>) {
    const next = [{ ...(outputParams[0] ?? { name: outputName, type: 'String' }), ...patch }];
    next[0].name = next[0].name || outputName;
    next[0].type = next[0].type || 'String';
    setConfig({
      outputParams: next,
      outputKey: next[0].name
    });
  }

  return (
    <>
      <ParamSection
        title="输入参数"
        emptyText="无输入参数"
        params={inputParams}
        onAdd={() => setConfig({ inputParams: [...inputParams, { name: 'input', value: 'question', type: 'String' }] })}
        onChange={(next) => setConfig({ inputParams: next })}
      />

      <SectionTitle title="模型设置" />
      <Form layout="vertical" size="small">
        <Form.Item label="模型">
          <Select
            aria-label="选择模型"
            placeholder="请选择模型"
            value={stringValue(config.providerId, undefined)}
            options={enabledModels.map((provider) => ({
              value: provider.id,
              label: `${provider.name}-${provider.model}`
            }))}
            onChange={(providerId) => {
              const provider = enabledModels.find((item) => item.id === providerId);
              setConfig({ providerId, model: provider?.model ?? '' });
            }}
          />
        </Form.Item>
        <SliderField label="Temperature" value={numberValue(config.temperature, 0.5)} min={0} max={2} step={0.1} onChange={(temperature) => setConfig({ temperature })} />
        <SliderField label="Top P" value={numberValue(config.topP, 0.9)} min={0} max={1} step={0.05} onChange={(topP) => setConfig({ topP })} />
        <SliderField label="Top K" value={numberValue(config.topK, 50)} min={1} max={100} step={1} onChange={(topK) => setConfig({ topK })} />
        <Form.Item label="系统提示词">
          <Input.TextArea
            placeholder="请输入系统提示词"
            autoSize={{ minRows: 5, maxRows: 10 }}
            value={String(config.systemPrompt ?? '')}
            onChange={(event) => setConfig({ systemPrompt: event.target.value })}
          />
        </Form.Item>
        <Form.Item label="用户提示词">
          <Input.TextArea
            placeholder="请输入用户提示词，如：{{question}}"
            autoSize={{ minRows: 5, maxRows: 10 }}
            value={String(config.userPrompt ?? config.promptTemplate ?? '{{question}}')}
            onChange={(event) => setConfig({ userPrompt: event.target.value })}
          />
        </Form.Item>
      </Form>

      <OutputSection
        params={outputParams}
        format={stringValue(config.outputFormat, 'TEXT') ?? 'TEXT'}
        onFormatChange={(outputFormat) => setConfig({ outputFormat })}
        onChange={(patch) => updateOutputParam(patch)}
      />
    </>
  );
}

function KnowledgeConfig({
  node,
  setConfig,
  knowledgeBases
}: {
  node: WorkflowNode;
  setConfig: (patch: Record<string, unknown>) => void;
  knowledgeBases: KnowledgeBase[];
}) {
  const config = node.config ?? {};
  const inputParams = readParams(config.inputParams, [{ name: 'search_key', value: String(config.queryKey ?? 'keyword'), type: 'String' }]);
  const outputParams = readParams(config.outputParams, [
    { name: String(config.outputKey ?? 'documents'), type: 'Array' },
    { name: 'title', type: 'String' },
    { name: 'content', type: 'String' },
    { name: 'documentId', type: 'Number' },
    { name: 'knowledgeId', type: 'Number' }
  ]);

  return (
    <>
      <ParamSection
        title="输入参数"
        params={inputParams}
        onAdd={() => setConfig({ inputParams: [...inputParams, { name: 'search_key', value: 'keyword', type: 'String' }] })}
        onChange={(next) => setConfig({ inputParams: next })}
      />

      <SectionTitle title="知识库设置" />
      <Form layout="vertical" size="small">
        <Form.Item label="知识库">
          <Select
            aria-label="选择知识库"
            placeholder="请选择知识库"
            value={stringValue(config.knowledgeBaseId, undefined)}
            options={knowledgeBases.map((base) => ({ value: base.id, label: base.name }))}
            onChange={(knowledgeBaseId) => setConfig({ knowledgeBaseId })}
          />
        </Form.Item>
        <Form.Item label="关键字">
          <Input
            placeholder="{{search_key}}"
            value={String(config.keywordTemplate ?? '{{search_key}}')}
            onChange={(event) => {
              setConfig({
                keywordTemplate: event.target.value,
                queryKey: firstTemplateVar(event.target.value) || config.queryKey || 'question'
              });
            }}
          />
        </Form.Item>
        <Form.Item label="获取数据量">
          <InputNumber
            min={1}
            max={50}
            style={{ width: '100%' }}
            value={numberValue(config.fetchCount ?? config.topK, 5)}
            onChange={(fetchCount) => setConfig({ fetchCount: fetchCount ?? 5, topK: fetchCount ?? 5 })}
          />
        </Form.Item>
      </Form>

      <OutputSection
        params={outputParams}
        format={stringValue(config.outputFormat, 'ARRAY') ?? 'ARRAY'}
        onFormatChange={(outputFormat) => setConfig({ outputFormat })}
        onChange={(patch, index) => {
          const next = outputParams.map((param, itemIndex) => itemIndex === index ? { ...param, ...patch } : param);
          setConfig({
            outputParams: next,
            outputKey: next[0]?.name || 'documents'
          });
        }}
      />
    </>
  );
}

function ParamSection({
  title,
  params,
  emptyText = '无输入参数',
  onAdd,
  onChange
}: {
  title: string;
  params: ParamRow[];
  emptyText?: string;
  onAdd: () => void;
  onChange: (params: ParamRow[]) => void;
}) {
  return (
    <section style={sectionStyle}>
      <SectionTitle title={title} action={<Button type="text" size="small" icon={<PlusOutlined />} onClick={onAdd} />} />
      {params.length === 0 ? (
        <div style={emptyParamStyle}>{emptyText}</div>
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size={8}>
          {params.map((param, index) => (
            <div key={`${param.name}-${index}`} style={paramGridStyle}>
              <Input
                aria-label={`参数名称 ${index + 1}`}
                placeholder="参数名称"
                value={param.name}
                onChange={(event) => onChange(params.map((item, itemIndex) => itemIndex === index ? { ...item, name: event.target.value } : item))}
              />
              <Input
                aria-label={`参数值 ${index + 1}`}
                placeholder="参数值"
                value={param.value}
                onChange={(event) => onChange(params.map((item, itemIndex) => itemIndex === index ? { ...item, value: event.target.value } : item))}
              />
              <Select
                aria-label={`参数类型 ${index + 1}`}
                value={param.type}
                options={paramTypeOptions}
                onChange={(type) => onChange(params.map((item, itemIndex) => itemIndex === index ? { ...item, type } : item))}
              />
            </div>
          ))}
        </Space>
      )}
    </section>
  );
}

function OutputSection({
  params,
  format,
  onFormatChange,
  onChange
}: {
  params: ParamRow[];
  format: string;
  onFormatChange: (format: string) => void;
  onChange: (patch: Partial<ParamRow>, index: number) => void;
}) {
  return (
    <section style={sectionStyle}>
      <SectionTitle
        title="输出参数"
        action={(
          <Select
            aria-label="输出格式"
            size="small"
            value={format}
            style={{ width: 110 }}
            options={[
              { value: 'TEXT', label: '文本' },
              { value: 'JSON', label: 'JSON' },
              { value: 'ARRAY', label: '数组' }
            ]}
            onChange={onFormatChange}
          />
        )}
      />
      <div style={outputHeaderStyle}>
        <Typography.Text type="secondary">参数名称</Typography.Text>
        <Typography.Text type="secondary">参数类型</Typography.Text>
      </div>
      <Space direction="vertical" style={{ width: '100%' }} size={8}>
        {params.map((param, index) => (
          <div key={`${param.name}-${index}`} style={outputGridStyle}>
            <Input aria-label={`输出参数名称 ${index + 1}`} value={param.name} onChange={(event) => onChange({ name: event.target.value }, index)} />
            <Select aria-label={`输出参数类型 ${index + 1}`} value={param.type} options={paramTypeOptions} onChange={(type) => onChange({ type }, index)} />
          </div>
        ))}
      </Space>
    </section>
  );
}

function GenericConfig({
  node,
  setConfig,
  promptTemplates
}: {
  node: WorkflowNode;
  setConfig: (patch: Record<string, unknown>) => void;
  promptTemplates: PromptTemplate[];
}) {
  const config = node.config ?? {};
  if (node.type === 'PROMPT') {
    return (
      <Form layout="vertical" size="small">
        <Form.Item label="已保存 Prompt">
          <Select
            allowClear
            aria-label="选择已保存 Prompt"
            value={stringValue(config.promptTemplateId, undefined)}
            options={promptTemplates.map((prompt) => ({ value: prompt.id, label: prompt.name }))}
            onChange={(promptTemplateId) => {
              const prompt = promptTemplates.find((item) => item.id === promptTemplateId);
              setConfig({ promptTemplateId, template: prompt?.template ?? String(config.template ?? '') });
            }}
          />
        </Form.Item>
        <Form.Item label="Prompt 模板">
          <Input.TextArea autoSize={{ minRows: 8, maxRows: 14 }} value={String(config.template ?? '')} onChange={(event) => setConfig({ template: event.target.value })} />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? 'prompt')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
      </Form>
    );
  }

  if (node.type === 'START') {
    const params = readParams(config.inputParams, [{ name: 'question', type: 'String' }]);
    return (
      <ParamSection
        title="输入参数"
        params={params}
        onAdd={() => setConfig({ inputParams: [...params, { name: 'question', type: 'String' }] })}
        onChange={(inputParams) => setConfig({ inputParams })}
      />
    );
  }

  if (node.type === 'END') {
    const params = readParams(config.outputParams, [{ name: 'output', type: 'String' }]);
    return (
      <OutputSection
        params={params}
        format={stringValue(config.outputFormat, 'JSON') ?? 'JSON'}
        onFormatChange={(outputFormat) => setConfig({ outputFormat })}
        onChange={(patch, index) => {
          const next = params.map((param, itemIndex) => itemIndex === index ? { ...param, ...patch } : param);
          setConfig({ outputParams: next, outputKeys: next.map((param) => param.name) });
        }}
      />
    );
  }

  return (
    <Form layout="vertical" size="small">
      <Form.Item label="节点名称">
        <Input value={node.name} readOnly />
      </Form.Item>
      <Form.Item label="配置 JSON">
        <Input.TextArea autoSize={{ minRows: 8, maxRows: 16 }} value={JSON.stringify(config, null, 2)} readOnly />
      </Form.Item>
      <Typography.Text type="secondary">该节点后续会继续按业务场景细化。</Typography.Text>
    </Form>
  );
}

function SectionTitle({ title, action }: { title: string; action?: React.ReactNode }) {
  return (
    <div style={sectionTitleStyle}>
      <Typography.Title level={5} style={{ margin: 0 }}>{title}</Typography.Title>
      {action}
    </div>
  );
}

function SliderField({ label, value, min, max, step, onChange }: { label: string; value: number; min: number; max: number; step: number; onChange: (value: number) => void }) {
  return (
    <Form.Item label={`${label}: ${value}`}>
      <Slider min={min} max={max} step={step} value={value} onChange={onChange} />
    </Form.Item>
  );
}

function readParams(value: unknown, fallback: ParamRow[] = []): ParamRow[] {
  if (!Array.isArray(value)) {
    return fallback;
  }
  return value
    .filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === 'object')
    .map((item) => ({
      name: String(item.name ?? ''),
      value: item.value === undefined ? undefined : String(item.value),
      type: String(item.type ?? 'String')
    }));
}

function stringValue(value: unknown, fallback: string | undefined) {
  return typeof value === 'string' && value ? value : fallback;
}

function numberValue(value: unknown, fallback: number) {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function firstTemplateVar(value: string) {
  return value.match(/\{\{\s*([A-Za-z0-9_.-]+)\s*}}/)?.[1] ?? '';
}

const paramTypeOptions = [
  { value: 'String', label: 'String' },
  { value: 'Number', label: 'Number' },
  { value: 'Boolean', label: 'Boolean' },
  { value: 'Object', label: 'Object' },
  { value: 'Array', label: 'Array' }
];

const panelStyle: React.CSSProperties = {
  background: '#fff',
  borderLeft: '0',
  height: '100%',
  overflow: 'auto',
  padding: 0,
  width: '100%'
};

const tinyHeaderStyle: React.CSSProperties = {
  background: '#f5f6f8',
  borderBottom: '1px solid #e5e7eb',
  color: '#c1c7d0',
  fontSize: 12,
  lineHeight: '34px',
  padding: '0 14px'
};

const nodeTitleStyle: React.CSSProperties = {
  padding: '14px 16px 8px'
};

const nodeTitleMainStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  gap: 10,
  marginBottom: 8
};

const nodeIconStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#eef3ff',
  borderRadius: 8,
  color: '#3b82f6',
  display: 'flex',
  fontSize: 22,
  height: 32,
  justifyContent: 'center',
  width: 32
};

const nodeNameInputStyle: React.CSSProperties = {
  border: 0,
  boxShadow: 'none',
  fontSize: 16,
  fontWeight: 700,
  paddingLeft: 0
};

const sectionStyle: React.CSSProperties = {
  padding: '10px 16px'
};

const sectionTitleStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  justifyContent: 'space-between',
  marginBottom: 10
};

const emptyParamStyle: React.CSSProperties = {
  background: '#f7f7f8',
  borderRadius: 6,
  color: '#8c8c8c',
  lineHeight: '44px',
  textAlign: 'center'
};

const paramGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 6,
  gridTemplateColumns: 'minmax(88px, 1fr) minmax(104px, 1.2fr) 96px'
};

const outputHeaderStyle: React.CSSProperties = {
  display: 'grid',
  gap: 6,
  gridTemplateColumns: 'minmax(110px, 1fr) minmax(110px, 1fr)',
  marginBottom: 6
};

const outputGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 6,
  gridTemplateColumns: 'minmax(110px, 1fr) minmax(110px, 1fr)'
};
