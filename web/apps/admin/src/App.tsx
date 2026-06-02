import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { useEffect, useState } from 'react';
import { AdminShell } from './layout/AdminShell';
import { resolveRoute } from './routes';

const queryClient = new QueryClient();

export function App() {
  const [pathname, setPathname] = useState(() => window.location.pathname);
  const route = resolveRoute(pathname);

  useEffect(() => {
    const handlePopState = () => setPathname(window.location.pathname);
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  function navigateTo(path: string) {
    window.history.pushState(null, '', path);
    setPathname(path);
  }

  return (
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={queryClient}>
        <AdminShell
          title={route.title}
          breadcrumb={route.breadcrumb}
          currentPath={pathname}
          onNavigate={navigateTo}
        >
          {route.element}
        </AdminShell>
      </QueryClientProvider>
    </ConfigProvider>
  );
}
