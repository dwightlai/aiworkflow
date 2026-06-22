export interface AgiChatWidgetOptions {
  botId: string;
  baseUrl?: string;
  title?: string;
  position?: 'right' | 'left';
  zIndex?: number;
  primaryColor?: string;
  getTicket: () => Promise<string>;
}

type WidgetInstance = {
  open: () => Promise<void>;
  close: () => void;
  destroy: () => void;
};

const STYLE_ID = 'agi-chat-widget-style';

function ensureStyles(zIndex: number, primaryColor: string) {
  const existing = document.getElementById(STYLE_ID);
  if (existing) {
    existing.remove();
  }
  const style = document.createElement('style');
  style.id = STYLE_ID;
  style.textContent = `
    .agi-chat-widget-launcher {
      position: fixed;
      bottom: 24px;
      width: 56px;
      height: 56px;
      border: none;
      border-radius: 50%;
      background: ${primaryColor};
      color: #fff;
      font-size: 24px;
      cursor: pointer;
      box-shadow: 0 8px 24px color-mix(in srgb, ${primaryColor} 35%, transparent);
      z-index: ${zIndex};
    }
    .agi-chat-widget-launcher.right { right: 24px; }
    .agi-chat-widget-launcher.left { left: 24px; }
    .agi-chat-widget-panel {
      position: fixed;
      bottom: 96px;
      width: min(420px, calc(100vw - 32px));
      height: min(680px, calc(100vh - 120px));
      border: 1px solid #e7ecf3;
      border-radius: 16px;
      overflow: hidden;
      background: #fff;
      box-shadow: 0 16px 48px rgba(15, 23, 42, 0.18);
      z-index: ${zIndex + 1};
      display: none;
    }
    .agi-chat-widget-panel.open { display: block; }
    .agi-chat-widget-panel.right { right: 24px; }
    .agi-chat-widget-panel.left { left: 24px; }
    .agi-chat-widget-header {
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 12px 0 16px;
      border-bottom: 1px solid #e7ecf3;
      background: #fafcff;
      font: 600 14px/1.4 system-ui, sans-serif;
      color: #0f172a;
    }
    .agi-chat-widget-close {
      border: none;
      background: transparent;
      font-size: 20px;
      cursor: pointer;
      color: #64748b;
    }
    .agi-chat-widget-frame {
      width: 100%;
      height: calc(100% - 48px);
      border: 0;
    }
  `;
  document.head.appendChild(style);
}

function normalizeBaseUrl(baseUrl?: string) {
  const origin = baseUrl?.trim() || window.location.origin;
  return origin.replace(/\/$/, '');
}

function postToParent(payload: Record<string, unknown>) {
  if (window.parent && window.parent !== window) {
    window.parent.postMessage(payload, '*');
  }
}

function createWidget(options: AgiChatWidgetOptions): WidgetInstance {
  const baseUrl = normalizeBaseUrl(options.baseUrl);
  const position = options.position === 'left' ? 'left' : 'right';
  const zIndex = options.zIndex ?? 2147483000;
  const primaryColor = options.primaryColor?.trim() || '#1677ff';
  ensureStyles(zIndex, primaryColor);

  const launcher = document.createElement('button');
  launcher.type = 'button';
  launcher.className = `agi-chat-widget-launcher ${position}`;
  launcher.setAttribute('aria-label', options.title ?? '打开智能体对话');
  launcher.textContent = '💬';

  const panel = document.createElement('div');
  panel.className = `agi-chat-widget-panel ${position}`;

  const header = document.createElement('div');
  header.className = 'agi-chat-widget-header';
  const title = document.createElement('span');
  title.textContent = options.title ?? '智能体助手';
  const closeBtn = document.createElement('button');
  closeBtn.type = 'button';
  closeBtn.className = 'agi-chat-widget-close';
  closeBtn.setAttribute('aria-label', '关闭');
  closeBtn.textContent = '×';
  header.append(title, closeBtn);

  const iframe = document.createElement('iframe');
  iframe.className = 'agi-chat-widget-frame';
  iframe.title = options.title ?? '智能体对话';
  iframe.allow = 'clipboard-write';
  panel.append(header, iframe);

  document.body.append(launcher, panel);

  let ticketLoaded = false;

  window.addEventListener('message', (event) => {
    if (event.source !== iframe.contentWindow) {
      return;
    }
    if (event.data?.type === 'AGI_CHAT_READY') {
      postToParent({ type: 'AGI_CHAT_WIDGET_READY', botId: options.botId });
    }
  });

  async function ensureIframe() {
    if (ticketLoaded) {
      return;
    }
    const ticket = await options.getTicket();
    const query = new URLSearchParams({
      embed: '1',
      ticket,
      primaryColor
    });
    iframe.src = `${baseUrl}/chat/bots/${encodeURIComponent(options.botId)}?${query.toString()}`;
    ticketLoaded = true;
  }

  async function open() {
    await ensureIframe();
    panel.classList.add('open');
    postToParent({ type: 'AGI_CHAT_WIDGET_OPEN', botId: options.botId });
  }

  function close() {
    panel.classList.remove('open');
    postToParent({ type: 'AGI_CHAT_WIDGET_CLOSE', botId: options.botId });
  }

  function destroy() {
    launcher.remove();
    panel.remove();
    postToParent({ type: 'AGI_CHAT_WIDGET_DESTROY', botId: options.botId });
  }

  launcher.addEventListener('click', () => {
    if (panel.classList.contains('open')) {
      close();
    } else {
      void open();
    }
  });
  closeBtn.addEventListener('click', close);

  return { open, close, destroy };
}

const AgiChatWidget = {
  init(options: AgiChatWidgetOptions) {
    if (!options?.botId) {
      throw new Error('botId is required');
    }
    if (typeof options.getTicket !== 'function') {
      throw new Error('getTicket is required');
    }
    return createWidget(options);
  }
};

declare global {
  interface Window {
    AgiChatWidget: typeof AgiChatWidget;
  }
}

window.AgiChatWidget = AgiChatWidget;

export { AgiChatWidget };
