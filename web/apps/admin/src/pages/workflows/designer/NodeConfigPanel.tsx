import { CommentOutlined, DeleteOutlined, PlusOutlined, RedoOutlined, RobotOutlined, SearchOutlined, ToolOutlined } from '@ant-design/icons';
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

interface KeyValueRow {
  key: string;
  value: string;
}

interface LoopStep {
  type: string;
  name?: string;
  template?: string;
  outputKey?: string;
  method?: string;
  url?: string;
  bodyType?: string;
  bodyTemplate?: string;
  responseBodyType?: string;
  headers?: KeyValueRow[];
  params?: KeyValueRow[];
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
      <Panel node={node} title="大模型" description="使用大模型处理问题" icon={<RobotOutlined />} onChange={onChange}>
        <LlmConfig node={node} setConfig={setConfig} modelProviders={modelProviders} />
      </Panel>
    );
  }

  if (node.type === 'KNOWLEDGE_RETRIEVAL') {
    return (
      <Panel node={node} title="知识库" description="通过知识库获取内容" icon={<SearchOutlined />} onChange={onChange}>
        <KnowledgeConfig node={node} setConfig={setConfig} knowledgeBases={knowledgeBases} />
      </Panel>
    );
  }

  if (node.type === 'CONTENT_TEMPLATE') {
    return (
      <Panel node={node} title="内容模板" description="通过模板生成文本或 JSON 内容" icon={<CommentOutlined />} onChange={onChange}>
        <ContentTemplateConfig node={node} setConfig={setConfig} />
      </Panel>
    );
  }

  if (node.type === 'HTTP_TOOL') {
    return (
      <Panel node={node} title="HTTP 请求" description="调用外部 API 并输出响应结构" icon={<ToolOutlined />} onChange={onChange}>
        <HttpConfig node={node} setConfig={setConfig} />
      </Panel>
    );
  }

  if (node.type === 'LOOP') {
    return (
      <Panel node={node} title="循环" description="遍历数组并执行循环体步骤" icon={<RedoOutlined />} onChange={onChange}>
        <LoopConfig node={node} setConfig={setConfig} />
      </Panel>
    );
  }

  return (
    <Panel node={node} title={node.name} description={node.type} onChange={onChange}>
      <GenericConfig node={node} setConfig={setConfig} promptTemplates={promptTemplates} />
    </Panel>
  );
}

function Panel({
  children,
  icon,
  title,
  description,
  node,
  onChange
}: {
  children: React.ReactNode;
  icon?: React.ReactNode;
  title: string;
  description: string;
  node: WorkflowNode;
  onChange: (nodeId: string, patch: Partial<WorkflowNode>) => void;
}) {
  return (
    <aside style={panelStyle}>
      <TinyHeader />
      <NodeTitle icon={icon} title={title} description={description} node={node} onChange={onChange} />
      {children}
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
        <Input value={node.name || title} style={nodeNameInputStyle} onChange={(event) => onChange(node.id, { name: event.target.value })} />
      </div>
      <Typography.Text type="secondary">{description}</Typography.Text>
    </div>
  );
}

function LlmConfig({ node, setConfig, modelProviders }: { node: WorkflowNode; setConfig: (patch: Record<string, unknown>) => void; modelProviders: ModelProvider[] }) {
  const config = node.config ?? {};
  const inputParams = readParams(config.inputParams);
  const outputParams = readParams(config.outputParams, [{ name: String(config.outputKey ?? 'output'), type: 'String' }]);
  const enabledModels = modelProviders.filter((provider) => provider.enabled && provider.modelUsage !== 'EMBEDDING');
  const outputName = outputParams[0]?.name || 'output';

  function updateOutputParam(patch: Partial<ParamRow>) {
    const next = [{ ...(outputParams[0] ?? { name: outputName, type: 'String' }), ...patch }];
    next[0].name = next[0].name || outputName;
    next[0].type = next[0].type || 'String';
    setConfig({ outputParams: next, outputKey: next[0].name });
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
            options={enabledModels.map((provider) => ({ value: provider.id, label: `${provider.name}-${provider.model}` }))}
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
          <Input.TextArea placeholder="请输入系统提示词" autoSize={{ minRows: 5, maxRows: 10 }} value={String(config.systemPrompt ?? '')} onChange={(event) => setConfig({ systemPrompt: event.target.value })} />
        </Form.Item>
        <Form.Item label="用户提示词">
          <Input.TextArea placeholder="请输入用户提示词，如：{{question}}" autoSize={{ minRows: 5, maxRows: 10 }} value={String(config.userPrompt ?? config.promptTemplate ?? '{{question}}')} onChange={(event) => setConfig({ userPrompt: event.target.value })} />
        </Form.Item>
      </Form>
      <OutputSection
        params={outputParams}
        format={stringValue(config.outputFormat, 'TEXT') ?? 'TEXT'}
        onFormatChange={(outputFormat) => setConfig({ outputFormat })}
        onChange={(patch) => updateOutputParam(patch)}
        onRemove={() => setConfig({ outputParams: [], outputKey: '' })}
      />
    </>
  );
}

