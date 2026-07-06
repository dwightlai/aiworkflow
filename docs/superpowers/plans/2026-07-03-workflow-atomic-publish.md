# Workflow Atomic Publish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the workflow designer publish the current name, description, nodes, edges, variables, and UI configuration atomically before activating the new version.

**Architecture:** Extend the existing publish endpoint with an optional request body. A new transactional application-service method validates the complete definition before updating metadata and draft state, then publishes that exact draft; callers that omit the body retain the existing behavior. The React designer sends a snapshot read directly from the designer instance, while workflow-list publishing remains unchanged.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring transactions, JUnit 5, MockMvc, React 18, TypeScript, TanStack Query, Vitest, Testing Library.

---

## File Structure

- Modify `server/src/main/java/com/mw/ai/agi/workflow/api/WorkflowController.java`: accept the optional atomic publish payload while preserving body-less publishing.
- Modify `server/src/main/java/com/mw/ai/agi/workflow/service/WorkflowApplicationService.java`: own the transactional save-and-publish operation and archived-state guard.
- Modify `server/src/test/java/com/mw/ai/agi/workflow/api/WorkflowControllerIntegrationTest.java`: verify latest metadata and multi-knowledge-base definition are published together and legacy publishing remains compatible.
- Modify `web/apps/admin/src/api/workflows.ts`: define the publish payload and send it only when supplied.
- Modify `web/apps/admin/src/api/workflows.test.ts`: verify body-less and full-snapshot publish requests.
- Modify `web/apps/admin/src/pages/workflows/WorkflowDesignerPage.tsx`: build and submit the current designer snapshot.
- Modify `web/apps/admin/src/pages/workflows/WorkflowDesignerPage.test.tsx`: prove direct publishing sends unsaved editor state.

### Task 1: Backend Atomic Publish Contract

**Files:**
- Modify: `server/src/test/java/com/mw/ai/agi/workflow/api/WorkflowControllerIntegrationTest.java`
- Modify: `server/src/main/java/com/mw/ai/agi/workflow/api/WorkflowController.java`
- Modify: `server/src/main/java/com/mw/ai/agi/workflow/service/WorkflowApplicationService.java`

- [ ] **Step 1: Write the failing integration test**

Add a test that creates a workflow and publishes a new snapshot without calling the draft or metadata endpoints first:

```java
@Test
void atomicallySavesMetadataAndDefinitionBeforePublishing() throws Exception {
    String workflowId = createWorkflow();

    mockMvc.perform(post("/api/workflows/{workflowId}/publish", workflowId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "name": "Knowledge federation",
                              "description": "Searches two knowledge bases",
                              "definition": {
                                "nodes": [
                                  { "id": "start", "type": "START", "name": "Start", "config": {} },
                                  {
                                    "id": "knowledge",
                                    "type": "KNOWLEDGE_RETRIEVAL",
                                    "name": "Knowledge",
                                    "config": {
                                      "knowledgeBaseIds": ["kb-standards", "kb-policies"],
                                      "queryText": "{{message}}",
                                      "topK": 5
                                    }
                                  },
                                  { "id": "end", "type": "END", "name": "End", "config": {} }
                                ],
                                "edges": [
                                  { "id": "edge-1", "sourceNodeId": "start", "targetNodeId": "knowledge", "condition": null },
                                  { "id": "edge-2", "sourceNodeId": "knowledge", "targetNodeId": "end", "condition": null }
                                ],
                                "variables": []
                              }
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Knowledge federation"))
            .andExpect(jsonPath("$.data.description").value("Searches two knowledge bases"))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.latestVersion.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.latestVersion.definition.nodes[1].config.knowledgeBaseIds[0]")
                    .value("kb-standards"))
            .andExpect(jsonPath("$.data.latestVersion.definition.nodes[1].config.knowledgeBaseIds[1]")
                    .value("kb-policies"));
}
```

Retain the existing `publishesWorkflowDraft()` test as the compatibility test for an empty request body.

Add a rollback-oriented validation test:

