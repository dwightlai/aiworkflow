# AI Workflow Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first foundation slice for the enterprise AI workflow platform: Spring Boot backend scaffold, React management frontend scaffold, framework-agnostic workflow designer package structure, database migration baseline, and third-party integration API skeleton.

**Architecture:** Use a modular monolith backend in `server/` and a frontend workspace in `web/`. Keep the workflow designer independent from React and Vue by placing schema, designer core, React adapter, Vue adapter, Web Component adapter, and SDK packages under `web/packages/`.

**Tech Stack:** Java 21, Spring Boot 3.x, Maven, PostgreSQL, Flyway, springdoc-openapi, React, TypeScript, Ant Design React, Vite, pnpm workspace, LogicFlow, Vitest.

---

## Scope Notes

The approved design covers several independent subsystems. This plan implements only the first foundation slice. Later plans should cover workflow engine, AI model calls, RAG, HTTP tools, monitoring, and production deployment.

This workspace is not currently a Git repository, so commit steps are written for future use but will fail until `git init` is run.

## File Structure

Create this structure:

```text
server
├── pom.xml
└── src
    ├── main
    │   ├── java/com/aiworkflow
    │   │   ├── AiWorkflowApplication.java
    │   │   ├── common/api/ApiResponse.java
    │   │   ├── common/api/ErrorResponse.java
    │   │   ├── common/exception/GlobalExceptionHandler.java
    │   │   ├── config/OpenApiConfig.java
    │   │   ├── workflow/api/WorkflowController.java
    │   │   ├── workflow/api/OpenWorkflowRunController.java
    │   │   └── integration/api/IntegrationAppController.java
    │   └── resources
    │       ├── application.yml
    │       └── db/migration/V1__foundation_schema.sql
    └── test/java/com/aiworkflow
        ├── common/api/ApiResponseTest.java
        └── workflow/api/WorkflowControllerTest.java
web
├── package.json
├── pnpm-workspace.yaml
├── tsconfig.base.json
├── apps/admin
│   ├── package.json
│   ├── index.html
│   ├── vite.config.ts
│   └── src
│       ├── App.tsx
│       ├── main.tsx
│       └── pages/WorkflowListPage.tsx
└── packages
    ├── workflow-schema
    │   ├── package.json
    │   └── src/index.ts
    ├── workflow-designer-core
    │   ├── package.json
    │   └── src/index.ts
    ├── workflow-designer-react
    │   ├── package.json
    │   └── src/index.tsx
    ├── workflow-designer-vue
    │   ├── package.json
    │   └── src/index.ts
    ├── workflow-designer-wc
    │   ├── package.json
    │   └── src/index.ts
    └── workflow-sdk
        ├── package.json
        └── src/index.ts
docker-compose.yml
```

## Task 1: Backend Maven Scaffold

**Files:**
- Create: `server/pom.xml`
- Create: `server/src/main/java/com/aiworkflow/AiWorkflowApplication.java`
- Create: `server/src/main/resources/application.yml`
- Test by running: `mvn -f server/pom.xml test`

- [ ] **Step 1: Create the Maven project file**

Create `server/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.aiworkflow</groupId>
    <artifactId>aiworkflow-server</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>aiworkflow-server</name>

    <properties>
        <java.version>21</java.version>
        <springdoc.version>2.6.0</springdoc.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create the Spring Boot entry point**

Create `server/src/main/java/com/aiworkflow/AiWorkflowApplication.java`:

```java
package com.aiworkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiWorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiWorkflowApplication.class, args);
    }
}
```

- [ ] **Step 3: Create local configuration**

Create `server/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: aiworkflow-server
  datasource:
    url: jdbc:postgresql://localhost:5432/aiworkflow
    username: aiworkflow
    password: aiworkflow
  flyway:
    enabled: true
    locations: classpath:db/migration

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 4: Run backend tests**

Run:

```bash
mvn -f server/pom.xml test
```

Expected: build succeeds.

## Task 2: Common API Envelope and Error Handling

**Files:**
- Create: `server/src/main/java/com/aiworkflow/common/api/ApiResponse.java`
- Create: `server/src/main/java/com/aiworkflow/common/api/ErrorResponse.java`
- Create: `server/src/main/java/com/aiworkflow/common/exception/GlobalExceptionHandler.java`
- Create: `server/src/test/java/com/aiworkflow/common/api/ApiResponseTest.java`

