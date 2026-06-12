import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider, Layout, Typography } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { useEffect, useMemo, useState } from 'react';
import { getAuthSession, logout, type AuthSession } from './api/auth';
import { LoginPage } from './pages/LoginPage';
import { BotChatPage } from './pages/BotChatPage';
import { BotListPage } from './pages/BotListPage';

const queryClient = new QueryClient();

function parseRoute(pathname: string, search: string) {
  const params = new URLSearchParams(search);
  const ticket = params.get('ticket');
  const sessionId = params.get('session');
  const botMatch = pathname.match(/^\/bots\/([^/]+)$/);
  if (botMatch) {
    return { kind: 'chat' as const, botId: botMatch[1], ticket, sessionId };
  }
  if (pathname === '/' || pathname === '') {
    return { kind: 'list' as const };
  }
  if (pathname === '/login') {
    return { kind: 'login' as const };
  }
  return { kind: 'list' as const };
}

export function App() {
  const [pathname, setPathname] = useState(() => window.location.pathname);
  const [search, setSearch] = useState(() => window.location.search);
  const [session, setSession] = useState<AuthSession | null>(() => getAuthSession());
  const route = useMemo(() => parseRoute(pathname, search), [pathname, search]);

  useEffect(() => {
    const handlePopState = () => {
      setPathname(window.location.pathname);
      setSearch(window.location.search);
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  function navigateTo(path: string) {
    window.history.pushState(null, '', path);
    const url = new URL(path, window.location.origin);
    setPathname(url.pathname);
    setSearch(url.search);
  }

  function clearTicketFromUrl() {
    if (!window.location.search.includes('ticket=')) {
      return;
    }
    navigateTo(pathname);
  }

  function handleLogin(nextSession: AuthSession) {
    setSession(nextSession);
    navigateTo('/');
  }

  async function handleLogout() {
    await logout();
    queryClient.clear();
    setSession(null);
    navigateTo('/login');
  }

  const needsAuth = !session && route.kind !== 'login';

  return (
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={queryClient}>
        {needsAuth ? (
          <LoginPage onLogin={handleLogin} />
        ) : route.kind === 'login' && !session ? (
          <LoginPage onLogin={handleLogin} />
        ) : (
          <Layout style={{ minHeight: '100vh' }}>
            {route.kind !== 'chat' ? (
              <Layout.Header
                style={{
                  background: '#fff',
                  borderBottom: '1px solid #e7ecf3',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0 24px'
                }}
              >
                <Typography.Text strong style={{ fontSize: 18 }}>智能体对话</Typography.Text>
                {session ? (
                  <Typography.Link onClick={handleLogout}>
                    {session.user.displayName || session.user.username} · 退出
                  </Typography.Link>
                ) : null}
              </Layout.Header>
            ) : null}
            <Layout.Content style={route.kind === 'chat' ? { height: '100vh', overflow: 'hidden' } : undefined}>
              {route.kind === 'list' ? (
                <BotListPage onNavigate={navigateTo} />
              ) : route.kind === 'chat' ? (
                <BotChatPage
                  botId={route.botId}
                  ticket={route.ticket}
                  initialSessionId={route.sessionId}
                  onNavigate={navigateTo}
                  onTicketConsumed={clearTicketFromUrl}
                />
              ) : (
                <BotListPage onNavigate={navigateTo} />
              )}
            </Layout.Content>
          </Layout>
        )}
      </QueryClientProvider>
    </ConfigProvider>
  );
}
