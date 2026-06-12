import {
  DeleteOutlined,
  EditOutlined,
  EllipsisOutlined,
  PushpinFilled,
  PushpinOutlined,
  ShareAltOutlined
} from '@ant-design/icons';
import { Button, Dropdown, Input, Modal, Typography, message } from 'antd';
import type { MenuProps } from 'antd';
import { useState } from 'react';
import type { ChatSession } from '../api/chat';

export function ChatSessionList({
  sessions,
  activeSessionId,
  onSelect,
  onRename,
  onTogglePin,
  onShare,
  onDelete
}: {
  sessions: ChatSession[];
  activeSessionId: string | null;
  onSelect: (session: ChatSession) => void;
  onRename: (session: ChatSession, title: string) => void;
  onTogglePin: (session: ChatSession) => void;
  onShare: (session: ChatSession) => void;
  onDelete: (session: ChatSession) => void;
}) {
  const [renaming, setRenaming] = useState<ChatSession | null>(null);
  const [renameTitle, setRenameTitle] = useState('');

  if (sessions.length === 0) {
    return (
      <Typography.Text type="secondary" style={{ display: 'block', padding: '8px 4px' }}>
        暂无对话
      </Typography.Text>
    );
  }

  function openRename(session: ChatSession) {
    setRenaming(session);
    setRenameTitle(session.title || '对话');
  }

  function submitRename() {
    if (!renaming) {
      return;
    }
    const title = renameTitle.trim();
    if (!title) {
      message.warning('请输入名称');
      return;
    }
    onRename(renaming, title);
    setRenaming(null);
  }

  function buildMenu(session: ChatSession): MenuProps {
    return {
      items: [
        { key: 'rename', label: '重命名', icon: <EditOutlined /> },
        {
          key: 'pin',
          label: session.pinned ? '取消置顶' : '置顶',
          icon: session.pinned ? <PushpinFilled /> : <PushpinOutlined />
        },
        { key: 'share', label: '分享', icon: <ShareAltOutlined /> },
        { type: 'divider' },
        { key: 'delete', label: '删除', icon: <DeleteOutlined />, danger: true }
      ],
      onClick: ({ key, domEvent }) => {
        domEvent.stopPropagation();
        if (key === 'rename') {
          openRename(session);
          return;
        }
        if (key === 'pin') {
          onTogglePin(session);
          return;
        }
        if (key === 'share') {
          onShare(session);
          return;
        }
        if (key === 'delete') {
          Modal.confirm({
            title: '删除此对话？',
            content: '删除后无法恢复',
            okText: '删除',
            cancelText: '取消',
            okButtonProps: { danger: true },
            onOk: () => onDelete(session)
          });
        }
      }
    };
  }

  return (
    <>
      <div style={listStyle}>
        {sessions.map((session) => {
          const active = session.id === activeSessionId;
          return (
            <div
              key={session.id}
              style={{ ...itemStyle, background: active ? '#eef6ff' : 'transparent' }}
              onClick={() => onSelect(session)}
            >
              {session.pinned ? (
                <PushpinFilled style={{ color: '#1677ff', flexShrink: 0, fontSize: 12 }} />
              ) : null}
              <Typography.Text ellipsis style={{ flex: 1, minWidth: 0 }}>
                {session.title || '对话'}
              </Typography.Text>
              <Dropdown menu={buildMenu(session)} trigger={['click']} placement="bottomRight">
                <Button
                  type="text"
                  size="small"
                  icon={<EllipsisOutlined />}
                  aria-label="更多操作"
                  onClick={(event) => event.stopPropagation()}
                />
              </Dropdown>
            </div>
          );
        })}
      </div>
      <Modal
        title="重命名对话"
        open={Boolean(renaming)}
        okText="保存"
        cancelText="取消"
        onOk={submitRename}
        onCancel={() => setRenaming(null)}
        destroyOnClose
      >
        <Input
          value={renameTitle}
          maxLength={200}
          placeholder="输入对话名称"
          onChange={(event) => setRenameTitle(event.target.value)}
          onPressEnter={submitRename}
        />
      </Modal>
    </>
  );
}

const listStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 4,
  height: 'calc(100vh - 160px)',
  overflowY: 'auto'
};

const itemStyle: React.CSSProperties = {
  alignItems: 'center',
  borderRadius: 8,
  cursor: 'pointer',
  display: 'flex',
  gap: 6,
  padding: '8px 6px 8px 10px'
};

export function buildSessionShareUrl(botId: string, sessionId: string) {
  const url = new URL(window.location.href);
  url.pathname = `/bots/${botId}`;
  url.search = `?session=${encodeURIComponent(sessionId)}`;
  return url.toString();
}
