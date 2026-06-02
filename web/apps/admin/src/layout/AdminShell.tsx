import type { ReactNode } from 'react';
import { useState } from 'react';
import { PageHeader } from './PageHeader';
import { menuGroups } from './menu';

export interface AdminShellProps {
  title: string;
  breadcrumb: string[];
  currentPath?: string;
  children: ReactNode;
  onNavigate?: (path: string) => void;
}

export function AdminShell({
  title,
  breadcrumb,
  currentPath = '/',
  children,
  onNavigate
}: AdminShellProps) {
  const [collapsed, setCollapsed] = useState(false);

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

        {menuGroups.map((group) => (
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
          title={title}
          breadcrumb={breadcrumb}
          onToggleSidebar={() => setCollapsed((value) => !value)}
        />
        <div style={{ padding: 24 }}>{children}</div>
      </main>
    </div>
  );
}
