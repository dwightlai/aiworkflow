import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export function MarkdownContent({ content, className }: { content: string; className?: string }) {
  return (
    <div className={className}>
      <Markdown remarkPlugins={[remarkGfm]}>{content}</Markdown>
    </div>
  );
}
