# Bot Advanced Debug Input Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the bot test drawer's workflow input JSON into a default-collapsed advanced section without changing the existing request behavior.

**Architecture:** Keep parsing, validation, reset, and API submission in `BotsPage`. Let `BotRunChatDrawer` own only the collapsed presentation and derive an “已配置” indicator from the existing Ant Design form value.

**Tech Stack:** React 18, TypeScript, Ant Design 5, Vitest, Testing Library.

---

### Task 1: Collapse Advanced Workflow Input

**Files:**
- Modify: `web/apps/admin/src/components/bot/BotRunChatDrawer.tsx`
- Modify: `web/apps/admin/src/pages/bots/BotsPage.test.tsx`

- [ ] **Step 1: Write the failing interaction test**

Add this focused test to `BotsPage.test.tsx`:

```tsx
it('hides workflow input JSON under advanced debug parameters and still submits it', async () => {
  renderPage();

  await userEvent.click(await screen.findByRole('button', { name: /运行智能体/ }));

  expect(screen.queryByLabelText('工作流输入 JSON')).not.toBeInTheDocument();

  await userEvent.click(screen.getByText('高级调试参数'));
  const input = await screen.findByLabelText('工作流输入 JSON');
  fireEvent.change(input, {
    target: { value: '{"documentId":"doc-1001","department":"档案部"}' }
  });

  expect(screen.getByText('已配置')).toBeInTheDocument();

  fireEvent.change(screen.getByLabelText('测试消息'), {
    target: { value: '查询这份文档' }
  });
  await userEvent.click(screen.getByRole('button', { name: /发送消息/ }));

  await waitFor(() => {
    expect(botsApiMock.chatBot).toHaveBeenCalledWith('bot_1', {
      sessionId: undefined,
      message: '查询这份文档',
      input: {
        documentId: 'doc-1001',
        department: '档案部'
      }
    });
  });

  expect(input).toHaveValue('{"documentId":"doc-1001","department":"档案部"}');

  await userEvent.click(screen.getByRole('button', { name: /新会话/ }));
  expect(input).toHaveValue('{}');
});
```

The test must use the actual visible Chinese labels from the rendered application. Keep the existing bot create/edit/run/delete test unchanged.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
cd D:\openworkspace\aiworkflow\web
pnpm --filter @aiworkflow/admin test -- src/pages/bots/BotsPage.test.tsx
```

Expected: the test fails because “附加变量 JSON” is visible immediately and there is no “高级调试参数” control.

- [ ] **Step 3: Implement the collapsed advanced section**

In `BotRunChatDrawer.tsx`, add `Collapse` and `Tag` to the Ant Design imports and watch the existing form field:

```tsx
const workflowInput = Form.useWatch('input', chatForm) ?? '{}';
const workflowInputConfigured = hasConfiguredWorkflowInput(workflowInput);
```

Add this file-local helper:

```tsx
function hasConfiguredWorkflowInput(value: string) {
  try {
    const parsed = JSON.parse(value || '{}') as unknown;
    return Boolean(
      parsed
      && typeof parsed === 'object'
      && !Array.isArray(parsed)
      && Object.keys(parsed as Record<string, unknown>).length > 0
    );
  } catch {
    return false;
  }
}
```

Replace the always-visible input form item with:

```tsx
<Collapse
  ghost
  size="small"
  items={[
    {
      key: 'advanced-input',
      label: (
        <Space size={8}>
          <span>高级调试参数</span>
          {workflowInputConfigured ? <Tag color="blue">已配置</Tag> : null}
        </Space>
      ),
      children: (
        <Form.Item
          name="input"
          label="工作流输入 JSON"
          extra="用于向工作流传入 documentId、department 等自定义变量"
        >
          <Input.TextArea
            style={{ fontFamily: 'Consolas, monospace' }}
            autoSize={{ minRows: 2, maxRows: 4 }}
          />
        </Form.Item>
      )
    }
  ]}
/>
```

Do not supply `defaultActiveKey`; the section must remain collapsed initially. Keep the form's `initialValues={{ message: '', input: '{}' }}` unchanged.

- [ ] **Step 4: Run tests and build**

Run:

```powershell
cd D:\openworkspace\aiworkflow\web
pnpm --filter @aiworkflow/admin test -- src/pages/bots/BotsPage.test.tsx
pnpm --filter @aiworkflow/admin build
```

Expected: all `BotsPage` tests pass and the admin TypeScript/Vite build exits with code 0.

- [ ] **Step 5: Inspect the rendered behavior**

With the existing admin service on port 5173:

1. Open the bot run drawer.
2. Confirm the JSON input is not visible initially.
3. Expand “高级调试参数”.
4. Enter a non-empty JSON object and confirm “已配置” appears.
5. Send a message and confirm the conversation still runs.
6. Start a new session and confirm the JSON value resets to `{}`.

- [ ] **Step 6: Commit**

```powershell
git add web/apps/admin/src/components/bot/BotRunChatDrawer.tsx `
        web/apps/admin/src/pages/bots/BotsPage.test.tsx
git commit -m "feat: collapse advanced bot debug input"
```