```java
@Test
void rejectsInvalidAtomicPublishBeforeChangingMetadata() throws Exception {
    String workflowId = createWorkflow();

    mockMvc.perform(post("/api/workflows/{workflowId}/publish", workflowId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "name": "Must not be saved",
                              "description": "Invalid snapshot",
                              "definition": {
                                "nodes": [
                                  { "id": "start", "type": "START", "name": "Start", "config": {} },
                                  { "id": "end", "type": "END", "name": "End", "config": {} }
                                ],
                                "edges": [
                                  { "id": "broken", "sourceNodeId": "start", "targetNodeId": "missing", "condition": null }
                                ],
                                "variables": []
                              }
                            }
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_WORKFLOW_DAG"));

    mockMvc.perform(get("/api/workflows/{workflowId}", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Support triage"))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.latestVersion.definition.edges[0].targetNodeId").value("end"));
}
```

Add an archived-state test:

```java
@Test
void rejectsAtomicPublishingForArchivedWorkflow() throws Exception {
    String workflowId = createWorkflow();
    mockMvc.perform(post("/api/workflows/{workflowId}/archive", workflowId))
            .andExpect(status().isOk());

    mockMvc.perform(post("/api/workflows/{workflowId}/publish", workflowId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "name": "Archived",
                              "description": null,
                              "definition": {
                                "nodes": [
                                  { "id": "start", "type": "START", "name": "Start", "config": {} },
                                  { "id": "end", "type": "END", "name": "End", "config": {} }
                                ],
                                "edges": [
                                  { "id": "edge-1", "sourceNodeId": "start", "targetNodeId": "end", "condition": null }
                                ],
                                "variables": []
                              }
                            }
                            """))
            .andExpect(status().isBadRequest());

    mockMvc.perform(get("/api/workflows/{workflowId}", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
}
```

- [ ] **Step 2: Run the backend test and verify RED**

Run:

```powershell
cd D:\openworkspace\aiworkflow\server
mvn -q -Dtest=WorkflowControllerIntegrationTest test
```

Expected: the new test fails because the publish endpoint ignores the request body and returns the old name and definition.

- [ ] **Step 3: Add the request contract and transactional service method**

In `WorkflowController`, add:

```java
public record PublishWorkflowRequest(
        @NotBlank String name,
        String description,
        @NotNull WorkflowDefinition definition
) {
}
```

Change the endpoint to:

```java
@PostMapping("/{workflowId}/publish")
public ApiResponse<WorkflowResponse> publish(
        @PathVariable String workflowId,
        @Valid @RequestBody(required = false) PublishWorkflowRequest request
) {
    WorkflowVersion publishedVersion = request == null
            ? workflowService.publishDraftVersion(workflowId, OperatorContext.currentUserId())
            : workflowService.saveAndPublish(
                    workflowId,
                    request.name(),
                    request.description(),
                    request.definition(),
                    OperatorContext.currentUserId()
            );
    Workflow workflow = workflowService.getWorkflow(workflowId);
    return ApiResponse.success(WorkflowResponse.from(workflow, publishedVersion));
}
```

Import `jakarta.validation.constraints.NotNull` and `WorkflowDefinition`.

In `WorkflowApplicationService`, add:

```java
import org.springframework.transaction.annotation.Transactional;

@Transactional
public synchronized WorkflowVersion saveAndPublish(
        String workflowId,
        String name,
        String description,
        WorkflowDefinition definition,
        String publishedBy
) {
    Workflow workflow = getWorkflow(workflowId);
    if (workflow.status() == WorkflowStatus.ARCHIVED) {
        throw new IllegalStateException("Archived workflow cannot be published.");
    }
    dagValidator.validate(definition);
    updateWorkflowMetadata(workflowId, name, description);
    updateDraftDefinition(workflowId, definition);
    return publishDraftVersion(workflowId, publishedBy);
}
```

The full validation must occur before the first write. Existing synchronized methods remain unchanged.

- [ ] **Step 4: Run backend tests and verify GREEN**

Run:

```powershell
cd D:\openworkspace\aiworkflow\server
mvn -q "-Dtest=WorkflowControllerIntegrationTest,WorkflowApplicationServiceTest" test
```

Expected: all selected tests pass, including body-less legacy publishing.

- [ ] **Step 5: Commit the backend change**

```powershell
git add server/src/main/java/com/mw/ai/agi/workflow/api/WorkflowController.java `
        server/src/main/java/com/mw/ai/agi/workflow/service/WorkflowApplicationService.java `
        server/src/test/java/com/mw/ai/agi/workflow/api/WorkflowControllerIntegrationTest.java
git commit -m "feat: publish workflow snapshots atomically"
```

### Task 2: Frontend Publish Snapshot API

