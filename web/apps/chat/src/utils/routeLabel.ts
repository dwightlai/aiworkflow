export const ROUTE_MATCH_LABELS: Record<string, string> = {
  default: '默认工作流',
  single: '唯一候选',
  primary: '主工作流兜底',
  empty: '空消息'
};

export function formatRouteMatchReason(reason?: string | null): string {
  if (!reason) {
    return '';
  }
  if (ROUTE_MATCH_LABELS[reason]) {
    return ROUTE_MATCH_LABELS[reason];
  }
  if (reason.startsWith('keyword:')) {
    return `关键词「${reason.slice(8)}」`;
  }
  if (reason.startsWith('code:')) {
    return `意图编码「${reason.slice(5)}」`;
  }
  return reason;
}
