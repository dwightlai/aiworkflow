# AgentFlowy API 快速接入指南

## 1. 安装 SDK

```bash
npm install @agentflowy/sdk
# 或
pip install agentflowy-client
```

## 2. 初始化客户端

```javascript
import { AgentFlowyClient } from '@agentflowy/sdk';

const client = new AgentFlowyClient({
  apiKey: process.env.AGENTFLOWY_API_KEY,
  baseUrl: 'https://api.agentflowy.com/v1',
  timeout: 30000,
  retryConfig: {
    maxRetries: 3,
    backoffMs: 1000,
    retryableStatuses: [429, 500, 502, 503]
  }
});

// 验证连接
(async () => {
  const healthy = await client.healthCheck();
  console.log('连接状态:', healthy ? '正常' : '异常');
})();
```

## 3. 创建工作流

```javascript
const workflow = await client.workflows.create({
  name: '客服自动回复',
  description: '根据用户问题自动匹配知识库并生成回复',
  steps: [
    {
      type: 'receive',
      name: '接收消息',
      config: {
        inputKey: 'user_message',
        channel: 'webhook'
      }
    },
    {
      type: 'search',
      name: '知识库检索',
      config: {
        kbId: 'kb_2024_q4',
        topK: 5,
        minScore: 0.7
      }
    },
    {
      type: 'llm',
      name: '生成回复',
      config: {
        model: 'gpt-4o',
        template: '基于以下知识库内容回答用户问题：\n\n{search_results}\n\n用户问题：{user_message}',
        temperature: 0.3,
        maxTokens: 2000
      }
    },
    {
      type: 'send',
      name: '发送回复',
      config: {
        channel: 'webhook_response'
      }
    }
  ]
});

console.log('工作流ID:', workflow.id);
```

> **固定长度切分（chunk_size=400）破坏效果演示**：
> - 块1：安装 + 初始化 JS 代码开头 → 在 `new AgentFlowyClient(` 对象参数中间被切断 ❌
> - 块2：剩余 JS 代码片段 + Python 创建流程 → 两块都不可执行
>
> **后果：用户搜索"AgentFlowyClient retryConfig"时，对象参数被切开，代码块无法被正确理解和使用**