- [ ] **Step 1: Write the response envelope test**

Create `server/src/test/java/com/aiworkflow/common/api/ApiResponseTest.java`:

```java
package com.aiworkflow.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    @Test
    void successWrapsData() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.error()).isNull();
    }

    @Test
    void failureWrapsError() {
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", "Invalid input", "req-1", null);
        ApiResponse<Void> response = ApiResponse.failure(error);

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isEqualTo(error);
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
mvn -f server/pom.xml -Dtest=ApiResponseTest test
```

Expected: FAIL because `ApiResponse` and `ErrorResponse` do not exist.

- [ ] **Step 3: Implement API response records**

Create `server/src/main/java/com/aiworkflow/common/api/ErrorResponse.java`:

```java
package com.aiworkflow.common.api;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        String requestId,
        Map<String, Object> details
) {
}
```

Create `server/src/main/java/com/aiworkflow/common/api/ApiResponse.java`:

```java
package com.aiworkflow.common.api;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> failure(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}
```

- [ ] **Step 4: Add global exception handler**

Create `server/src/main/java/com/aiworkflow/common/exception/GlobalExceptionHandler.java`:

```java
package com.aiworkflow.common.exception;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed.",
                request.getRequestId(),
                Map.of("fieldErrors", exception.getBindingResult().getFieldErrorCount())
        );
        return ApiResponse.failure(error);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception exception, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "Unexpected server error.",
                request.getRequestId(),
                null
        );
        return ApiResponse.failure(error);
    }
}
```

- [ ] **Step 5: Run the test**

Run:

```bash
mvn -f server/pom.xml -Dtest=ApiResponseTest test
```

Expected: PASS.

## Task 3: OpenAPI Configuration and Skeleton Controllers

**Files:**
- Create: `server/src/main/java/com/aiworkflow/config/OpenApiConfig.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/WorkflowController.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/api/OpenWorkflowRunController.java`
- Create: `server/src/main/java/com/aiworkflow/integration/api/IntegrationAppController.java`
- Create: `server/src/test/java/com/aiworkflow/workflow/api/WorkflowControllerTest.java`

- [ ] **Step 1: Write controller smoke test**

Create `server/src/test/java/com/aiworkflow/workflow/api/WorkflowControllerTest.java`:

```java
package com.aiworkflow.workflow.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkflowControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listWorkflowsReturnsEmptyPageEnvelope() throws Exception {
        mockMvc.perform(get("/api/workflows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
mvn -f server/pom.xml -Dtest=WorkflowControllerTest test
```

Expected: FAIL because `WorkflowController` does not exist.

- [ ] **Step 3: Add OpenAPI metadata**

Create `server/src/main/java/com/aiworkflow/config/OpenApiConfig.java`:

```java
package com.aiworkflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI aiWorkflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Workflow Platform API")
                        .version("v1")
                        .description("Management and third-party APIs for the AI workflow platform."));
    }
}
```

- [ ] **Step 4: Add management workflow controller**

Create `server/src/main/java/com/aiworkflow/workflow/api/WorkflowController.java`:

```java
package com.aiworkflow.workflow.api;

import com.aiworkflow.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    @GetMapping
    public ApiResponse<PageResponse<WorkflowSummaryResponse>> list() {
        return ApiResponse.success(new PageResponse<>(List.of(), 0));
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record WorkflowSummaryResponse(String id, String name, String status) {
    }
}
```

- [ ] **Step 5: Add third-party workflow run controller**

Create `server/src/main/java/com/aiworkflow/workflow/api/OpenWorkflowRunController.java`:

```java
package com.aiworkflow.workflow.api;

import com.aiworkflow.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/openapi/v1")
public class OpenWorkflowRunController {
    @PostMapping("/workflows/{workflowId}/runs")
    public ApiResponse<Map<String, String>> createRun(@PathVariable String workflowId) {
        return ApiResponse.success(Map.of("workflowId", workflowId, "runId", "run_" + UUID.randomUUID()));
    }

    @GetMapping("/workflow-runs/{runId}")
    public ApiResponse<Map<String, String>> getRun(@PathVariable String runId) {
        return ApiResponse.success(Map.of("runId", runId, "status", "PENDING"));
    }
}
```

