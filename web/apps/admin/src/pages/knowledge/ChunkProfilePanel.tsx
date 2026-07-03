import { CheckOutlined, ExperimentOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Input, InputNumber, Select, Space, Statistic, Tag, Typography, message } from 'antd';
import { useState } from 'react';
import {
  activateChunkProfile,
  compareChunkProfiles,
  createChunkProfile,
  listChunkProfiles
} from '../../api/knowledge';

export function ChunkProfilePanel({
  knowledgeBaseId,
  documentId
}: {
  knowledgeBaseId: string;
  documentId: string;
}) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [strategy, setStrategy] = useState('STRUCTURE_AWARE');
  const [chunkSize, setChunkSize] = useState(500);
  const [chunkOverlap, setChunkOverlap] = useState(50);
  const [semanticSimilarityThreshold, setSemanticSimilarityThreshold] = useState(0.78);
  const [leftId, setLeftId] = useState<string>();
  const [rightId, setRightId] = useState<string>();

  const profilesQuery = useQuery({
    queryKey: ['chunk-profiles', knowledgeBaseId],
    queryFn: () => listChunkProfiles(knowledgeBaseId)
  });
  const profiles = profilesQuery.data ?? [];
  const createMutation = useMutation({
    mutationFn: () => createChunkProfile(knowledgeBaseId, {
      name,
      strategy,
      chunkSize,
      chunkOverlap,
      configJson: strategy === 'SEMANTIC'
        ? JSON.stringify({ semanticSimilarityThreshold })
        : '{}'
    }),
    onSuccess: async () => {
      message.success('Profile 版本已保存');
      setName('');
      await queryClient.invalidateQueries({ queryKey: ['chunk-profiles', knowledgeBaseId] });
    }
  });
  const activateMutation = useMutation({
    mutationFn: (profileId: string) => activateChunkProfile(knowledgeBaseId, profileId),
    onSuccess: async () => {
      message.success('Profile 已激活，可按该版本重新解析文档');
      await queryClient.invalidateQueries({ queryKey: ['chunk-profiles', knowledgeBaseId] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    }
  });
  const comparisonQuery = useQuery({
    queryKey: ['chunk-profile-comparison', knowledgeBaseId, documentId, leftId, rightId],
    queryFn: () => compareChunkProfiles(knowledgeBaseId, documentId, leftId!, rightId!),
    enabled: Boolean(leftId && rightId)
  });
  const options = profiles.map((profile) => ({
    label: `V${profile.version} ${profile.name}`,
    value: profile.id
  }));

  return (
    <Card
      title={<Space><ExperimentOutlined />分块 Profile</Space>}
      style={{ marginBottom: 16 }}
      extra={profiles.find((profile) => profile.status === 'ACTIVE')
        ? <Tag color="green">当前 V{profiles.find((profile) => profile.status === 'ACTIVE')?.version}</Tag>
        : <Tag>尚未激活</Tag>}
    >
      <Space wrap style={{ marginBottom: 16 }}>
        <Input placeholder="版本名称" value={name} onChange={(event) => setName(event.target.value)} />
        <Select
          value={strategy}
          onChange={setStrategy}
          style={{ width: 190 }}
          options={[
            { label: '结构感知', value: 'STRUCTURE_AWARE' },
            { label: '句子边界', value: 'SENTENCE_BOUNDARY' },
            { label: '语义分块', value: 'SEMANTIC' },
            { label: '固定长度', value: 'FIXED_LENGTH' }
          ]}
        />
        <InputNumber min={50} max={5000} value={chunkSize} onChange={(value) => setChunkSize(value ?? 500)} />
        <InputNumber min={0} max={1000} value={chunkOverlap} onChange={(value) => setChunkOverlap(value ?? 0)} />
        {strategy === 'SEMANTIC' ? (
          <InputNumber
            aria-label="语义相似度阈值"
            min={0}
            max={1}
            step={0.01}
            precision={2}
            value={semanticSimilarityThreshold}
            onChange={(value) => setSemanticSimilarityThreshold(value ?? 0.78)}
          />
        ) : null}
        <Button
          icon={<PlusOutlined />}
          loading={createMutation.isPending}
          onClick={() => createMutation.mutate()}
        >
          保存版本
        </Button>
      </Space>

      <Space wrap style={{ marginBottom: 16 }}>
        <Select placeholder="对比版本 A" options={options} value={leftId} onChange={setLeftId} style={{ width: 220 }} />
        <Select placeholder="对比版本 B" options={options} value={rightId} onChange={setRightId} style={{ width: 220 }} />
        <Button
          icon={<CheckOutlined />}
          disabled={!rightId}
          loading={activateMutation.isPending}
          onClick={() => rightId && activateMutation.mutate(rightId)}
        >
          激活版本 B
        </Button>
      </Space>

      {comparisonQuery.data ? (
        <Space size={32} wrap>
          <Statistic title={`V${comparisonQuery.data.left.profile.version} 分块数`} value={comparisonQuery.data.left.quality.chunkCount} />
          <Statistic title="平均 Tokens" value={comparisonQuery.data.left.quality.averageTokens} precision={1} />
          <Statistic title={`V${comparisonQuery.data.right.profile.version} 分块数`} value={comparisonQuery.data.right.quality.chunkCount} />
          <Statistic title="平均 Tokens" value={comparisonQuery.data.right.quality.averageTokens} precision={1} />
          <Typography.Text type="secondary">
            原子块比例：{(comparisonQuery.data.left.quality.atomicChunkRatio * 100).toFixed(1)}% /
            {(comparisonQuery.data.right.quality.atomicChunkRatio * 100).toFixed(1)}%
          </Typography.Text>
        </Space>
      ) : null}
    </Card>
  );
}
