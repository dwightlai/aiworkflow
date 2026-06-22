import type { ReactNode } from 'react';
import { BotsPage } from './pages/bots/BotsPage';
import { DashboardPage } from './pages/DashboardPage';
import { KnowledgeBasesPage } from './pages/knowledge/KnowledgeBasesPage';
import { KnowledgeDocumentCreatePage } from './pages/knowledge/KnowledgeDocumentCreatePage';
import { KnowledgeDocumentEditPage } from './pages/knowledge/KnowledgeDocumentEditPage';
import { KnowledgeDocumentsPage } from './pages/knowledge/KnowledgeDocumentsPage';
import { PlaceholderPage } from './pages/PlaceholderPage';
import { ModelProvidersPage } from './pages/models/ModelProvidersPage';
import { PromptTemplatesPage } from './pages/prompts/PromptTemplatesPage';
import { GenerationTemplatesPage } from './pages/research/GenerationTemplatesPage';
import { ResearchCompilationPage } from './pages/research/ResearchCompilationPage';
import { IdentityOrganizationPage } from './pages/system/IdentityOrganizationPage';
import { AssetGrantsPage } from './pages/system/AssetGrantsPage';
import { DictionaryPage } from './pages/system/DictionaryPage';
import { LogsPage } from './pages/system/LogsPage';
import { MenusPage } from './pages/system/MenusPage';
import { OpenApiDocsPage } from './pages/system/OpenApiDocsPage';
import { IntegrationAppsPage } from './pages/system/IntegrationAppsPage';
import { StorageSettingsPage } from './pages/system/StorageSettingsPage';
import { TenantsPage } from './pages/system/TenantsPage';
import { TenantWorkspacePage } from './pages/system/TenantWorkspacePage';
import { WorkflowCardsPage } from './pages/workflows/WorkflowCardsPage';
import { WorkflowDesignerPage } from './pages/workflows/WorkflowDesignerPage';
import { WorkflowRunDetailPage } from './pages/workflows/WorkflowRunDetailPage';
import { WorkflowRunsPage } from './pages/workflows/WorkflowRunsPage';
import { ConnectorsPage } from './pages/connectors/ConnectorsPage';
import { AgentAuditPage } from './pages/agent/AgentAuditPage';
import { ConnectorCallLogsPage } from './pages/agent/ConnectorCallLogsPage';
import { AgentJobsPage } from './pages/agent/AgentJobsPage';

export interface ResolvedRoute {
  title: string;
  breadcrumb: string[];
  element: ReactNode;
}

