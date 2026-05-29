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
      { key: 'prompts', label: 'Prompt', path: '/prompts' },
      { key: 'knowledge', label: '知识库', path: '/knowledge' },
      { key: 'tools', label: '工具插件', path: '/tools' },
      { key: 'models', label: '模型配置', path: '/models' },
      { key: 'model-market', label: '模型市场', path: '/model-market' }
    ]
  },
  {
    title: '系统管理',
    items: [
      { key: 'users', label: '用户管理', path: '/system/users' },
      { key: 'roles', label: '角色管理', path: '/system/roles' },
      { key: 'menus', label: '菜单管理', path: '/system/menus' },
      { key: 'departments', label: '部门管理', path: '/system/departments' },
      { key: 'dictionary', label: '数据字典', path: '/system/dictionary' },
      { key: 'jobs', label: '定时任务', path: '/system/jobs' },
      { key: 'logs', label: '日志管理', path: '/system/logs' }
    ]
  }
];
