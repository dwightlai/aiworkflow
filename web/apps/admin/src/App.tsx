import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider, Layout, Typography } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { WorkflowListPage } from './pages/WorkflowListPage';

const queryClient = new QueryClient();

export function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={queryClient}>
        <Layout style={{ minHeight: '100vh', background: '#f4f6f8' }}>
          <Layout.Header
            style={{
              alignItems: 'center',
              background: '#18212f',
              display: 'flex',
              height: 56,
              paddingInline: 24
            }}
          >
            <Typography.Text style={{ color: '#fff', fontSize: 16, fontWeight: 700 }}>
              AI Workflow 控制台
            </Typography.Text>
          </Layout.Header>
          <Layout.Content style={{ padding: 24 }}>
            <WorkflowListPage />
          </Layout.Content>
        </Layout>
      </QueryClientProvider>
    </ConfigProvider>
  );
}