function KnowledgeConfig({ node, setConfig, knowledgeBases }: { node: WorkflowNode; setConfig: (patch: Record<string, unknown>) => void; knowledgeBases: KnowledgeBase[] }) {
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
      <ParamSection title="输入参数" params={inputParams} onAdd={() => setConfig({ inputParams: [...inputParams, { name: 'search_key', value: 'keyword', type: 'String' }] })} onChange={(next) => setConfig({ inputParams: next })} />
      <SectionTitle title="知识库设置" />
      <Form layout="vertical" size="small">
        <Form.Item label="知识库">
          <Select aria-label="选择知识库" placeholder="请选择知识库" value={stringValue(config.knowledgeBaseId, undefined)} options={knowledgeBases.map((base) => ({ value: base.id, label: base.name }))} onChange={(knowledgeBaseId) => setConfig({ knowledgeBaseId })} />
        </Form.Item>
        <Form.Item label="关键字">
          <Input
            placeholder="{{search_key}}"
            value={String(config.keywordTemplate ?? '{{search_key}}')}
            onChange={(event) => setConfig({ keywordTemplate: event.target.value, queryKey: firstTemplateVar(event.target.value) || config.queryKey || 'question' })}
          />
        </Form.Item>
        <Form.Item label="获取数据量">
          <InputNumber min={1} max={50} style={{ width: '100%' }} value={numberValue(config.fetchCount ?? config.topK, 5)} onChange={(fetchCount) => setConfig({ fetchCount: fetchCount ?? 5, topK: fetchCount ?? 5 })} />
        </Form.Item>
      </Form>
      <OutputSection
        params={outputParams}
        format={stringValue(config.outputFormat, 'ARRAY') ?? 'ARRAY'}
        onFormatChange={(outputFormat) => setConfig({ outputFormat })}
        onChange={(patch, index) => {
          const next = outputParams.map((param, itemIndex) => itemIndex === index ? { ...param, ...patch } : param);
          setConfig({ outputParams: next, outputKey: next[0]?.name || 'documents' });
        }}
        onRemove={(index) => {
          const next = outputParams.filter((_, itemIndex) => itemIndex !== index);
          setConfig({ outputParams: next, outputKey: next[0]?.name || 'documents' });
        }}
      />
    </>
  );
}

function ContentTemplateConfig({ node, setConfig }: { node: WorkflowNode; setConfig: (patch: Record<string, unknown>) => void }) {
  const config = node.config ?? {};
  const outputParams = readParams(config.outputParams, [{ name: String(config.outputKey ?? 'content'), type: stringValue(config.outputFormat, 'TEXT') === 'JSON' ? 'Object' : 'String' }]);
  return (
    <>
      <SectionTitle title="模板设置" />
      <Form layout="vertical" size="small">
        <Form.Item label="输出格式">
          <Select value={stringValue(config.outputFormat, 'TEXT')} options={outputFormatOptions} onChange={(outputFormat) => setConfig({ outputFormat })} />
        </Form.Item>
        <Form.Item label="模板内容">
          <Input.TextArea placeholder="使用 {{question}}、{{documents}} 等上下文变量" autoSize={{ minRows: 10, maxRows: 18 }} value={String(config.template ?? '')} onChange={(event) => setConfig({ template: event.target.value })} />
        </Form.Item>
      </Form>
      <OutputSection
        params={outputParams}
        format={stringValue(config.outputFormat, 'TEXT') ?? 'TEXT'}
        onFormatChange={(outputFormat) => setConfig({ outputFormat })}
        onChange={(patch, index) => {
          const next = outputParams.map((param, itemIndex) => itemIndex === index ? { ...param, ...patch } : param);
          setConfig({ outputParams: next, outputKey: next[0]?.name || 'content' });
        }}
        onRemove={(index) => {
          const next = outputParams.filter((_, itemIndex) => itemIndex !== index);
          setConfig({ outputParams: next, outputKey: next[0]?.name || 'content' });
        }}
      />
    </>
  );
}