export function resolveRoute(pathname: string): ResolvedRoute {
  if (pathname === '/') {
    return {
      title: '工作台',
      breadcrumb: ['首页', 'AI 功能', '工作台'],
      element: <DashboardPage />
    };
  }

  if (pathname === '/workflows') {
    return {
      title: '工作流',
      breadcrumb: ['首页', 'AI 功能', '工作流'],
      element: <WorkflowCardsPage />
    };
  }

  if (pathname === '/bots') {
    return {
      title: '智能体',
      breadcrumb: ['首页', 'AI 功能', '智能体'],
      element: <BotsPage />
    };
  }

  if (pathname === '/connectors') {
    return {
      title: '连接器',
      breadcrumb: ['首页', 'AI 功能', '连接器'],
      element: <ConnectorsPage />
    };
  }

  if (pathname === '/agent-audit') {
    return {
      title: '智能体审计',
      breadcrumb: ['首页', 'AI 功能', '智能体审计'],
      element: <AgentAuditPage />
    };
  }

  if (pathname === '/connector-logs') {
    return {
      title: '调用日志',
      breadcrumb: ['首页', 'AI 功能', '调用日志'],
      element: <ConnectorCallLogsPage />
    };
  }

  if (pathname === '/agent-jobs') {
    return {
      title: '异步任务',
      breadcrumb: ['首页', 'AI 功能', '异步任务'],
      element: <AgentJobsPage />
    };
  }

  if (pathname.startsWith('/workflows/') && pathname.endsWith('/designer')) {
    const workflowId = pathname.split('/')[2] ?? 'new';
    return {
      title: '工作流设计器',
      breadcrumb: ['首页', 'AI 功能', '工作流', '设计器'],
      element: <WorkflowDesignerPage workflowId={workflowId} />
    };
  }

  if (pathname === '/workflow-runs') {
    return {
      title: '运行历史',
      breadcrumb: ['首页', 'AI 功能', '运行历史'],
      element: <WorkflowRunsPage />
    };
  }

  if (pathname.startsWith('/workflow-runs/')) {
    const executionId = pathname.split('/')[2] ?? '';
    return {
      title: '执行详情',
      breadcrumb: ['首页', 'AI 功能', '运行历史', '执行详情'],
      element: <WorkflowRunDetailPage executionId={executionId} />
    };
  }

  if (pathname === '/models') {
    return {
      title: '模型配置',
      breadcrumb: ['首页', 'AI 功能', '模型配置'],
      element: <ModelProvidersPage />
    };
  }

  if (pathname === '/prompts') {
    return {
      title: 'Prompt',
      breadcrumb: ['首页', 'AI 功能', 'Prompt'],
      element: <PromptTemplatesPage />
    };
  }

  if (pathname === '/knowledge') {
    return {
      title: '知识库',
      breadcrumb: ['首页', 'AI 功能', '知识库'],
      element: <KnowledgeBasesPage />
    };
  }

  if (pathname === '/research/templates') {
    return {
      title: '编研模板',
      breadcrumb: ['首页', 'AI 功能', '编研模板'],
      element: <GenerationTemplatesPage />
    };
  }

  if (pathname === '/research/compile') {
    return {
      title: '智能编研',
      breadcrumb: ['首页', 'AI 功能', '智能编研'],
      element: <ResearchCompilationPage />
    };
  }

  const tenantWorkspaceMatch = pathname.match(/^\/system\/tenants\/([^/]+)(?:\/(.*))?$/);
  if (tenantWorkspaceMatch) {
    const tenantId = tenantWorkspaceMatch[1] ?? '';
    const tab = tenantWorkspaceMatch[2];
    const defaultTab = tab === 'users' ? 'users' : tab === 'roles' ? 'roles' : tab === 'apps' || tab === 'integration-apps' ? 'integration-apps' : 'organizations';
    return {
      title: defaultTab === 'integration-apps' ? '租户第三方应用' : '租户工作台',
      breadcrumb: ['首页', '系统管理', '租户管理', defaultTab === 'integration-apps' ? '第三方应用' : '租户工作台'],
      element: <TenantWorkspacePage tenantId={tenantId} defaultTab={defaultTab} />
    };
  }

  if (pathname === '/system/tenants') {
    return {
      title: '租户管理',
      breadcrumb: ['首页', '系统管理', '租户管理'],
      element: <TenantsPage />
    };
  }

  if (pathname === '/system/users') {
    return {
      title: '组织用户',
      breadcrumb: ['首页', '系统管理', '组织用户'],
      element: <IdentityOrganizationPage defaultTab="users" />
    };
  }

  if (pathname === '/system/roles') {
    return {
      title: '组织用户',
      breadcrumb: ['首页', '系统管理', '组织用户'],
      element: <IdentityOrganizationPage defaultTab="roles" />
    };
  }

  if (pathname === '/system/departments') {
    return {
      title: '组织用户',
      breadcrumb: ['首页', '系统管理', '组织用户'],
      element: <IdentityOrganizationPage defaultTab="organizations" />
    };
  }

  if (pathname === '/system/units' || pathname === '/system/identity') {
    return {
      title: '组织用户',
      breadcrumb: ['首页', '系统管理', '组织用户'],
      element: <IdentityOrganizationPage defaultTab="organizations" />
    };
  }

  if (pathname === '/system/open-api-docs') {
    return {
      title: '开放 API 文档',
      breadcrumb: ['首页', '系统管理', '开放 API 文档'],
      element: <OpenApiDocsPage />
    };
  }

  if (pathname === '/system/integration-apps') {
    return {
      title: '第三方应用',
      breadcrumb: ['首页', '系统管理', '第三方应用'],
      element: <IntegrationAppsPage />
    };
  }

  if (pathname === '/system/asset-grants') {
    return {
      title: '资产授权',
      breadcrumb: ['首页', '系统管理', '资产授权'],
      element: <AssetGrantsPage />
    };
  }

  if (pathname === '/system/menus') {
    return {
      title: '菜单管理',
      breadcrumb: ['首页', '系统管理', '菜单管理'],
      element: <MenusPage />
    };
  }

  if (pathname === '/system/dictionary') {
    return {
      title: '数据字典',
      breadcrumb: ['首页', '系统管理', '数据字典'],
      element: <DictionaryPage />
    };
  }

  if (pathname === '/system/storage-settings') {
    return {
      title: '存储路径配置',
      breadcrumb: ['首页', '系统管理', '存储路径配置'],
      element: <StorageSettingsPage />
    };
  }

  if (pathname === '/system/logs') {
    return {
      title: '日志管理',
      breadcrumb: ['首页', '系统管理', '日志管理'],
      element: <LogsPage />
    };
  }

  if (pathname.startsWith('/knowledge/') && pathname.endsWith('/archive-topics')) {
    const knowledgeBaseId = pathname.split('/')[2] ?? '';
    return {
      title: '知识库资料',
      breadcrumb: ['首页', 'AI 功能', '知识库', '资料管理'],
      element: <KnowledgeDocumentsPage knowledgeBaseId={knowledgeBaseId} />
    };
  }

  if (pathname.startsWith('/knowledge/') && pathname.endsWith('/documents/new')) {
    const knowledgeBaseId = pathname.split('/')[2] ?? '';
    return {
      title: '新增文档',
      breadcrumb: ['首页', 'AI 功能', '知识库', '新增文档'],
      element: <KnowledgeDocumentCreatePage knowledgeBaseId={knowledgeBaseId} />
    };
  }

  if (pathname.startsWith('/knowledge/') && pathname.includes('/documents/') && pathname.endsWith('/edit')) {
    const [, , knowledgeBaseId, , documentId] = pathname.split('/');
    return {
      title: '编辑文档',
      breadcrumb: ['首页', 'AI 功能', '知识库', '编辑文档'],
      element: <KnowledgeDocumentEditPage knowledgeBaseId={knowledgeBaseId ?? ''} documentId={documentId ?? ''} />
    };
  }

  if (pathname.startsWith('/knowledge/') && pathname.endsWith('/documents')) {
    const knowledgeBaseId = pathname.split('/')[2] ?? '';
    return {
      title: '知识库资料',
      breadcrumb: ['首页', 'AI 功能', '知识库', '资料管理'],
      element: <KnowledgeDocumentsPage knowledgeBaseId={knowledgeBaseId} />
    };
  }

  const fallbackTitle = routeTitleByPath[pathname] ?? '功能建设中';
  return {
    title: fallbackTitle,
    breadcrumb: ['首页', 'AI Workflow', fallbackTitle],
    element: <PlaceholderPage title={fallbackTitle} />
  };
}

const routeTitleByPath: Record<string, string> = {
  '/bots': '智能体',
  '/connectors': '连接器',
  '/prompts': 'Prompt',
  '/knowledge': '知识库',
  '/research/templates': '编研模板',
  '/research/compile': '智能编研',
  '/tools': '工具插件',
  '/models': '模型配置',
  '/model-market': '模型市场',
  '/system/tenants': '租户管理',
  '/system/users': '组织用户',
  '/system/roles': '组织用户',
  '/system/asset-grants': '资产授权',
  '/system/integration-apps': '第三方应用',
  '/system/open-api-docs': '开放 API 文档',
  '/system/menus': '菜单管理',
  '/system/departments': '组织用户',
  '/system/dictionary': '数据字典',
  '/system/storage-settings': '存储路径配置',
  '/system/jobs': '定时任务',
  '/system/logs': '日志管理'
};
