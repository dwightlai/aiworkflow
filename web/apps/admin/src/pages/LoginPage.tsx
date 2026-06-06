import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, Typography, message } from 'antd';
import { useState } from 'react';
import { login, type AuthSession } from '../api/auth';

export function LoginPage({ onLogin }: { onLogin: (session: AuthSession) => void }) {
  const [loading, setLoading] = useState(false);

  async function handleFinish(values: { username: string; password: string }) {
    setLoading(true);
    try {
      const session = await login(values.username, values.password);
      onLogin(session);
      message.success('登录成功');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main
      style={{
        alignItems: 'center',
        background: 'linear-gradient(135deg, #eef5ff 0%, #f7f9fc 52%, #edf7f2 100%)',
        display: 'flex',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: 24
      }}
    >
      <Card style={{ borderRadius: 8, boxShadow: '0 18px 50px rgba(31, 42, 68, 0.12)', width: 420 }}>
        <Typography.Title level={3} style={{ marginTop: 0 }}>
          登录 AIFlow 管理端
        </Typography.Title>
        <Typography.Paragraph type="secondary">
          初始管理员：admin / admin123
        </Typography.Paragraph>
        <Alert
          showIcon
          type="info"
          message="首次登录后请及时修改管理员密码。"
          style={{ marginBottom: 20 }}
        />
        <Form layout="vertical" onFinish={handleFinish}>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={loading} aria-label="登录" block>
            登录
          </Button>
        </Form>
      </Card>
    </main>
  );
}
