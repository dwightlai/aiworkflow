import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider, Layout } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { WorkflowListPage } from './pages/WorkflowListPage';

const queryClient = new QueryClient();

export function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={queryClient}>
        <Layout style={{ minHeight: '100vh' }}>
          <Layout.Header style={{ color: '#fff', fontWeight: 600 }}>
            AI Workflow
          </Layout.Header>
          <Layout.Content style={{ padding: 24 }}>
            <WorkflowListPage />
          </Layout.Content>
        </Layout>
      </QueryClientProvider>
    </ConfigProvider>
  );
}
