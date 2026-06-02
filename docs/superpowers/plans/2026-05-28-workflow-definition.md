# Workflow Definition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add backend workflow definition capabilities: DAG schema objects, validation, in-memory workflow/version management, and REST APIs for create/list/get/update/publish.

**Architecture:** Keep this stage independent of PostgreSQL runtime by using an in-memory repository behind a service boundary. The domain model and validator are persistence-neutral so the next persistence stage can swap storage without changing API behavior.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Jackson, Jakarta Validation, JUnit 5, MockMvc.

---

## Scope Notes

This stage implements workflow definition behavior only. It does not execute workflows, call AI models, store data in PostgreSQL, or render the visual designer page. The workflow definition JSON shape must stay aligned with `web/packages/workflow-schema`.

## File Structure

```text
server/src/main/java/com/aiworkflow/workflow/domain
├── WorkflowStatus.java
├── WorkflowVersionStatus.java
├── WorkflowNodeType.java
├── WorkflowVariableType.java
├── WorkflowDefinition.java
├── WorkflowNode.java
├── WorkflowEdge.java
├── WorkflowVariable.java
├── Workflow.java
└── WorkflowVersion.java

server/src/main/java/com/aiworkflow/workflow/service
├── DagValidationException.java
├── DagValidator.java
├── WorkflowNotFoundException.java
├── WorkflowStore.java
├── InMemoryWorkflowStore.java
└── WorkflowApplicationService.java

server/src/main/java/com/aiworkflow/workflow/api
├── WorkflowController.java
├── CreateWorkflowRequest.java
├── UpdateWorkflowDraftRequest.java
├── WorkflowResponse.java
├── WorkflowVersionResponse.java
└── WorkflowDefinitionResponse.java
```

## Task 1: DAG Domain Model and Validator

**Files:**
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/WorkflowNodeType.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/WorkflowVariableType.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/WorkflowDefinition.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/WorkflowNode.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/WorkflowEdge.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/WorkflowVariable.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/service/DagValidationException.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/service/DagValidator.java`
- Test: `server/src/test/java/com/aiworkflow/workflow/service/DagValidatorTest.java`

- [ ] **Step 1: Write failing validator tests**

Create `DagValidatorTest` with tests for:

```java
@Test
void acceptsSingleStartAndEndConnectedDag()

@Test
void rejectsDefinitionWithoutStartNode()

@Test
void rejectsDefinitionWithCycle()

@Test
void rejectsEdgeWithMissingTargetNode()
```

The valid workflow should contain `START -> TEXT_TRANSFORM -> END`.

- [ ] **Step 2: Run red test**

Run:

```bash
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=DagValidatorTest test
```

Expected: FAIL because domain and validator classes do not exist.

- [ ] **Step 3: Implement domain records and enums**

Create enums:

```java
public enum WorkflowNodeType {
    START, END, LLM, PROMPT, KNOWLEDGE_RETRIEVAL, HTTP_TOOL, CONDITION, TEXT_TRANSFORM
}

public enum WorkflowVariableType {
    STRING, NUMBER, BOOLEAN, OBJECT, ARRAY
}
```

Create records:

```java
public record WorkflowNode(String id, WorkflowNodeType type, String name, Map<String, Object> config) {}
public record WorkflowEdge(String id, String sourceNodeId, String targetNodeId, String condition) {}
public record WorkflowVariable(String name, WorkflowVariableType type, boolean required) {}
public record WorkflowDefinition(List<WorkflowNode> nodes, List<WorkflowEdge> edges, List<WorkflowVariable> variables) {}
```

- [ ] **Step 4: Implement DAG validator**

`DagValidator.validate(WorkflowDefinition definition)` must:

- require non-null definition
- require exactly one `START` node
- require at least one `END` node
- require every edge source and target to exist
- reject directed cycles

Throw `DagValidationException` with stable messages:

- `Workflow definition is required.`
- `Workflow definition must contain exactly one START node.`
- `Workflow definition must contain at least one END node.`
- `Workflow edge references missing node.`
- `Workflow definition must be acyclic.`

- [ ] **Step 5: Run green test**

Run:

```bash
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=DagValidatorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add server/src/main/java/com/aiworkflow/workflow/domain server/src/main/java/com/aiworkflow/workflow/service server/src/test/java/com/aiworkflow/workflow/service/DagValidatorTest.java
git commit -m "feat: add workflow DAG validation"
```

## Task 2: Workflow Store and Application Service

**Files:**
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/WorkflowStatus.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/WorkflowVersionStatus.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/Workflow.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/domain/WorkflowVersion.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/service/WorkflowNotFoundException.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/service/WorkflowStore.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/service/InMemoryWorkflowStore.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/service/WorkflowApplicationService.java`
- Test: `server/src/test/java/com/aiworkflow/workflow/service/WorkflowApplicationServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Create tests for:

```java
@Test
void createsWorkflowWithDraftVersion()

