import type { AuthUser } from '../api/auth';
import { isPlatformOperator } from '../api/auth';

export interface AppMenuItem {
  key: string;
  label: string;
  path: string;
}

export interface AppMenuGroup {
  title: string;
  items: AppMenuItem[];
}

export const menuGroups: AppMenuGroup[] = [
  {
    title: 'AI 功能',
    items: [
      { key: 'dashboard', label: '工作台', path: '/' },
      { key: 'bots', label: '智能体 Bots', path: '/bots' },
      { key: 'workflows', label: '工作流', path: '/workflows' },
      { key: 'workflow-runs', label: '运行监控', path: '/workflow-runs' },
      { key: 'knowledge', label: '知识库', path: '/knowledge' },
      { key: 'models', label: '模型配置', path: '/models' }
    ]
  },
  {
    title: '系统管理',
    items: [
      { key: 'tenants', label: '租户管理', path: '/system/tenants' },
      { key: 'identity', label: '组织用户', path: '/system/identity' },
      { key: 'menus', label: '菜单管理', path: '/system/menus' },
      { key: 'dictionary', label: '数据字典', path: '/system/dictionary' },
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