- [ ] **Step 6: Add integration app controller**

Create `server/src/main/java/com/aiworkflow/integration/api/IntegrationAppController.java`:

```java
package com.aiworkflow.integration.api;

import com.aiworkflow.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integration-apps")
public class IntegrationAppController {
    @GetMapping
    public ApiResponse<List<IntegrationAppResponse>> list() {
        return ApiResponse.success(List.of());
    }

    public record IntegrationAppResponse(String id, String name, String status) {
    }
}
```

- [ ] **Step 7: Run controller test**

Run:

```bash
mvn -f server/pom.xml -Dtest=WorkflowControllerTest test
```

Expected: PASS because the Web MVC test disables security filters.

## Task 4: Database Migration Baseline

**Files:**
- Create: `server/src/main/resources/db/migration/V1__foundation_schema.sql`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Create foundation schema migration**

Create `server/src/main/resources/db/migration/V1__foundation_schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS workflow (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    current_version_id VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS workflow_version (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL REFERENCES workflow(id),
    version INTEGER NOT NULL,
    definition_json JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    published_by VARCHAR(64),
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (workflow_id, version)
);

CREATE TABLE IF NOT EXISTS workflow_execution (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    workflow_version_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json JSONB NOT NULL,
    context_json JSONB NOT NULL,
    output_json JSONB,
    error_code VARCHAR(100),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS workflow_node_execution (
    id VARCHAR(64) PRIMARY KEY,
    workflow_execution_id VARCHAR(64) NOT NULL REFERENCES workflow_execution(id),
    node_id VARCHAR(100) NOT NULL,
    node_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json JSONB,
    output_json JSONB,
    error_code VARCHAR(100),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS integration_app (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS webhook_subscription (
    id VARCHAR(64) PRIMARY KEY,
    integration_app_id VARCHAR(64) NOT NULL REFERENCES integration_app(id),
    event_type VARCHAR(100) NOT NULL,
    target_url TEXT NOT NULL,
    secret_ref VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_workflow_tenant_status ON workflow(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_workflow_execution_workflow ON workflow_execution(workflow_id, created_at);
CREATE INDEX IF NOT EXISTS idx_node_execution_run ON workflow_node_execution(workflow_execution_id);
```

- [ ] **Step 2: Create Docker Compose for PostgreSQL**

Create `docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16
    container_name: aiworkflow-postgres
    environment:
      POSTGRES_DB: aiworkflow
      POSTGRES_USER: aiworkflow
      POSTGRES_PASSWORD: aiworkflow
    ports:
      - "5432:5432"
    volumes:
      - aiworkflow-postgres-data:/var/lib/postgresql/data

volumes:
  aiworkflow-postgres-data:
```

- [ ] **Step 3: Run database and application**

Run:

```bash
docker compose up -d postgres
mvn -f server/pom.xml spring-boot:run
```

Expected: application starts and Flyway applies `V1__foundation_schema.sql`.

## Task 5: Frontend Workspace Scaffold

**Files:**
- Create: `web/package.json`
- Create: `web/pnpm-workspace.yaml`
- Create: `web/tsconfig.base.json`
- Create: `web/apps/admin/package.json`
- Create: `web/apps/admin/index.html`
- Create: `web/apps/admin/vite.config.ts`
- Create: `web/apps/admin/src/main.tsx`
- Create: `web/apps/admin/src/App.tsx`
- Create: `web/apps/admin/src/pages/WorkflowListPage.tsx`

- [ ] **Step 1: Create workspace package files**

Create `web/package.json`:

```json
{
  "name": "@aiworkflow/web-workspace",
  "private": true,
  "scripts": {
    "dev": "pnpm --filter @aiworkflow/admin dev",
    "build": "pnpm -r build",
    "test": "pnpm -r test"
  },
  "devDependencies": {
    "@types/node": "^22.9.0",
    "typescript": "^5.6.3"
  }
}
```

Create `web/pnpm-workspace.yaml`:

```yaml
packages:
  - "apps/*"
  - "packages/*"
```

Create `web/tsconfig.base.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "strict": true,
    "jsx": "react-jsx",
    "skipLibCheck": true,
    "declaration": true,
    "sourceMap": true
  }
}
```

- [ ] **Step 2: Create admin app package**

Create `web/apps/admin/package.json`:

