import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { useEffect, useState } from 'react';
import { getAuthSession, logout, type AuthSession } from './api/auth';
import { AdminShell } from './layout/AdminShell';
import { LoginPage } from './pages/LoginPage';
import { resolveRoute } from './routes';

const queryClient = new QueryClient();

export function App() {
  const [pathname, setPathname] = useState(() => window.location.pathname);
  const [session, setSession] = useState<AuthSession | null>(() => getAuthSession());
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

  function handleLogin(nextSession: AuthSession) {
    setSession(nextSession);
    navigateTo(pathname === '/login' ? '/' : pathname);
  }

  async function handleLogout() {
    await logout();
    queryClient.clear();
    setSession(null);
    navigateTo('/login');
  }

  return (
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={queryClient}>
        {session ? (
          <AdminShell
            title={route.title}
            breadcrumb={route.breadcrumb}
            currentPath={pathname}
            currentUser={session.user}
            onLogout={handleLogout}
            onNavigate={navigateTo}
          >
            {route.element}
          </AdminShell>
        ) : (
          <LoginPage onLogin={handleLogin} />
        )}
      </QueryClientProvider>
    </ConfigProvider>
  );
}