function HttpConfig({ node, setConfig }: { node: WorkflowNode; setConfig: (patch: Record<string, unknown>) => void }) {
  const config = node.config ?? {};
  const params = readKeyValues(config.params);
  const headers = readKeyValues(config.headers, [{ key: 'Content-Type', value: 'application/json' }]);
  const formData = readKeyValues(config.formData);
  const outputParams = readParams(config.outputParams, [
    { name: 'headers', type: 'Object' },
    { name: 'statusCode', type: 'Number' },
    { name: 'body', type: 'String' }
  ]);

  return (
    <>
      <SectionTitle title="请求设置" />
      <Form layout="vertical" size="small">
        <Form.Item label="请求方法">
          <Select value={stringValue(config.method, 'POST')} options={methodOptions} onChange={(method) => setConfig({ method })} />
        </Form.Item>
        <Form.Item label="请求地址">
          <Input placeholder="https://api.example.com/orders/{{orderId}}" value={String(config.url ?? '')} onChange={(event) => setConfig({ url: event.target.value })} />
        </Form.Item>
        <Form.Item label="超时时间(ms)">
          <InputNumber min={1000} max={120000} style={{ width: '100%' }} value={numberValue(config.timeoutMs, 30000)} onChange={(timeoutMs) => setConfig({ timeoutMs: timeoutMs ?? 30000 })} />
        </Form.Item>
      </Form>
      <KeyValueSection title="Params" rows={params} onAdd={() => setConfig({ params: [...params, { key: '', value: '' }] })} onChange={(next) => setConfig({ params: next })} />
      <KeyValueSection title="Headers" rows={headers} onAdd={() => setConfig({ headers: [...headers, { key: '', value: '' }] })} onChange={(next) => setConfig({ headers: next })} />
      <SectionTitle title="Body 设置" />
      <Form layout="vertical" size="small">
        <Form.Item label="Body 类型">
          <Select value={stringValue(config.bodyType, 'JSON')} options={bodyTypeOptions} onChange={(bodyType) => setConfig({ bodyType })} />
        </Form.Item>
        {stringValue(config.bodyType, 'JSON') === 'FORM_DATA' ? (
          <KeyValueSection title="Form Data" rows={formData} onAdd={() => setConfig({ formData: [...formData, { key: '', value: '' }] })} onChange={(next) => setConfig({ formData: next })} />
        ) : (
          <Form.Item label="Body 内容">
            <Input.TextArea placeholder='{"question":"{{question}}"}' autoSize={{ minRows: 8, maxRows: 14 }} value={String(config.bodyTemplate ?? config.body ?? '')} onChange={(event) => setConfig({ bodyTemplate: event.target.value })} />
          </Form.Item>
        )}
        <Form.Item label="响应 Body 类型">
          <Select value={stringValue(config.responseBodyType, 'TEXT')} options={[{ value: 'TEXT', label: '文本' }, { value: 'JSON', label: 'JSON' }]} onChange={(responseBodyType) => setConfig({ responseBodyType })} />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? 'httpResult')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
      </Form>
      <OutputSection
        params={outputParams}
        format="OBJECT"
        onFormatChange={() => undefined}
        onChange={(patch, index) => setConfig({ outputParams: outputParams.map((param, itemIndex) => itemIndex === index ? { ...param, ...patch } : param) })}
        onRemove={(index) => setConfig({ outputParams: outputParams.filter((_, itemIndex) => itemIndex !== index) })}
      />
    </>
  );
}

