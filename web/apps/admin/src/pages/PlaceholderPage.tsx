import { Card, Typography } from 'antd';

export interface PlaceholderPageProps {
  title: string;
}

export function PlaceholderPage({ title }: PlaceholderPageProps) {
  return (
    <Card>
      <Typography.Title level={4} style={{ marginTop: 0 }}>
        {title}
      </Typography.Title>
      <Typography.Text type="secondary">
        该模块入口已预留，将在后续阶段补齐业务能力。
      </Typography.Text>
    </Card>
  );
}
