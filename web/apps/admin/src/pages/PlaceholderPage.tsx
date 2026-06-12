import {
  ApiOutlined,
  AppstoreAddOutlined,
  CheckCircleOutlined,
  DeploymentUnitOutlined,
  FileTextOutlined,
  RocketOutlined
} from '@ant-design/icons';
import { Button, Progress, Space, Tag, Typography } from 'antd';
import type React from 'react';

export interface PlaceholderPageProps {
  title: string;
}

export function PlaceholderPage({ title }: PlaceholderPageProps) {
  const profile = moduleProfiles[title] ?? defaultProfile(title);

  return (
    <div style={pageStyle}>
      <section style={heroStyle}>
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>AI Workflow / 模块工作区</Typography.Text>
          <Typography.Title level={3} style={{ margin: '4px 0 6px' }}>{title}</Typography.Title>
          <Typography.Paragraph style={heroCopyStyle}>{profile.description}</Typography.Paragraph>
        </div>
        <Space wrap>
          <Tag color="processing">集成状态：规划中</Tag>
          <Button type="primary" icon={<DeploymentUnitOutlined />} onClick={() => navigateTo('/workflows')}>
            进入工作流运营台
          </Button>
        </Space>
      </section>

      <section style={gridStyle}>
        <div style={panelStyle}>
          <Space align="center" style={titleRowStyle}>
            <Typography.Title level={5} style={{ margin: 0 }}>能力规划</Typography.Title>
            <Tag>{profile.phase}</Tag>
          </Space>
          <div style={capabilityListStyle}>
            {profile.capabilities.map((item) => (
              <div key={item} style={capabilityItemStyle}>
                <CheckCircleOutlined style={{ color: '#16a34a' }} />
                <Typography.Text>{item}</Typography.Text>
              </div>
            ))}
          </div>
        </div>

        <div style={panelStyle}>
          <Typography.Title level={5} style={{ marginTop: 0 }}>集成状态</Typography.Title>
          <div style={statusBoxStyle}>
            <Progress percent={profile.progress} strokeColor="#1677ff" />
            <Typography.Paragraph type="secondary" style={{ margin: 0 }}>
              第一版先以工作流编排为核心，当前模块保留产品入口、菜单位置和后端扩展点，后续可按业务优先级接入真实数据。
            </Typography.Paragraph>
          </div>
        </div>

        <div style={panelStyle}>
          <Typography.Title level={5} style={{ marginTop: 0 }}>关联能力</Typography.Title>
          <div style={actionListStyle}>
            <RelatedAction icon={<RocketOutlined />} title="工作流编排" detail="将模块能力包装成可运行 DAG 节点" path="/workflows" />
            <RelatedAction icon={<ApiOutlined />} title="开放集成" detail="通过 SDK 或 API 接入第三方系统" path="/tools" />
            <RelatedAction icon={<FileTextOutlined />} title="运行审计" detail="在运行监控中追踪执行记录" path="/workflow-runs" />
          </div>
        </div>
      </section>
    </div>
  );
}

function RelatedAction({
  icon,
  title,
  detail,
  path
}: {
  icon: React.ReactNode;
  title: string;
  detail: string;
  path: string;
}) {
  return (
    <button type="button" style={relatedActionStyle} onClick={() => navigateTo(path)}>
      <span style={relatedIconStyle}>{icon}</span>
      <span>
        <span style={relatedTitleStyle}>{title}</span>
        <span style={relatedDetailStyle}>{detail}</span>
      </span>
    </button>
  );
}

function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

const moduleProfiles: Record<string, { description: string; phase: string; progress: number; capabilities: string[] }> = {
  '智能体': {
    description: '把已发布工作流包装成可对话、可配置、可投放的智能体应用。',
    phase: '后续阶段',
    progress: 35,
    capabilities: ['绑定工作流作为技能', '配置欢迎语和输入表单', '发布为聊天入口']
  },
  Prompt: {
    description: '集中管理 Prompt 模板、变量、版本和测试样例，为工作流节点复用提供素材。',
    phase: '后续阶段',
    progress: 45,
    capabilities: ['模板变量管理', '版本对比', 'Prompt 调试样例']
  },
  知识库: {
    description: '沉淀文档、向量检索和召回配置，让工作流能接入企业知识上下文。',
    phase: '后续阶段',
    progress: 30,
    capabilities: ['文档数据源', '向量索引状态', '召回测试']
  },
  工具插件: {
    description: '管理第三方 API、函数工具和业务系统连接器，供工作流节点调用。',
    phase: '后续阶段',
    progress: 40,
    capabilities: ['工具清单', '鉴权配置', '调用参数 Schema']
  },
  模型配置: {
    description: '统一维护模型供应商、模型参数和默认调用策略。',
    phase: '后续阶段',
    progress: 50,
    capabilities: ['供应商配置', '模型路由', '调用限额']
  },
  模型市场: {
    description: '为不同任务场景沉淀可选模型和推荐配置。',
    phase: '后续阶段',
    progress: 25,
    capabilities: ['模型目录', '场景推荐', '成本与能力对比']
  }
};

function defaultProfile(title: string) {
  return {
    description: `${title} 已接入系统导航，后续会按业务优先级补齐管理、配置和审计能力。`,
    phase: '平台治理阶段',
    progress: 20,
    capabilities: ['列表管理', '权限接入', '审计记录']
  };
}

const pageStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 16
};

const heroStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  borderBottom: '1px solid #edf0f5',
  display: 'flex',
  justifyContent: 'space-between',
  margin: '-16px -24px 0',
  padding: '20px 24px'
};

const heroCopyStyle: React.CSSProperties = {
  color: '#667085',
  margin: 0,
  maxWidth: 720
};

const gridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(0, 1.1fr) minmax(280px, 0.9fr) 320px'
};

const panelStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #edf0f5',
  borderRadius: 8,
  minHeight: 220,
  padding: 16
};

const titleRowStyle: React.CSSProperties = {
  justifyContent: 'space-between',
  marginBottom: 12,
  width: '100%'
};

const capabilityListStyle: React.CSSProperties = {
  display: 'grid',
  gap: 10
};

const capabilityItemStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#f8fafc',
  border: '1px solid #edf0f5',
  borderRadius: 8,
  display: 'flex',
  gap: 10,
  padding: '10px 12px'
};

const statusBoxStyle: React.CSSProperties = {
  display: 'grid',
  gap: 12
};

const actionListStyle: React.CSSProperties = {
  display: 'grid',
  gap: 10
};

const relatedActionStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fbfdff',
  border: '1px solid #e5ebf4',
  borderRadius: 8,
  cursor: 'pointer',
  display: 'grid',
  gap: 10,
  gridTemplateColumns: '36px minmax(0, 1fr)',
  padding: 12,
  textAlign: 'left',
  width: '100%'
};

const relatedIconStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#eef6ff',
  borderRadius: 8,
  color: '#1677ff',
  display: 'flex',
  height: 36,
  justifyContent: 'center',
  width: 36
};

const relatedTitleStyle: React.CSSProperties = {
  color: '#0f172a',
  display: 'block',
  fontWeight: 700
};

const relatedDetailStyle: React.CSSProperties = {
  color: '#667085',
  display: 'block',
  fontSize: 12,
  marginTop: 3
};