function LoopConfig({ node, setConfig }: { node: WorkflowNode; setConfig: (patch: Record<string, unknown>) => void }) {
  const config = node.config ?? {};
  const steps = readLoopSteps(config.loopSteps);
  const outputParams = readParams(config.outputParams, [{ name: String(config.outputKey ?? 'loopResults'), type: 'Array' }]);
  return (
    <>
      <SectionTitle title="循环设置" />
      <Form layout="vertical" size="small">
        <Form.Item label="循环变量">
          <Input placeholder="items" value={String(config.loopVar ?? 'items')} onChange={(event) => setConfig({ loopVar: event.target.value })} />
        </Form.Item>
        <Form.Item label="循环项变量">
          <Input placeholder="loopItem" value={String(config.itemVar ?? 'loopItem')} onChange={(event) => setConfig({ itemVar: event.target.value })} />
        </Form.Item>
        <Form.Item label="索引变量">
          <Input placeholder="index" value={String(config.indexVar ?? 'index')} onChange={(event) => setConfig({ indexVar: event.target.value })} />
        </Form.Item>
        <Form.Item label="最大循环次数">
          <InputNumber min={1} max={1000} style={{ width: '100%' }} value={numberValue(config.maxIterations, 100)} onChange={(maxIterations) => setConfig({ maxIterations: maxIterations ?? 100 })} />
        </Form.Item>
      </Form>
      <section style={sectionStyle}>
        <SectionTitle title="循环体步骤" action={<Button type="text" size="small" icon={<PlusOutlined />} onClick={() => setConfig({ loopSteps: [...steps, defaultLoopStep()] })} />} />
        <Space direction="vertical" style={{ width: '100%' }} size={10}>
          {steps.map((step, index) => (
            <div key={`${step.name}-${index}`} style={stepCardStyle}>
              <Input placeholder="步骤名称" value={step.name} onChange={(event) => setConfig({ loopSteps: updateLoopStep(steps, index, { name: event.target.value }) })} />
              <Select value={step.type} options={[{ value: 'CONTENT_TEMPLATE', label: '内容模板' }, { value: 'HTTP_TOOL', label: 'HTTP 请求' }]} onChange={(type) => setConfig({ loopSteps: updateLoopStep(steps, index, { type }) })} />
              {step.type === 'HTTP_TOOL' ? (
                <>
                  <Select value={step.method ?? 'POST'} options={methodOptions} onChange={(method) => setConfig({ loopSteps: updateLoopStep(steps, index, { method }) })} />
                  <Input placeholder="请求地址" value={step.url} onChange={(event) => setConfig({ loopSteps: updateLoopStep(steps, index, { url: event.target.value }) })} />
                  <Input.TextArea placeholder='{"item":"{{loopItem}}"}' autoSize={{ minRows: 4, maxRows: 8 }} value={step.bodyTemplate} onChange={(event) => setConfig({ loopSteps: updateLoopStep(steps, index, { bodyTemplate: event.target.value }) })} />
                </>
              ) : (
                <Input.TextArea placeholder="第 {{index}} 项：{{loopItem}}" autoSize={{ minRows: 4, maxRows: 8 }} value={step.template} onChange={(event) => setConfig({ loopSteps: updateLoopStep(steps, index, { template: event.target.value }) })} />
              )}
              <Input placeholder="输出变量" value={step.outputKey} onChange={(event) => setConfig({ loopSteps: updateLoopStep(steps, index, { outputKey: event.target.value }) })} />
            </div>
          ))}
        </Space>
      </section>
      <OutputSection
        params={outputParams}
        format="ARRAY"
        onFormatChange={() => undefined}
        onChange={(patch, index) => {
          const next = outputParams.map((param, itemIndex) => itemIndex === index ? { ...param, ...patch } : param);
          setConfig({ outputParams: next, outputKey: next[0]?.name || 'loopResults' });
        }}
        onRemove={(index) => {
          const next = outputParams.filter((_, itemIndex) => itemIndex !== index);
          setConfig({ outputParams: next, outputKey: next[0]?.name || 'loopResults' });
        }}
      />
    </>
  );
}

