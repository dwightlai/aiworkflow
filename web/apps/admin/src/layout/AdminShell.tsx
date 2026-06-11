import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { AuthUser } from '../api/auth';
import { listMenuNavigation } from '../api/system';
import { useState } from 'react';
import { PageHeader } from './PageHeader';
import { getVisibleMenuGroups, mapNavigationToMenuGroups, type AppMenuGroup } from './menu';

export interface AdminShellProps {
  breadcrumb: string[];
  currentPath?: string;
  currentUser?: AuthUser;
  children: ReactNode;
  onLogout?: () => void | Promise<void>;
  onNavigate?: (path: string) => void;
}

export function AdminShell({
  breadcrumb,
  currentPath = '/',
  currentUser,
  children,
  onLogout,
  onNavigate
}: AdminShellProps) {
  const [collapsed, setCollapsed] = useState(false);
  const navigationQuery = useQuery({
    queryKey: ['system', 'menu-navigation'],
    queryFn: listMenuNavigation,
    staleTime: 60_000
  });
  const visibleMenuGroups: AppMenuGroup[] = navigationQuery.data?.length
    ? mapNavigationToMenuGroups(navigationQuery.data)
    : getVisibleMenuGroups(currentUser);

  return (
    <div
      style={{
        background: '#f5f6f8',
        display: 'grid',
        gridTemplateColumns: collapsed ? '72px 1fr' : '216px 1fr',
        minHeight: '100vh'
      }}
    >
      <aside
        style={{
          background: '#fff',
          borderRight: '1px solid #e8edf4',
          overflow: 'hidden',
          padding: collapsed ? '16px 8px' : '18px 12px'
        }}
      >
        <div
          style={{
            color: '#1f2a44',
            fontSize: collapsed ? 18 : 26,
            fontWeight: 800,
            letterSpacing: 0.2,
            margin: collapsed ? '0 0 24px' : '0 10px 26px',
            whiteSpace: 'nowrap'
          }}
        >
          <span style={{ color: '#1062ff' }}>AI</span>{collapsed ? '' : 'Flow'}
        </div>

        {visibleMenuGroups.map((group) => (
          <div key={group.title}>
            {!collapsed ? (
              <div style={{ color: '#b7bfcc', fontSize: 13, margin: '18px 10px 10px' }}>
                {group.title}
              </div>
            ) : null}
            {group.items.map((item) => {
              const active = item.path === currentPath;
              return (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => onNavigate?.(item.path)}
                  style={{
                    background: active ? '#eef5ff' : 'transparent',
                    border: 0,
                    borderRadius: 8,
                    color: active ? '#0066ff' : '#3b4658',
                    cursor: 'pointer',
                    display: 'block',
                    font: 'inherit',
                    fontWeight: active ? 650 : 400,
                    margin: '4px 6px',
                    overflow: 'hidden',
                    padding: collapsed ? '11px 6px' : '11px 12px',
                    textAlign: 'left',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    width: collapsed ? 44 : 180
                  }}
                  title={item.label}
                >
                  {collapsed ? item.label.slice(0, 1) : item.label}
                </button>
              );
            })}
          </div>
        ))}
      </aside>

      <main style={{ minWidth: 0 }}>
        <PageHeader
          breadcrumb={breadcrumb}
          currentUser={currentUser}
          onLogout={onLogout}
          onToggleSidebar={() => setCollapsed((value) => !value)}
        />
        <div style={{ padding: 24 }}>{children}</div>
      </main>
    </div>
  );
}