```json
{
  "name": "@aiworkflow/admin",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0 --port 5173",
    "build": "tsc --noEmit && vite build",
    "test": "vitest run"
  },
  "dependencies": {
    "@ant-design/pro-components": "^2.8.0",
    "@aiworkflow/workflow-designer-react": "workspace:*",
    "@tanstack/react-query": "^5.59.16",
    "antd": "^5.21.6",
    "echarts": "^5.5.1",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "zustand": "^5.0.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.3.3",
    "typescript": "^5.6.3",
    "vite": "^5.4.10",
    "vitest": "^2.1.4"
  }
}
```

- [ ] **Step 3: Create admin app entry files**

Create `web/apps/admin/index.html`:

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>AI Workflow</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

Create `web/apps/admin/vite.config.ts`:

```ts
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/openapi': 'http://localhost:8080'
    }
  }
});
```

Create `web/apps/admin/src/main.tsx`:

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

Create `web/apps/admin/src/App.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider, Layout } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { WorkflowListPage } from './pages/WorkflowListPage';

const queryClient = new QueryClient();

export function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={queryClient}>
        <Layout style={{ minHeight: '100vh' }}>
          <Layout.Header style={{ color: '#fff', fontWeight: 600 }}>
            AI Workflow
          </Layout.Header>
          <Layout.Content style={{ padding: 24 }}>
            <WorkflowListPage />
          </Layout.Content>
        </Layout>
      </QueryClientProvider>
    </ConfigProvider>
  );
}
```

Create `web/apps/admin/src/pages/WorkflowListPage.tsx`:

```tsx
import { Button, Card, Space, Table, Typography } from 'antd';

export function WorkflowListPage() {
  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space style={{ justifyContent: 'space-between', width: '100%' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          工作流
        </Typography.Title>
        <Button type="primary">新建工作流</Button>
      </Space>
      <Card>
        <Table
          rowKey="id"
          pagination={false}
          dataSource={[]}
          columns={[
            { title: '名称', dataIndex: 'name' },
            { title: '状态', dataIndex: 'status' },
            { title: '更新时间', dataIndex: 'updatedAt' }
          ]}
        />
      </Card>
    </Space>
  );
}
```

- [ ] **Step 4: Install and build frontend**

Run:

```bash
cd web
pnpm install
pnpm build
```

Expected: dependencies install and admin app builds.

## Task 6: Framework-Agnostic Designer Packages

**Files:**
- Create: `web/packages/workflow-schema/package.json`
- Create: `web/packages/workflow-schema/src/index.ts`
- Create: `web/packages/workflow-designer-core/package.json`
- Create: `web/packages/workflow-designer-core/src/index.ts`
- Create: `web/packages/workflow-designer-react/package.json`
- Create: `web/packages/workflow-designer-react/src/index.tsx`
- Create: `web/packages/workflow-designer-vue/package.json`
- Create: `web/packages/workflow-designer-vue/src/index.ts`
- Create: `web/packages/workflow-designer-wc/package.json`
- Create: `web/packages/workflow-designer-wc/src/index.ts`
- Create: `web/packages/workflow-sdk/package.json`
- Create: `web/packages/workflow-sdk/src/index.ts`

- [ ] **Step 1: Create workflow schema package**

Create `web/packages/workflow-schema/package.json`:

```json
{
  "name": "@aiworkflow/workflow-schema",
  "version": "0.1.0",
  "type": "module",
  "main": "src/index.ts",
  "types": "src/index.ts",
  "scripts": {
    "build": "tsc --noEmit",
    "test": "vitest run"
  },
  "devDependencies": {
    "typescript": "^5.6.3",
    "vitest": "^2.1.4"
  }
}
```

Create `web/packages/workflow-schema/src/index.ts`:

```ts
export type WorkflowNodeType =
  | 'START'
  | 'END'
  | 'LLM'
  | 'PROMPT'
  | 'KNOWLEDGE_RETRIEVAL'
  | 'HTTP_TOOL'
  | 'CONDITION'
  | 'TEXT_TRANSFORM';

export interface WorkflowNode {
  id: string;
  type: WorkflowNodeType;
  name: string;
  config: Record<string, unknown>;
}

export interface WorkflowEdge {
  id: string;
  sourceNodeId: string;
  targetNodeId: string;
  condition?: string | null;
}

export interface WorkflowVariable {
  name: string;
  type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'OBJECT' | 'ARRAY';
  required: boolean;
}

export interface WorkflowDefinition {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  variables: WorkflowVariable[];
}

export function createEmptyWorkflowDefinition(): WorkflowDefinition {
  return {
    nodes: [{ id: 'start_1', type: 'START', name: '开始', config: {} }],
    edges: [],
    variables: []
  };
}
```

