# Workflow Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight synchronous DAG execution engine for published workflow versions, including execution records, node records, context passing, START/END/TEXT_TRANSFORM/CONDITION executors, and run/query REST APIs.

**Architecture:** Keep the first engine version small and deterministic. The engine reads the immutable published version from the workflow definition service, executes nodes in dependency order from START, applies simple branch conditions, stores execution records in memory, and exposes APIs for third-party callers and the future React/Vue embedded designer.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Jackson, JUnit 5, AssertJ, MockMvc.

---

## Scope Notes

This stage implements synchronous execution only. It does not add RabbitMQ, async workers, parallel scheduling, LLM calls, HTTP tool calls, persistence, cancellation, or real-time push. The first supported executable node set is `START`, `END`, `TEXT_TRANSFORM`, and `CONDITION`.

## File Structure

```text
server/src/main/java/com/aiworkflow/workflow/engine
|-- WorkflowExecutionStatus.java
|-- NodeExecutionStatus.java
|-- WorkflowExecution.java
|-- NodeExecution.java
|-- WorkflowExecutionRequest.java
|-- WorkflowExecutionResult.java
|-- WorkflowExecutionStore.java
|-- InMemoryWorkflowExecutionStore.java
|-- NodeExecutionContext.java
|-- NodeExecutionResult.java
|-- WorkflowNodeExecutor.java
|-- WorkflowNodeExecutorRegistry.java
|-- StartNodeExecutor.java
|-- EndNodeExecutor.java
|-- TextTransformNodeExecutor.java
|-- ConditionNodeExecutor.java
|-- WorkflowExecutionService.java

server/src/main/java/com/aiworkflow/workflow/api
|-- RunWorkflowRequest.java
|-- WorkflowExecutionResponse.java
|-- NodeExecutionResponse.java
|-- WorkflowRunController.java
```

## Task 1: Execution Domain and Store

**Files:**
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/WorkflowExecutionStatus.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/NodeExecutionStatus.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/WorkflowExecution.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/NodeExecution.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/WorkflowExecutionStore.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/InMemoryWorkflowExecutionStore.java`
- Test: `server/src/test/java/com/aiworkflow/workflow/engine/InMemoryWorkflowExecutionStoreTest.java`

- [ ] **Step 1: Write failing store tests**

Create tests:

```java
@Test
void savesAndFindsWorkflowExecution()

@Test
void savesAndListsNodeExecutionsInCreatedOrder()

@Test
void returnsImmutableSnapshots()
```

- [ ] **Step 2: Run red test**

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=InMemoryWorkflowExecutionStoreTest test
```

Expected: FAIL because execution domain and store classes do not exist.

- [ ] **Step 3: Implement execution records and store**

Statuses:

```java
public enum WorkflowExecutionStatus { RUNNING, SUCCEEDED, FAILED }
public enum NodeExecutionStatus { RUNNING, SUCCEEDED, FAILED, SKIPPED }
```

Records:

```java
public record WorkflowExecution(
        String id,
        String workflowId,
        String workflowVersionId,
        WorkflowExecutionStatus status,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {}

public record NodeExecution(
        String id,
        String workflowExecutionId,
        String nodeId,
        WorkflowNodeType nodeType,
        NodeExecutionStatus status,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {}
```

`InMemoryWorkflowExecutionStore` should save workflow executions by id, save node executions by execution id, return `Optional<WorkflowExecution>`, and return immutable node execution lists.

- [ ] **Step 4: Run green test**

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=InMemoryWorkflowExecutionStoreTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add server/src/main/java/com/aiworkflow/workflow/engine server/src/test/java/com/aiworkflow/workflow/engine/InMemoryWorkflowExecutionStoreTest.java
git commit -m "feat: add workflow execution records"
```

## Task 2: Node Executor Registry and Built-in Executors

**Files:**
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/NodeExecutionContext.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/NodeExecutionResult.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/WorkflowNodeExecutor.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/WorkflowNodeExecutorRegistry.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/StartNodeExecutor.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/EndNodeExecutor.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/TextTransformNodeExecutor.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/ConditionNodeExecutor.java`
- Test: `server/src/test/java/com/aiworkflow/workflow/engine/BuiltinNodeExecutorTest.java`

- [ ] **Step 1: Write failing executor tests**

Create tests:

```java
@Test
void startNodePassesInputThrough()

@Test
void endNodeReturnsConfiguredOutputKeys()

@Test
void textTransformNodeRendersTemplateFromContext()

@Test
void conditionNodeSelectsMatchedBranch()
```

`TEXT_TRANSFORM` uses config `{ "outputKey": "message", "template": "Hello {{name}}" }`.

`CONDITION` uses config `{ "contextKey": "tier", "equals": "vip", "trueTargetNodeId": "vip", "falseTargetNodeId": "normal" }`.

