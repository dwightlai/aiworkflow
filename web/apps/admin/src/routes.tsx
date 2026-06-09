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
import { IdentityOrganizationPage } from './pages/system/IdentityOrganizationPage';
import { AssetGrantsPage } from './pages/system/AssetGrantsPage';
import { IntegrationAppsPage } from './pages/system/IntegrationAppsPage';
import { TenantsPage } from './pages/system/TenantsPage';
import { TenantWorkspacePage } from './pages/system/TenantWorkspacePage';
import { WorkflowCardsPage } from './pages/workflows/WorkflowCardsPage';
import { WorkflowDesignerPage } from './pages/workflows/WorkflowDesignerPage';
import { WorkflowRunDetailPage } from './pages/workflows/WorkflowRunDetailPage';
import { WorkflowRunsPage } from './pages/workflows/WorkflowRunsPage';

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
      title: '智能体 Bots',
      breadcrumb: ['首页', 'AI 功能', '智能体 Bots'],
      element: <BotsPage />
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
      title: '知识库文档',
      breadcrumb: ['首页', 'AI 功能', '知识库', '文档管理'],
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
  '/bots': '智能体 Bots',
  '/prompts': 'Prompt',
  '/knowledge': '知识库',
  '/tools': '工具插件',
  '/models': '模型配置',
  '/model-market': '模型市场',
  '/system/tenants': '租户管理',
  '/system/users': '组织用户',
  '/system/roles': '组织用户',
  '/system/asset-grants': '资产授权',
  '/system/integration-apps': '第三方应用',
  '/system/departments': '组织用户',
  '/system/dictionary': '数据字典',
  '/system/jobs': '定时任务',
  '/system/logs': '日志管理'
};