- [ ] **Step 2: Create designer core package**

Create `web/packages/workflow-designer-core/package.json`:

```json
{
  "name": "@aiworkflow/workflow-designer-core",
  "version": "0.1.0",
  "type": "module",
  "main": "src/index.ts",
  "types": "src/index.ts",
  "scripts": {
    "build": "tsc --noEmit",
    "test": "vitest run"
  },
  "dependencies": {
    "@aiworkflow/workflow-schema": "workspace:*",
    "@logicflow/core": "^2.0.0"
  },
  "devDependencies": {
    "typescript": "^5.6.3",
    "vitest": "^2.1.4"
  }
}
```

Create `web/packages/workflow-designer-core/src/index.ts`:

```ts
import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';

export interface WorkflowDesignerCoreOptions {
  container: HTMLElement;
  value: WorkflowDefinition;
  readonly?: boolean;
  onChange?: (value: WorkflowDefinition) => void;
}

export interface WorkflowDesignerCore {
  mount(): void;
  destroy(): void;
  getValue(): WorkflowDefinition;
  setValue(value: WorkflowDefinition): void;
}

export function createWorkflowDesignerCore(options: WorkflowDesignerCoreOptions): WorkflowDesignerCore {
  let currentValue = options.value;

  return {
    mount() {
      options.container.dataset.workflowDesignerMounted = 'true';
    },
    destroy() {
      delete options.container.dataset.workflowDesignerMounted;
    },
    getValue() {
      return currentValue;
    },
    setValue(value: WorkflowDefinition) {
      currentValue = value;
      options.onChange?.(value);
    }
  };
}
```

- [ ] **Step 3: Create React adapter**

Create `web/packages/workflow-designer-react/package.json`:

```json
{
  "name": "@aiworkflow/workflow-designer-react",
  "version": "0.1.0",
  "type": "module",
  "main": "src/index.tsx",
  "types": "src/index.tsx",
  "scripts": {
    "build": "tsc --noEmit",
    "test": "vitest run"
  },
  "dependencies": {
    "@aiworkflow/workflow-designer-core": "workspace:*",
    "@aiworkflow/workflow-schema": "workspace:*",
    "react": "^18.3.1"
  },
  "devDependencies": {
    "@types/react": "^18.3.12",
    "typescript": "^5.6.3",
    "vitest": "^2.1.4"
  },
  "peerDependencies": {
    "react": ">=18"
  }
}
```

Create `web/packages/workflow-designer-react/src/index.tsx`:

```tsx
import { createWorkflowDesignerCore } from '@aiworkflow/workflow-designer-core';
import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';
import { useEffect, useRef } from 'react';

export interface WorkflowDesignerReactProps {
  value: WorkflowDefinition;
  readonly?: boolean;
  onChange?: (value: WorkflowDefinition) => void;
}

export function WorkflowDesignerReact(props: WorkflowDesignerReactProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!containerRef.current) {
      return;
    }

    const designer = createWorkflowDesignerCore({
      container: containerRef.current,
      value: props.value,
      readonly: props.readonly,
      onChange: props.onChange
    });

    designer.mount();
    return () => designer.destroy();
  }, [props.value, props.readonly, props.onChange]);

  return <div ref={containerRef} style={{ width: '100%', height: '100%' }} />;
}
```

- [ ] **Step 4: Create Vue and Web Component minimal adapters**

Create `web/packages/workflow-designer-vue/package.json`:

```json
{
  "name": "@aiworkflow/workflow-designer-vue",
  "version": "0.1.0",
  "type": "module",
  "main": "src/index.ts",
  "types": "src/index.ts",
  "scripts": {
    "build": "tsc --noEmit"
  },
  "dependencies": {
    "@aiworkflow/workflow-designer-core": "workspace:*",
    "@aiworkflow/workflow-schema": "workspace:*"
  },
  "peerDependencies": {
    "vue": ">=3"
  },
  "devDependencies": {
    "typescript": "^5.6.3"
  }
}
```