**Files:**
- Modify: `web/apps/admin/src/api/workflows.ts`
- Modify: `web/apps/admin/src/api/workflows.test.ts`

- [ ] **Step 1: Write failing API tests**

Add tests that verify both supported request forms:

```ts
it('publishes a complete designer snapshot when supplied', async () => {
  const definition = {
    nodes: [{ id: 'start', type: 'START', name: 'Start', config: {} }],
    edges: [],
    variables: []
  };

  await publishWorkflow('workflow-1', {
    name: 'Knowledge federation',
    description: 'Two knowledge bases',
    definition
  });

  expect(fetchMock).toHaveBeenCalledWith(
    '/api/workflows/workflow-1/publish',
    expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({
        name: 'Knowledge federation',
        description: 'Two knowledge bases',
        definition
      })
    })
  );
});

it('keeps body-less publishing compatible', async () => {
  await publishWorkflow('workflow-1');

  expect(fetchMock).toHaveBeenCalledWith(
    '/api/workflows/workflow-1/publish',
    expect.objectContaining({ method: 'POST' })
  );
  expect(fetchMock.mock.calls.at(-1)?.[1]).not.toHaveProperty('body');
});
```

- [ ] **Step 2: Run the API tests and verify RED**

Run:

```powershell
cd D:\openworkspace\aiworkflow\web
pnpm --filter @aiworkflow/admin test -- src/api/workflows.test.ts
```

Expected: TypeScript or assertion failure because `publishWorkflow` accepts only the workflow ID and never sends a body.

- [ ] **Step 3: Implement the optional publish payload**

Add:

```ts
export interface PublishWorkflowRequest {
  name: string;
  description: string | null;
  definition: WorkflowDefinition;
}

export async function publishWorkflow(
  workflowId: string,
  request?: PublishWorkflowRequest
): Promise<Workflow> {
  return requestJson<Workflow>(`/api/workflows/${workflowId}/publish`, {
    method: 'POST',
    ...(request ? { body: JSON.stringify(request) } : {})
  });
}
```

- [ ] **Step 4: Run the API tests and verify GREEN**

Run:

```powershell
cd D:\openworkspace\aiworkflow\web
pnpm --filter @aiworkflow/admin test -- src/api/workflows.test.ts
```

Expected: all workflow API tests pass.

- [ ] **Step 5: Commit the API change**

```powershell
git add web/apps/admin/src/api/workflows.ts web/apps/admin/src/api/workflows.test.ts
git commit -m "feat: send workflow snapshot when publishing"
```

### Task 3: Designer Save-and-Publish Interaction

**Files:**
- Modify: `web/apps/admin/src/pages/workflows/WorkflowDesignerPage.tsx`
- Modify: `web/apps/admin/src/pages/workflows/WorkflowDesignerPage.test.tsx`

- [ ] **Step 1: Write the failing designer test**

Add a test that changes the in-memory definition without saving a draft, then publishes:

```ts
it('publishes current metadata and unsaved designer definition in one request', async () => {
  const dataTransfer = createDataTransfer();
  render(
    <QueryClientProvider client={new QueryClient()}>
      <WorkflowDesignerPage workflowId="workflow-1" />
    </QueryClientProvider>
  );

  await screen.findByText('节点编排');
  fireEvent.dragStart(screen.getByRole('button', { name: /Prompt 模板/ }), { dataTransfer });
  const dropTarget = screen.getByLabelText('工作流画布投放区');
  fireEvent.dragOver(dropTarget, { clientX: 420, clientY: 250, dataTransfer });
  const dropEvent = createEvent.drop(dropTarget, { dataTransfer });
  Object.defineProperties(dropEvent, {
    clientX: { value: 420 },
    clientY: { value: 250 },
    pageX: { value: 420 },
    pageY: { value: 250 }
  });
  fireEvent(dropTarget, dropEvent);
  await userEvent.click(screen.getByRole('button', { name: '发布' }));

  expect(workflowApiMock.updateWorkflowDraft).not.toHaveBeenCalled();
  expect(workflowApiMock.publishWorkflow).toHaveBeenCalledWith(
    'workflow-1',
    expect.objectContaining({
      name: expect.any(String),
      description: null,
      definition: expect.objectContaining({
        nodes: expect.arrayContaining([
          expect.objectContaining({ type: 'PROMPT' })
        ])
      })
    })
  );
});
```

