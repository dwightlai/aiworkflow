import type { ReactNode } from 'react';
import { DashboardPage } from './pages/DashboardPage';
import { KnowledgeBasesPage } from './pages/knowledge/KnowledgeBasesPage';
import { PlaceholderPage } from './pages/PlaceholderPage';
import { ModelProvidersPage } from './pages/models/ModelProvidersPage';
import { PromptTemplatesPage } from './pages/prompts/PromptTemplatesPage';
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
  '/system/users': '用户管理',
  '/system/roles': '角色管理',
  '/system/menus': '菜单管理',
  '/system/departments': '部门管理',
  '/system/dictionary': '数据字典',
  '/system/jobs': '定时任务',
  '/system/logs': '日志管理'
};