@Test
void updatesDraftDefinitionAfterValidation()

@Test
void publishesDraftAsImmutableVersion()

@Test
void throwsWhenWorkflowDoesNotExist()
```

- [ ] **Step 2: Run red test**

```bash
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=WorkflowApplicationServiceTest test
```

Expected: FAIL because service/store classes do not exist.

- [ ] **Step 3: Implement workflow domain**

Create statuses:

```java
public enum WorkflowStatus { DRAFT, PUBLISHED, ARCHIVED }
public enum WorkflowVersionStatus { DRAFT, PUBLISHED, DISABLED }
```

`Workflow` should include id, tenantId, name, description, status, currentVersionId, createdBy, createdAt, updatedAt.

`WorkflowVersion` should include id, workflowId, version, definition, status, publishedBy, publishedAt, createdAt.

- [ ] **Step 4: Implement in-memory store**

`InMemoryWorkflowStore` should use `ConcurrentHashMap` and expose save/find/list methods through `WorkflowStore`.

- [ ] **Step 5: Implement service**

`WorkflowApplicationService` should:

- create a workflow with one draft version `1`
- update draft definition after `DagValidator.validate`
- publish the draft version, set workflow status `PUBLISHED`, and set `currentVersionId`
- list workflows
- get workflow by id or throw `WorkflowNotFoundException`

- [ ] **Step 6: Run green test**

```bash
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=WorkflowApplicationServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add server/src/main/java/com/aiworkflow/workflow/domain server/src/main/java/com/aiworkflow/workflow/service server/src/test/java/com/aiworkflow/workflow/service/WorkflowApplicationServiceTest.java
git commit -m "feat: add workflow definition service"
```

## Task 3: Workflow REST API

**Files:**
- Modify: `server/src/main/java/com/aiworkflow/workflow/api/WorkflowController.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/CreateWorkflowRequest.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/UpdateWorkflowDraftRequest.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/WorkflowResponse.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/WorkflowVersionResponse.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/WorkflowDefinitionResponse.java`
- Test: `server/src/test/java/com/aiworkflow/workflow/api/WorkflowControllerIntegrationTest.java`

- [ ] **Step 1: Write failing API tests**

Create MockMvc integration tests for:

```java
@Test
void createsAndFetchesWorkflow()

@Test
void rejectsInvalidDagOnUpdate()

@Test
void publishesWorkflowDraft()
```

- [ ] **Step 2: Run red test**

```bash
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=WorkflowControllerIntegrationTest test
```

Expected: FAIL because API behavior is not implemented.

- [ ] **Step 3: Implement request/response DTOs**

Create DTOs with validation:

```java
public record CreateWorkflowRequest(@NotBlank String name, String description, @NotNull WorkflowDefinition definition) {}
public record UpdateWorkflowDraftRequest(@NotNull WorkflowDefinition definition) {}
```

Responses should include workflow metadata and latest draft/published version data.

- [ ] **Step 4: Update controller**

Endpoints:

- `GET /api/workflows`
- `POST /api/workflows`
- `GET /api/workflows/{workflowId}`
- `PUT /api/workflows/{workflowId}/draft`
- `POST /api/workflows/{workflowId}/publish`

Use `ApiResponse.success(...)`.

- [ ] **Step 5: Map domain errors**

Extend `GlobalExceptionHandler`:

- `DagValidationException` -> HTTP 400, code `INVALID_WORKFLOW_DAG`
- `WorkflowNotFoundException` -> HTTP 404, code `WORKFLOW_NOT_FOUND`

- [ ] **Step 6: Run green tests**

```bash
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=WorkflowControllerIntegrationTest test
```

Expected: PASS.

- [ ] **Step 7: Run full backend test**

```bash
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add server/src/main/java/com/aiworkflow/workflow/api server/src/main/java/com/aiworkflow/common/exception server/src/test/java/com/aiworkflow/workflow/api
git commit -m "feat: expose workflow definition API"
```

