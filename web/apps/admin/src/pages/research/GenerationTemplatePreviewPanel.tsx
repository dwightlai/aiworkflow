import { ApartmentOutlined, FileTextOutlined, FormOutlined, NodeIndexOutlined } from '@ant-design/icons';
import { Card, Space, Steps, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type {
  GenerationTemplateSchema,
  GenerationTemplateSection,
  GenerationTemplateVariable,
  WorkflowSnapshotNode
} from '../../api/generationTemplates';
import { buildTemplateSkeleton } from '../../api/generationTemplates';

interface GenerationTemplatePreviewPanelProps {
  schema: GenerationTemplateSchema;
  outputType?: string;
  templateName?: string;
  workflowNodes?: WorkflowSnapshotNode[];
  workflowLoading?: boolean;
  workflowName?: string | null;
}

const sourceLabel: Record<string, string> = {
  INTERNAL_KNOWLEDGE_BASE: '知识库',
  EXTERNAL_CORPUS: '外部语料',
  ARCHIVE_CORPUS: '档案馆'
};

export function GenerationTemplatePreviewPanel({
  schema,
  outputType,
  templateName,
  workflowNodes = [],
  workflowLoading = false,
  workflowName
}: GenerationTemplatePreviewPanelProps) {
  const sections = schema.sections ?? [];
  const variables = schema.variables ?? [];
  const skeleton = buildTemplateSkeleton(schema, templateName || schema.title);

  const sectionColumns: ColumnsType<GenerationTemplateSection> = [
    { title: '键', dataIndex: 'key', width: 100 },
    { title: '标题', dataIndex: 'title', width: 140 },
    { title: '生成说明', dataIndex: 'instruction', ellipsis: true },
    {
      title: '资料来源',
      width: 130,
      render: (_, row) => (
        <Space size={4} wrap>
          {(row.requiredSources ?? []).length > 0
            ? (row.requiredSources ?? []).map((source) => (
              <Tag key={source}>{sourceLabel[source] ?? source}</Tag>
            ))
            : <Typography.Text type="secondary">-</Typography.Text>}
        </Space>
      )
    },
    {
      title: '引用',
      width: 64,
      render: (_, row) => (
        row.citationRequired ? <Tag color="blue">必须</Tag> : <Tag>可选</Tag>
      )
    },
    {
      title: '格式',
      dataIndex: 'outputFormat',
      width: 80,
      render: (value: string | undefined) => value ? <Tag>{value}</Tag> : <Typography.Text type="secondary">-</Typography.Text>
    }
  ];

  const variableColumns: ColumnsType<GenerationTemplateVariable> = [
    { title: '字段', dataIndex: 'name', width: 120 },
    { title: '标签', dataIndex: 'label', width: 120 },
    { title: '类型', dataIndex: 'type', width: 80 },
    {
      title: '必填',
      width: 64,
      render: (_, row) => row.required ? <Tag color="red">是</Tag> : <Tag>否</Tag>
    }
  ];

  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <Space wrap>
        <Tag icon={<FileTextOutlined />} color="blue">{sections.length} 章</Tag>
        <Tag icon={<FormOutlined />} color="purple">{variables.length} 变量</Tag>
        {outputType ? <Tag color="geekblue">输出 {outputType}</Tag> : null}
        {workflowName ? <Tag icon={<NodeIndexOutlined />} color="cyan">{workflowName}</Tag> : null}
      </Space>

      <Card size="small" title="变量预览">
        {variables.length > 0 ? (
          <Table size="small" rowKey="name" pagination={false} columns={variableColumns} dataSource={variables} />
        ) : (
          <Typography.Text type="secondary">未定义变量。</Typography.Text>
        )}
      </Card>

      <Card size="small" title={<Space><ApartmentOutlined />章节结构</Space>}>
        {sections.length > 0 ? (
          <Table size="small" rowKey="key" pagination={false} columns={sectionColumns} dataSource={sections} />
        ) : (
          <Typography.Text type="secondary">未定义章节。</Typography.Text>
        )}
      </Card>

      <Card size="small" title="工作流编排" loading={workflowLoading}>
        {workflowNodes.length > 0 ? (
          <Steps
            size="small"
            direction="vertical"
            current={workflowNodes.length}
            items={workflowNodes.map((node) => ({
              title: node.name,
              description: node.type ? `${node.type}${node.order ? ` · 步骤 ${node.order}` : ''}` : undefined
            }))}
          />
        ) : (
          <Typography.Text type="secondary">未绑定工作流或未加载节点。</Typography.Text>
        )}
      </Card>

      <Card size="small" title="成果骨架预览">
        <Typography.Paragraph
          copyable
          style={{
            background: '#fafafa',
            border: '1px solid #f0f0f0',
            borderRadius: 6,
            fontFamily: 'Consolas, monospace',
            fontSize: 12,
            marginBottom: 0,
            padding: 12,
            whiteSpace: 'pre-wrap'
          }}
        >
          {skeleton}
        </Typography.Paragraph>
      </Card>
    </Space>
  );
}
