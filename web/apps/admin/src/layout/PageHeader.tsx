import { Button, Typography } from 'antd';

export interface PageHeaderProps {
  title: string;
  breadcrumb: string[];
  onToggleSidebar?: () => void;
}

export function PageHeader({ title, breadcrumb, onToggleSidebar }: PageHeaderProps) {
  return (
    <div
      style={{
        alignItems: 'center',
        background: '#fff',
        borderBottom: '1px solid #edf0f5',
        display: 'flex',
        gap: 14,
        height: 50,
        paddingInline: 24
      }}
    >
      <Button size="small" onClick={onToggleSidebar} aria-label="折叠菜单">
        ☰
      </Button>
      <Typography.Text style={{ color: '#1f2937', fontSize: 20, fontWeight: 700 }}>
        {title}
      </Typography.Text>
      <Typography.Text style={{ color: '#8d96a6' }}>
        {breadcrumb.join(' / ')}
      </Typography.Text>
    </div>
  );
}