Create `web/packages/workflow-designer-vue/src/index.ts`:

```ts
export { createWorkflowDesignerCore } from '@aiworkflow/workflow-designer-core';
export type { WorkflowDefinition } from '@aiworkflow/workflow-schema';
```

Create `web/packages/workflow-designer-wc/package.json`:

```json
{
  "name": "@aiworkflow/workflow-designer-wc",
  "version": "0.1.0",
  "type": "module",
  "main": "src/index.ts",
  "types": "src/index.ts",
  "scripts": {
    "build": "tsc --noEmit"
  },
  "dependencies": {
    "@aiworkflow/workflow-designer-core": "workspace:*",
    "@aiworkflow/workflow-schema": "workspace:*"
  },
  "devDependencies": {
    "typescript": "^5.6.3"
  }
}
```

Create `web/packages/workflow-designer-wc/src/index.ts`:

```ts
import { createEmptyWorkflowDefinition } from '@aiworkflow/workflow-schema';
import { createWorkflowDesignerCore, type WorkflowDesignerCore } from '@aiworkflow/workflow-designer-core';

export class AiWorkflowDesignerElement extends HTMLElement {
  private designer?: WorkflowDesignerCore;

  connectedCallback() {
    this.designer = createWorkflowDesignerCore({
      container: this,
      value: createEmptyWorkflowDefinition()
    });
    this.designer.mount();
  }

  disconnectedCallback() {
    this.designer?.destroy();
  }
}

customElements.define('ai-workflow-designer', AiWorkflowDesignerElement);
```

- [ ] **Step 5: Create SDK package**

Create `web/packages/workflow-sdk/package.json`:

```json
{
  "name": "@aiworkflow/workflow-sdk",
  "version": "0.1.0",
  "type": "module",
  "main": "src/index.ts",
  "types": "src/index.ts",
  "scripts": {
    "build": "tsc --noEmit"
  },
  "devDependencies": {
    "typescript": "^5.6.3"
  }
}
```

Create `web/packages/workflow-sdk/src/index.ts`:

```ts
export interface WorkflowRunRequest {
  input: Record<string, unknown>;
}

export interface WorkflowRunResponse {
  workflowId: string;
  runId: string;
}

export class AiWorkflowClient {
  constructor(
    private readonly baseUrl: string,
    private readonly apiKey: string
  ) {}

  async createRun(workflowId: string, request: WorkflowRunRequest): Promise<WorkflowRunResponse> {
    const response = await fetch(`${this.baseUrl}/openapi/v1/workflows/${workflowId}/runs`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${this.apiKey}`
      },
      body: JSON.stringify(request)
    });

    if (!response.ok) {
      throw new Error(`Failed to create workflow run: ${response.status}`);
    }

    const body = await response.json();
    return body.data;
  }
}
```

- [ ] **Step 6: Build workspace packages**

Run:

```bash
cd web
pnpm install
pnpm build
```

Expected: all TypeScript packages compile.

## Task 7: Verification

**Files:**
- No new files.

- [ ] **Step 1: Run backend verification**

Run:

```bash
mvn -f server/pom.xml test
```

Expected: all backend tests pass.

- [ ] **Step 2: Run frontend verification**

Run:

```bash
cd web
pnpm build
```

Expected: all frontend packages and admin app build.

- [ ] **Step 3: Run local services**

Run:

```bash
docker compose up -d postgres
mvn -f server/pom.xml spring-boot:run
```

Expected: backend starts on `http://localhost:8080`; OpenAPI JSON is available at `http://localhost:8080/v3/api-docs`; Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

- [ ] **Step 4: Run frontend dev server**

Run:

```bash
cd web
pnpm dev
```

Expected: admin frontend starts on `http://localhost:5173`.

## Task 8: Commit

**Files:**
- All files created by this plan.

- [ ] **Step 1: Initialize Git if needed**

Run only if `.git` does not exist:

```bash
git init
```

Expected: repository is initialized.

- [ ] **Step 2: Commit foundation scaffold**

Run:

```bash
git add docker-compose.yml server web docs/superpowers/plans/2026-05-27-ai-workflow-foundation.md
git commit -m "chore: scaffold ai workflow foundation"
```

Expected: commit succeeds.
