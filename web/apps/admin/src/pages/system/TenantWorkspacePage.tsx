import { ArrowLeftOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Space, Tag, Typography } from 'antd';
import { getTenant } from '../../api/identity';
import { IdentityOrganizationPage } from './IdentityOrganizationPage';
import { IntegrationAppsPage } from './IntegrationAppsPage';

interface TenantWorkspacePageProps {
  tenantId: string;
  defaultTab?: string;
}

function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

export function TenantWorkspacePage({ tenantId, defaultTab = 'organizations' }: TenantWorkspacePageProps) {
  const tenantQuery = useQuery({
    queryKey: ['identity', 'tenant', tenantId],
    queryFn: () => getTenant(tenantId)
  });
  const tenant = tenantQuery.data;

  return (
    <section>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Space align="start" style={{ justifyContent: 'space-between', width: '100%' }}>
          <Space direction="vertical" size={4}>
            <Button type="link" icon={<ArrowLeftOutlined />} style={{ padding: 0 }} onClick={() => navigateTo('/system/tenants')}>
              返回租户列表
            </Button>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {tenant?.name ?? '租户工作台'}
            </Typography.Title>
            <Space wrap>
              {tenant ? <Tag>{tenant.code}</Tag> : null}
              {tenant ? <Tag color={tenant.status === 'ACTIVE' ? 'success' : 'default'}>{tenant.status === 'ACTIVE' ? '启用' : '禁用'}</Tag> : null}
              <Typography.Text type="secondary">{tenantId}</Typography.Text>
            </Space>
          </Space>
        </Space>

        {tenantQuery.isError ? (
          <Alert type="error" showIcon message="租户信息加载失败" />
        ) : null}

        {defaultTab === 'integration-apps' ? (
          <IntegrationAppsPage tenantId={tenantId} />
        ) : (
          <IdentityOrganizationPage tenantId={tenantId} defaultTab={defaultTab} />
        )}
      </Space>
    </section>
  );
}