- [ ] **Step 2: Run red test**

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=BuiltinNodeExecutorTest test
```

Expected: FAIL because executor classes do not exist.

- [ ] **Step 3: Implement executor contract**

`WorkflowNodeExecutor` exposes:

```java
WorkflowNodeType nodeType();
NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context);
```

`NodeExecutionResult` includes output updates and an optional `nextNodeId` override for condition branches.

- [ ] **Step 4: Implement built-in executors**

Behavior:

- `START`: returns current input/context unchanged.
- `END`: if `outputKeys` is configured as a list, returns only those keys from context; otherwise returns full context.
- `TEXT_TRANSFORM`: replaces `{{key}}` tokens with context values and writes the rendered text to `outputKey`.
- `CONDITION`: compares `contextKey` value to `equals` as strings and returns `trueTargetNodeId` or `falseTargetNodeId`.

- [ ] **Step 5: Run green test**

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=BuiltinNodeExecutorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add server/src/main/java/com/aiworkflow/workflow/engine server/src/test/java/com/aiworkflow/workflow/engine/BuiltinNodeExecutorTest.java
git commit -m "feat: add built-in workflow node executors"
```

## Task 3: Synchronous Workflow Execution Service

**Files:**
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/WorkflowExecutionRequest.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/WorkflowExecutionResult.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/WorkflowExecutionService.java`
- Modify: `server/src/main/java/com/aiworkflow/workflow/service/WorkflowApplicationService.java`
- Test: `server/src/test/java/com/aiworkflow/workflow/engine/WorkflowExecutionServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Create tests:

```java
@Test
void runsPublishedWorkflowFromStartToEnd()

@Test
void failsWhenWorkflowHasNoPublishedVersion()

@Test
void conditionNodeChoosesMatchingBranch()

@Test
void recordsFailedNodeAndWorkflowWhenExecutorFails()
```

- [ ] **Step 2: Run red test**

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=WorkflowExecutionServiceTest test
```

Expected: FAIL because execution service classes do not exist.

- [ ] **Step 3: Expose published version lookup**

Add to `WorkflowApplicationService`:

```java
public WorkflowVersion getPublishedVersion(String workflowId) {
    Workflow workflow = getWorkflow(workflowId);
    if (workflow.currentVersionId() == null) {
        throw new WorkflowNotFoundException("Published version not found for workflow: " + workflowId);
    }
    return store.listVersions(workflowId).stream()
            .filter(version -> workflow.currentVersionId().equals(version.id()))
            .findFirst()
            .orElseThrow(() -> new WorkflowNotFoundException("Published version not found for workflow: " + workflowId));
}
```

- [ ] **Step 4: Implement execution service**

Execution behavior:

- Create a `RUNNING` workflow execution before node execution.
- Start from the single START node.
- For normal nodes, pick the first outgoing edge after the node succeeds.
- For CONDITION, use `NodeExecutionResult.nextNodeId()` to pick the next node.
- Merge node output into the mutable execution context after each success.
- Create one `NodeExecution` per executed node.
- Mark workflow `SUCCEEDED` with END output when END succeeds.
- Mark workflow `FAILED` when any executor throws.

- [ ] **Step 5: Run green test**

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=WorkflowExecutionServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add server/src/main/java/com/aiworkflow/workflow/engine server/src/main/java/com/aiworkflow/workflow/service/WorkflowApplicationService.java server/src/test/java/com/aiworkflow/workflow/engine/WorkflowExecutionServiceTest.java
git commit -m "feat: add synchronous workflow execution service"
```

## Task 4: Workflow Run REST API

**Files:**
- Create: `server/src/main/java/com/aiworkflow/workflow/api/RunWorkflowRequest.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/WorkflowExecutionResponse.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/NodeExecutionResponse.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/WorkflowRunController.java`
- Test: `server/src/test/java/com/aiworkflow/workflow/api/WorkflowRunControllerIntegrationTest.java`

- [ ] **Step 1: Write failing API tests**

Create MockMvc integration tests:

```java
@Test
void runsPublishedWorkflow()

@Test
void getsWorkflowExecutionDetail()

@Test
void returnsNotFoundForMissingExecution()
```

- [ ] **Step 2: Run red test**

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=WorkflowRunControllerIntegrationTest test
```

Expected: FAIL because run API classes do not exist.

- [ ] **Step 3: Implement run request and responses**

`RunWorkflowRequest`:

```java
public record RunWorkflowRequest(Map<String, Object> input) {}
```

Responses expose workflow execution metadata, input, output, error message, timestamps, and node execution details.

- [ ] **Step 4: Implement run controller**

Endpoints:

- `POST /api/workflows/{workflowId}/runs`
- `GET /api/workflow-runs/{executionId}`

Both return `ApiResponse.success(...)`.

- [ ] **Step 5: Run green API test**

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=WorkflowRunControllerIntegrationTest test
```

Expected: PASS.

- [ ] **Step 6: Run full backend test**

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add server/src/main/java/com/aiworkflow/workflow/api server/src/main/java/com/aiworkflow/workflow/engine server/src/test/java/com/aiworkflow/workflow/api/WorkflowRunControllerIntegrationTest.java
git commit -m "feat: expose workflow run API"
```

## Final Verification

Run:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml test
```

Run:

```powershell
corepack pnpm build
```

Expected:

- Backend tests pass.
- Frontend workspace builds.
- `git status --short` is clean.

