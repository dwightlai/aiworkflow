import type { AuthUser } from '../api/auth';
import { isPlatformOperator } from '../api/auth';
import type { MenuGroup } from '../api/system';

export interface AppMenuItem {
  key: string;
  label: string;
  path: string;
}

export interface AppMenuGroup {
  title: string;
  items: AppMenuItem[];
}

export function mapNavigationToMenuGroups(groups: MenuGroup[]): AppMenuGroup[] {
  return groups.map((group) => ({
    title: group.title,
    items: group.items.map((item) => ({
      key: item.menuKey,
      label: item.title,
      path: item.path
    }))
  }));
}

export const menuGroups: AppMenuGroup[] = [
  {
    title: 'AI 功能',
    items: [
      { key: 'dashboard', label: '工作台', path: '/' },
      { key: 'bots', label: '智能体', path: '/bots' },
      { key: 'connectors', label: '连接器', path: '/connectors' },
      { key: 'connector-logs', label: '调用日志', path: '/connector-logs' },
      { key: 'agent-jobs', label: '异步任务', path: '/agent-jobs' },
      { key: 'agent-audit', label: '智能体审计', path: '/agent-audit' },
      { key: 'workflows', label: '工作流', path: '/workflows' },
      { key: 'workflow-runs', label: '运行监控', path: '/workflow-runs' },
      { key: 'knowledge', label: '知识库', path: '/knowledge' },
      { key: 'models', label: '模型配置', path: '/models' },
      { key: 'generation-templates', label: '编研模板', path: '/research/templates' },
      { key: 'research', label: '智能编研', path: '/research/compile' }
    ]
  },
  {
    title: '系统管理',
    items: [
      { key: 'tenants', label: '租户管理', path: '/system/tenants' },
      { key: 'identity', label: '组织用户', path: '/system/identity' },
      { key: 'asset-grants', label: '资产授权', path: '/system/asset-grants' },
      { key: 'integration-apps', label: '第三方应用', path: '/system/integration-apps' },
      { key: 'open-api-docs', label: '开放 API 文档', path: '/system/open-api-docs' },
      { key: 'menus', label: '菜单管理', path: '/system/menus' },
      { key: 'dictionary', label: '数据字典', path: '/system/dictionary' },
      { key: 'storage-settings', label: '存储路径配置', path: '/system/storage-settings' },
      { key: 'logs', label: '日志管理', path: '/system/logs' }
    ]
  }
];

export function getVisibleMenuGroups(user?: AuthUser | null): AppMenuGroup[] {
  const platformOperator = isPlatformOperator(user);
  return menuGroups.map((group) => ({
    ...group,
    items: group.items.filter((item) => platformOperator || item.key !== 'tenants')
  }));
}
