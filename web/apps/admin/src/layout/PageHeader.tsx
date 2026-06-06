import { Button, Space, Typography } from 'antd';
import type { AuthUser } from '../api/auth';

export interface PageHeaderProps {
  title: string;
  breadcrumb: string[];
  currentUser?: AuthUser;
  onLogout?: () => void | Promise<void>;
  onToggleSidebar?: () => void;
}

export function PageHeader({ title, breadcrumb, currentUser, onLogout, onToggleSidebar }: PageHeaderProps) {
  return (
    <div
      style={{
        alignItems: 'center',
        background: '#fff',
        borderBottom: '1px solid #edf0f5',
        display: 'flex',
        gap: 14,
        height: 50,
        justifyContent: 'space-between',
        paddingInline: 24
      }}
    >
      <Space size={14}>
        <Button size="small" onClick={onToggleSidebar} aria-label="折叠菜单">
          ☰
        </Button>
        <Typography.Text style={{ color: '#1f2937', fontSize: 20, fontWeight: 700 }}>
          {title}
        </Typography.Text>
        <Typography.Text style={{ color: '#8d96a6' }}>
          {breadcrumb.join(' / ')}
        </Typography.Text>
      </Space>
      {currentUser ? (
        <Space>
          <Typography.Text>{currentUser.displayName || currentUser.username}</Typography.Text>
          <Button size="small" onClick={onLogout}>
            退出登录
          </Button>
        </Space>
      ) : null}
    </div>
  );
}