function ParamSection({ title, params, emptyText = '无输入参数', onAdd, onChange }: { title: string; params: ParamRow[]; emptyText?: string; onAdd: () => void; onChange: (params: ParamRow[]) => void }) {
  return (
    <section style={sectionStyle}>
      <SectionTitle title={title} action={<Button type="text" size="small" icon={<PlusOutlined />} onClick={onAdd} />} />
      {params.length === 0 ? (
        <div style={emptyParamStyle}>{emptyText}</div>
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size={8}>
          {params.map((param, index) => (
            <div key={`${param.name}-${index}`} style={paramGridStyle}>
              <Input aria-label={`参数名称 ${index + 1}`} placeholder="参数名称" value={param.name} onChange={(event) => onChange(params.map((item, itemIndex) => itemIndex === index ? { ...item, name: event.target.value } : item))} />
              <Input aria-label={`参数值 ${index + 1}`} placeholder="参数值" value={param.value} onChange={(event) => onChange(params.map((item, itemIndex) => itemIndex === index ? { ...item, value: event.target.value } : item))} />
              <Select aria-label={`参数类型 ${index + 1}`} value={param.type} options={paramTypeOptions} onChange={(type) => onChange(params.map((item, itemIndex) => itemIndex === index ? { ...item, type } : item))} />
              <Button
                aria-label={`删除输入参数 ${index + 1}`}
                danger
                icon={<DeleteOutlined />}
                size="small"
                type="text"
                onClick={() => onChange(params.filter((_, itemIndex) => itemIndex !== index))}
              />
            </div>
          ))}
        </Space>
      )}
    </section>
  );
}

function KeyValueSection({ title, rows, onAdd, onChange }: { title: string; rows: KeyValueRow[]; onAdd: () => void; onChange: (rows: KeyValueRow[]) => void }) {
  return (
    <section style={sectionStyle}>
      <SectionTitle title={title} action={<Button type="text" size="small" icon={<PlusOutlined />} onClick={onAdd} />} />
      <Space direction="vertical" style={{ width: '100%' }} size={8}>
        {rows.map((row, index) => (
          <div key={`${row.key}-${index}`} style={keyValueGridStyle}>
            <Input placeholder="Key" value={row.key} onChange={(event) => onChange(rows.map((item, itemIndex) => itemIndex === index ? { ...item, key: event.target.value } : item))} />
            <Input placeholder="Value" value={row.value} onChange={(event) => onChange(rows.map((item, itemIndex) => itemIndex === index ? { ...item, value: event.target.value } : item))} />
          </div>
        ))}
      </Space>
    </section>
  );
}

function OutputSection({ params, format, onFormatChange, onChange, onRemove }: { params: ParamRow[]; format: string; onFormatChange: (format: string) => void; onChange: (patch: Partial<ParamRow>, index: number) => void; onRemove?: (index: number) => void }) {
  return (
    <section style={sectionStyle}>
      <SectionTitle
        title="输出参数"
        action={<Select aria-label="输出格式" size="small" value={format} style={{ width: 110 }} options={outputFormatOptions} onChange={onFormatChange} />}
      />
      <div style={outputHeaderStyle}>
        <Typography.Text type="secondary">参数名称</Typography.Text>
        <Typography.Text type="secondary">参数类型</Typography.Text>
        <span />
      </div>
      <Space direction="vertical" style={{ width: '100%' }} size={8}>
        {params.map((param, index) => (
          <div key={`${param.name}-${index}`} style={outputGridStyle}>
            <Input aria-label={`输出参数名称 ${index + 1}`} value={param.name} onChange={(event) => onChange({ name: event.target.value }, index)} />
            <Select aria-label={`输出参数类型 ${index + 1}`} value={param.type} options={paramTypeOptions} onChange={(type) => onChange({ type }, index)} />
            <Button
              aria-label={`删除输出参数 ${index + 1}`}
              danger
              icon={<DeleteOutlined />}
              size="small"
              type="text"
              onClick={() => onRemove?.(index)}
            />
          </div>
        ))}
      </Space>
    </section>
  );
}

