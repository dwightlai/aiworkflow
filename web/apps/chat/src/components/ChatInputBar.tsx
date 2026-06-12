import { ArrowUpOutlined } from '@ant-design/icons';
import { useEffect, useRef } from 'react';

export function ChatInputBar({
  value,
  loading,
  disabled,
  placeholder,
  onChange,
  onSubmit
}: {
  value: string;
  loading?: boolean;
  disabled?: boolean;
  placeholder?: string;
  onChange: (value: string) => void;
  onSubmit: (text: string) => void;
}) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const el = textareaRef.current;
    if (!el) {
      return;
    }
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
  }, [value]);

  function handleSubmit() {
    const trimmed = value.trim();
    if (!trimmed || loading || disabled) {
      return;
    }
    onSubmit(trimmed);
    onChange('');
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSubmit();
    }
  }

  const canSend = Boolean(value.trim()) && !loading && !disabled;

  return (
    <div className="chat-input-dock">
      <div className="chat-input-shell">
        <textarea
          ref={textareaRef}
          className="chat-input-textarea"
          value={value}
          disabled={disabled || loading}
          placeholder={placeholder ?? '给智能体发送消息'}
          rows={1}
          onChange={(event) => onChange(event.target.value)}
          onKeyDown={handleKeyDown}
        />
        <div className="chat-input-toolbar">
          <button
            type="button"
            className="chat-input-send"
            disabled={!canSend}
            aria-label="发送"
            onClick={handleSubmit}
          >
            <ArrowUpOutlined style={{ fontSize: 16 }} />
          </button>
        </div>
      </div>
      <span className="chat-input-hint">人工智能生成的内容可能不准确。</span>
    </div>
  );
}
