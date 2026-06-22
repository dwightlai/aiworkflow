import { Alert, Button, Card, Progress, Space, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { confirmChatTask, getConfirmTask, rejectChatTask } from '../api/chat';
import type { CitationItem, PlatformMessage } from '../utils/sseAdapter';
import { citationFromMetadata } from '../utils/sseAdapter';

function taskIdFrom(msg: PlatformMessage): string | null {
  const meta = msg.metadata ?? {};
  const id = meta.taskId ?? meta.confirmTaskId;
  return id == null ? null : String(id);
}

function payloadFrom(msg: PlatformMessage): Record<string, unknown> | null {
  const meta = msg.metadata ?? {};
  const snapshot = meta.payloadSnapshot;
  if (snapshot && typeof snapshot === 'object') {
    return snapshot as Record<string, unknown>;
  }
  return null;
}

function PayloadPreview({ payload }: { payload: Record<string, unknown> | null }) {
  if (!payload || Object.keys(payload).length === 0) {
    return null;
  }
  return <pre className="chat-confirm-payload">{JSON.stringify(payload, null, 2)}</pre>;
}

export function ConfirmCard({
  msg,
  onDone
}: {
  msg: PlatformMessage;
  onDone?: () => void;
}) {
  const [loading, setLoading] = useState<'confirm' | 'reject' | null>(null);
  const [resolved, setResolved] = useState<'CONFIRMED' | 'REJECTED' | null>(null);
  const [payload, setPayload] = useState<Record<string, unknown> | null>(payloadFrom(msg));
  const taskId = taskIdFrom(msg);
  const summary = String(msg.metadata?.summary ?? msg.content.replace(/^待确认：/, ''));

  useEffect(() => {
    if (payload || !taskId) {
      return;
    }
    getConfirmTask(taskId)
      .then((task) => setPayload(task.payloadSnapshot ?? null))
      .catch(() => undefined);
  }, [payload, taskId]);

  async function handleConfirm() {
    if (!taskId || loading) {
      return;
    }
    setLoading('confirm');
    try {
      await confirmChatTask(taskId);
      setResolved('CONFIRMED');
      onDone?.();
    } finally {
      setLoading(null);
    }
  }

  async function handleReject() {
    if (!taskId || loading) {
      return;
    }
    setLoading('reject');
    try {
      await rejectChatTask(taskId);
      setResolved('REJECTED');
      onDone?.();
    } finally {
      setLoading(null);
    }
  }

  if (resolved) {
    return (
      <Alert
        type={resolved === 'CONFIRMED' ? 'success' : 'info'}
        showIcon
        message={resolved === 'CONFIRMED' ? '已确认并继续执行' : '已取消操作'}
      />
    );
  }

  return (
    <Card size="small" title="待确认操作" className="chat-card chat-confirm-card">
      <Typography.Paragraph style={{ marginBottom: 12 }}>{summary}</Typography.Paragraph>
      <PayloadPreview payload={payload} />
      {taskId ? (
        <Space>
          <Button type="primary" loading={loading === 'confirm'} onClick={handleConfirm}>
            确认
          </Button>
          <Button danger loading={loading === 'reject'} onClick={handleReject}>
            拒绝
          </Button>
        </Space>
      ) : (
        <Typography.Text type="secondary">缺少确认任务 ID</Typography.Text>
      )}
    </Card>
  );
}

export function CitationList({ citations }: { citations: CitationItem[] }) {
  const [expanded, setExpanded] = useState(false);
  if (citations.length === 0) {
    return null;
  }
  const visible = expanded ? citations : citations.slice(0, 3);
  return (
    <div className="chat-citation-foot">
      <div className="chat-citation-foot-title">引用文档</div>
      <ul className="chat-citation-foot-list">
        {visible.map((item, index) => (
          <li key={item.id} className="chat-citation-foot-item">
            <span className="chat-citation-foot-index">{index + 1}.</span>
            <span className="chat-citation-foot-text">{item.title}</span>
          </li>
        ))}
      </ul>
      {citations.length > 3 ? (
        <button type="button" className="chat-citation-foot-more" onClick={() => setExpanded((v) => !v)}>
          {expanded ? '收起' : `全部 ${citations.length} 条`}
        </button>
      ) : null}
    </div>
  );
}

function citationDedupeKey(item: CitationItem): string {
  return item.title.replace(/\s+/g, ' ').trim().toLowerCase();
}

export function citationsFromMessages(messages: PlatformMessage[]): CitationItem[] {
  const bestByTitle = new Map<string, CitationItem>();
  for (const msg of messages) {
    const item = citationFromMetadata(msg.id, msg.metadata, msg.content.replace(/^引用：/, ''));
    const key = citationDedupeKey(item);
    const existing = bestByTitle.get(key);
    if (!existing || (item.score ?? 0) > (existing.score ?? 0)) {
      bestByTitle.set(key, item);
    }
  }
  return [...bestByTitle.values()];
}

export function mergeCitations(...groups: CitationItem[][]): CitationItem[] {
  const bestByTitle = new Map<string, CitationItem>();
  for (const group of groups) {
    for (const item of group) {
      const key = citationDedupeKey(item);
      const existing = bestByTitle.get(key);
      if (!existing || (item.score ?? 0) > (existing.score ?? 0)) {
        bestByTitle.set(key, item);
      }
    }
  }
  return [...bestByTitle.values()];
}

export function ProgressCard({ msg }: { msg: PlatformMessage }) {
  const meta = msg.metadata ?? {};
  const progress = Number(meta.progress ?? 0);
  const step = String(meta.currentStep ?? meta.step ?? '');
  return (
    <Card size="small" title="任务进度" className="chat-card chat-progress-card">
      <Progress percent={Number.isFinite(progress) ? progress : 0} size="small" />
      {step ? <Typography.Text type="secondary">{step}</Typography.Text> : null}
    </Card>
  );
}

export function JobCompletedCard({ msg }: { msg: PlatformMessage }) {
  const meta = msg.metadata ?? {};
  const title = String(meta.title ?? '任务已完成');
  const downloadUrl = meta.downloadUrl ? String(meta.downloadUrl) : null;
  const resultUrl = meta.resultUrl ? String(meta.resultUrl) : null;
  const linkUrl = downloadUrl ?? resultUrl;
  return (
    <Card size="small" title={title} className="chat-card chat-job-card">
      {linkUrl ? (
        <Button type="link" href={linkUrl} target="_blank" rel="noreferrer">
          下载结果
        </Button>
      ) : (
        <Typography.Text type="secondary">任务 ID：{String(meta.jobId ?? '')}</Typography.Text>
      )}
    </Card>
  );
}

export function ErrorCard({ msg }: { msg: PlatformMessage }) {
  const text = String(msg.metadata?.message ?? msg.content.replace(/^错误：/, ''));
  return <Alert type="error" showIcon message="执行失败" description={text} className="chat-card" />;
}