function GenericConfig({ node, setConfig, promptTemplates }: { node: WorkflowNode; setConfig: (patch: Record<string, unknown>) => void; promptTemplates: PromptTemplate[] }) {
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
    return <ParamSection title="输入参数" params={params} onAdd={() => setConfig({ inputParams: [...params, { name: 'question', type: 'String' }] })} onChange={(inputParams) => setConfig({ inputParams })} />;
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
        onRemove={(index) => {
          const next = params.filter((_, itemIndex) => itemIndex !== index);
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

function readKeyValues(value: unknown, fallback: KeyValueRow[] = []): KeyValueRow[] {
  if (!Array.isArray(value)) {
    return fallback;
  }
  return value
    .filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === 'object')
    .map((item) => ({ key: String(item.key ?? ''), value: String(item.value ?? '') }));
}

function readLoopSteps(value: unknown): LoopStep[] {
  if (!Array.isArray(value)) {
    return [defaultLoopStep()];
  }
  return value
    .filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === 'object')
    .map((item) => ({
      type: String(item.type ?? 'CONTENT_TEMPLATE'),
      name: String(item.name ?? ''),
      template: String(item.template ?? ''),
      outputKey: String(item.outputKey ?? 'text'),
      method: String(item.method ?? 'POST'),
      url: String(item.url ?? ''),
      bodyType: String(item.bodyType ?? 'JSON'),
      bodyTemplate: String(item.bodyTemplate ?? ''),
      responseBodyType: String(item.responseBodyType ?? 'TEXT')
    }));
}

function updateLoopStep(steps: LoopStep[], index: number, patch: Partial<LoopStep>) {
  return steps.map((step, itemIndex) => itemIndex === index ? { ...step, ...patch } : step);
}

function defaultLoopStep(): LoopStep {
  return { type: 'CONTENT_TEMPLATE', name: '模板处理', template: '第 {{index}} 项：{{loopItem}}', outputKey: 'text' };
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

const outputFormatOptions = [
  { value: 'TEXT', label: '文本' },
  { value: 'JSON', label: 'JSON' },
  { value: 'ARRAY', label: '数组' },
  { value: 'OBJECT', label: '对象' }
];

const methodOptions = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].map((method) => ({ value: method, label: method }));
const bodyTypeOptions = [
  { value: 'JSON', label: 'JSON' },
  { value: 'RAW', label: 'Raw' },
  { value: 'FORM_DATA', label: 'Form Data' },
  { value: 'NONE', label: 'None' }
];

const panelStyle: React.CSSProperties = { background: '#fff', borderLeft: '0', height: '100%', overflow: 'auto', padding: 0, width: '100%' };
const tinyHeaderStyle: React.CSSProperties = { background: '#f5f6f8', borderBottom: '1px solid #e5e7eb', color: '#c1c7d0', fontSize: 12, lineHeight: '34px', padding: '0 14px' };
const nodeTitleStyle: React.CSSProperties = { padding: '14px 16px 8px' };
const nodeTitleMainStyle: React.CSSProperties = { alignItems: 'center', display: 'flex', gap: 10, marginBottom: 8 };
const nodeIconStyle: React.CSSProperties = { alignItems: 'center', background: '#eef3ff', borderRadius: 8, color: '#3b82f6', display: 'flex', fontSize: 22, height: 32, justifyContent: 'center', width: 32 };
const nodeNameInputStyle: React.CSSProperties = { border: 0, boxShadow: 'none', fontSize: 16, fontWeight: 700, paddingLeft: 0 };
const sectionStyle: React.CSSProperties = { padding: '10px 16px' };
const sectionTitleStyle: React.CSSProperties = { alignItems: 'center', display: 'flex', justifyContent: 'space-between', marginBottom: 10 };
const emptyParamStyle: React.CSSProperties = { background: '#f7f7f8', borderRadius: 6, color: '#8c8c8c', lineHeight: '44px', textAlign: 'center' };
const paramGridStyle: React.CSSProperties = { alignItems: 'center', display: 'grid', gap: 6, gridTemplateColumns: 'minmax(78px, 1fr) minmax(96px, 1.15fr) 90px 30px' };
const keyValueGridStyle: React.CSSProperties = { display: 'grid', gap: 6, gridTemplateColumns: 'minmax(100px, 1fr) minmax(140px, 1.4fr)' };
const outputHeaderStyle: React.CSSProperties = { display: 'grid', gap: 6, gridTemplateColumns: 'minmax(100px, 1fr) minmax(100px, 1fr) 30px', marginBottom: 6 };
const outputGridStyle: React.CSSProperties = { alignItems: 'center', display: 'grid', gap: 6, gridTemplateColumns: 'minmax(100px, 1fr) minmax(100px, 1fr) 30px' };
const stepCardStyle: React.CSSProperties = { border: '1px solid #e5e7eb', borderRadius: 8, display: 'grid', gap: 8, padding: 10 };