- [ ] **Step 2: Run the designer test and verify RED**

Run:

```powershell
cd D:\openworkspace\aiworkflow\web
pnpm --filter @aiworkflow/admin test -- src/pages/workflows/WorkflowDesignerPage.test.tsx
```

Expected: `publishWorkflow` is called with only `workflow-1`.

- [ ] **Step 3: Submit the complete current snapshot**

Import `PublishWorkflowRequest`. Change the mutation to:

```ts
const publishMutation = useMutation({
  mutationFn: (request: PublishWorkflowRequest) => publishWorkflow(workflowId, request),
  onSuccess: async () => {
    message.success('工作流已发布');
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['workflow', workflowId] }),
      queryClient.invalidateQueries({ queryKey: ['workflows'] })
    ]);
  },
  onError: (error) => {
    message.error((error as Error).message || '工作流发布失败');
  }
});
```

Change `handlePublish` to:

```ts
function handlePublish() {
  const { nextDefinition, nextIssues } = validateCurrentDefinition();
  if (nextIssues.length > 0) {
    message.warning('流程结构校验未通过');
    return;
  }
  publishMutation.mutate({
    name: workflowTitle.trim() || '新建工作流',
    description: workflowDescription.trim() || null,
    definition: nextDefinition
  });
}
```

Disable save, publish, and run controls while `publishMutation.isPending` is true. Keep the current editor state untouched in `onError`.

- [ ] **Step 4: Run focused frontend tests and build**

Run:

```powershell
cd D:\openworkspace\aiworkflow\web
pnpm --filter @aiworkflow/admin test -- src/pages/workflows/WorkflowDesignerPage.test.tsx src/api/workflows.test.ts
pnpm --filter @aiworkflow/admin build
```

Expected: focused tests pass and TypeScript/Vite build exits with code 0.

- [ ] **Step 5: Commit the designer change**

```powershell
git add web/apps/admin/src/pages/workflows/WorkflowDesignerPage.tsx `
        web/apps/admin/src/pages/workflows/WorkflowDesignerPage.test.tsx
git commit -m "fix: save workflow editor state before publish"
```

### Task 4: End-to-End Verification

**Files:**
- No source changes expected.

- [ ] **Step 1: Run the complete relevant automated suite**

```powershell
cd D:\openworkspace\aiworkflow\server
mvn -q "-Dtest=WorkflowControllerIntegrationTest,WorkflowApplicationServiceTest,WorkflowRunControllerIntegrationTest" test

cd D:\openworkspace\aiworkflow\web
pnpm --filter @aiworkflow/admin test -- src/api/workflows.test.ts src/pages/workflows/WorkflowDesignerPage.test.tsx
pnpm --filter @aiworkflow/admin build
```

Expected: every command exits with code 0.

- [ ] **Step 2: Package and restart the backend**

Stop only the process listening on port 8080, package the server, and start it with JDK 17:

```powershell
$connection = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
  Select-Object -First 1
if ($connection) {
  Stop-Process -Id $connection.OwningProcess -Force
}

cd D:\openworkspace\aiworkflow\server
mvn -q -DskipTests package
Start-Process -FilePath 'D:\devtools\Zulu\zulu-17\bin\java.exe' `
  -ArgumentList '-jar', 'D:\openworkspace\aiworkflow\server\target\aiworkflow-server-0.1.0-SNAPSHOT.jar' `
  -WorkingDirectory 'D:\openworkspace\aiworkflow\server' `
  -WindowStyle Hidden
```

Expected: port 8080 listens under the new Java process.

- [ ] **Step 3: Verify the user acceptance scenario**

In the “知识库测试” designer:

1. Select both “档案标准规范” and “企业规章制度”.
2. Do not click “保存草稿”.
3. Click “发布”.
4. Read the latest published workflow through `GET /api/workflows/{id}`.
5. Assert the latest published knowledge node contains both knowledge-base IDs.
6. Run the workflow and assert `START`, `KNOWLEDGE_RETRIEVAL`, `LLM`, and `END` all have status `SUCCEEDED`.

Expected: the published version contains both IDs and the full workflow execution succeeds.

- [ ] **Step 4: Review the final diff**

```powershell
cd D:\openworkspace\aiworkflow
git diff --check
git status --short
```

Expected: no whitespace errors; unrelated pre-existing changes remain untouched.
