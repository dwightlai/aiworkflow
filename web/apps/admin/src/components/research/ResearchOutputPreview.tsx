import { useQuery } from '@tanstack/react-query';
import { Alert, Spin } from 'antd';
import { renderAsync } from 'docx-preview';
import { useEffect, useRef } from 'react';
import { fetchResearchOutputDocx, fetchResearchOutputHtmlPreview } from '../../api/research';
import '../../styles/botRunChat.css';
import '../../styles/researchOutputPreview.css';
import { MarkdownContent } from '../MarkdownContent';

export function MarkdownPreviewPanel({ content }: { content: string }) {
  return (
    <div className="research-output-preview">
      <MarkdownContent content={content} className="bot-run-assistant-content" />
    </div>
  );
}

export function HtmlPreviewPanel({ outputId }: { outputId: string }) {
  const query = useQuery({
    queryKey: ['research-output-html', outputId],
    queryFn: () => fetchResearchOutputHtmlPreview(outputId)
  });
  if (query.isLoading) {
    return <Spin />;
  }
  if (query.isError) {
    return <Alert type="error" showIcon message={(query.error as Error).message || 'HTML 预览加载失败'} />;
  }
  return (
    <iframe
      title="html-preview"
      srcDoc={query.data}
      className="research-output-html-frame"
    />
  );
}

export function DocxPreviewPanel({
  outputId,
  enabled,
  versionKey
}: {
  outputId: string;
  enabled: boolean;
  versionKey?: string | null;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const query = useQuery({
    queryKey: ['research-output-docx', outputId, versionKey],
    queryFn: () => fetchResearchOutputDocx(outputId),
    enabled
  });

  useEffect(() => {
    const container = containerRef.current;
    if (!container || !query.data) {
      return;
    }
    container.innerHTML = '';
    void renderAsync(query.data, container, undefined, {
      className: 'docx',
      inWrapper: true
    });
  }, [query.data]);

  if (!enabled) {
    return null;
  }
  if (query.isLoading) {
    return <Spin />;
  }
  if (query.isError) {
    return <Alert type="error" showIcon message={(query.error as Error).message || 'DOCX 预览加载失败'} />;
  }
  return <div ref={containerRef} className="research-output-docx-preview" />;
}
