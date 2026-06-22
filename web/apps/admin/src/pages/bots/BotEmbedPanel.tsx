import { CopyOutlined } from '@ant-design/icons';
import { Alert, Button, Input, Typography, message } from 'antd';

export function BotEmbedPanel({ botId, botName }: { botId: string; botName: string }) {
  const origin = typeof window !== 'undefined' ? window.location.origin : 'https://your-domain.com';
  const iframeUrl = `${origin}/chat/bots/${botId}?ticket=<ticket>`;
  const snippet = `<script src="${origin}/chat/embed/agi-chat-widget.js"></script>
<script>
  AgiChatWidget.init({
    botId: '${botId}',
    baseUrl: '${origin}',
    title: '${botName.replace(/'/g, "\\'")}',
    primaryColor: '#1677ff',
    getTicket: async () => {
      const response = await fetch('/your-backend/chat/embed-ticket', { method: 'POST' });
      const data = await response.json();
      return data.ticket;
    }
  });
</script>`;

  async function copy(text: string) {
    try {
      await navigator.clipboard.writeText(text);
      message.success('已复制');
    } catch {
      message.error('复制失败');
    }
  }

  return (
    <div style={{ marginTop: 16 }}>
      <Typography.Title level={5}>嵌入 Widget</Typography.Title>
      <Typography.Paragraph type="secondary">
        第三方页面加载 JS SDK，由业务后端签发 ticket 后打开对话面板。也可用 iframe 直接嵌入。
      </Typography.Paragraph>
      <Typography.Text type="secondary">iframe 地址</Typography.Text>
      <Input.TextArea value={iframeUrl} readOnly autoSize={{ minRows: 2, maxRows: 3 }} style={{ marginTop: 8, marginBottom: 12 }} />
      <Button icon={<CopyOutlined />} onClick={() => copy(iframeUrl)} style={{ marginBottom: 16 }}>
        复制 iframe 地址
      </Button>
      <Typography.Text type="secondary">Widget 嵌入代码</Typography.Text>
      <Input.TextArea value={snippet} readOnly autoSize={{ minRows: 10, maxRows: 16 }} style={{ marginTop: 8, fontFamily: 'monospace' }} />
      <Button type="primary" icon={<CopyOutlined />} onClick={() => copy(snippet)} style={{ marginTop: 12 }}>
        复制嵌入代码
      </Button>
      <Alert
        type="info"
        showIcon
        style={{ marginTop: 12 }}
        message="ticket 需由可信后端调用 POST /api/open/chat/embed-tickets 签发；可传 unitId 做组织范围校验。"
      />
    </div>
  );
}
