const PREFIX = 'agi-chat-stream';

export interface StreamSessionState {
  botId: string;
  sessionId: string;
  assistantId: string;
  startedAt: number;
}

function key(botId: string, sessionId: string) {
  return `${PREFIX}:${botId}:${sessionId}`;
}

export function saveStreamSession(state: StreamSessionState) {
  sessionStorage.setItem(key(state.botId, state.sessionId), JSON.stringify(state));
}

export function readStreamSession(botId: string, sessionId: string): StreamSessionState | null {
  const raw = sessionStorage.getItem(key(botId, sessionId));
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as StreamSessionState;
  } catch {
    return null;
  }
}

export function clearStreamSession(botId: string, sessionId: string) {
  sessionStorage.removeItem(key(botId, sessionId));
}
