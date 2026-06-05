# Identity Organization Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the backend identity, organization, local authentication, JWT context, and API Key integration foundation from `2026-06-05-identity-organization-auth-design.md`.

**Architecture:** Add `auth` as a focused backend module under `com.mw.ai.agi.auth` with runtime identity records, MyBatis Plus entities/mappers, services, controllers, and initialization. Keep current business APIs permissive for now, but expose working `/api/auth/**` local login APIs and reusable third-party API Key validation services for open APIs.

**Tech Stack:** Spring Boot 3.3, Spring Security password hashing, MyBatis Plus, Flyway PostgreSQL/Dameng migrations, JUnit/MockMvc.

---

### Task 1: Schema And Default Data

**Files:**
- Create: `server/src/main/resources/db/migration/postgresql/V14__identity_organization_auth_schema.sql`
- Modify: `server/src/main/resources/db/migration/dameng/V1__agi_schema.sql`
- Test: `server/src/test/java/com/mw/ai/agi/config/FlywaySchemaMigrationTest.java`
- Test: `server/src/test/java/com/mw/ai/agi/config/DamengSchemaMigrationContractTest.java`

- [ ] **Step 1: Write failing migration assertions**

Add assertions that PostgreSQL/H2 migrations create `agi_tenant`, `agi_unit`, `agi_department`, `agi_role`, `agi_user`, `agi_user_unit`, `agi_user_department`, `agi_user_role`, `agi_integration_app`, `agi_integration_app_secret`, `agi_integration_app_scope`, `agi_login_session`, and `agi_auth_audit_log`, and that `agi_unit.code` and `agi_department.code` exist.

- [ ] **Step 2: Run migration tests and verify failure**

Run: `mvn -q -Dtest=FlywaySchemaMigrationTest,DamengSchemaMigrationContractTest test`

Expected: failure because the new tables and columns do not exist yet.

- [ ] **Step 3: Add PostgreSQL migration**

Create V14 with all `agi_` tables, default tenant/unit/department/roles/admin user, and unique indexes on codes.

- [ ] **Step 4: Add Dameng contract SQL**

Mirror the table definitions in the Dameng schema contract so the architecture tests see the same table names and required columns.

- [ ] **Step 5: Run migration tests**

Run: `mvn -q -Dtest=FlywaySchemaMigrationTest,DamengSchemaMigrationContractTest test`

Expected: pass.

### Task 2: Runtime Identity Persistence And Bootstrap Service

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/auth/persistence/*.java`
- Create: `server/src/main/java/com/mw/ai/agi/auth/service/*.java`
- Create: `server/src/main/java/com/mw/ai/agi/auth/service/IdentityBootstrapService.java`
- Test: `server/src/test/java/com/mw/ai/agi/auth/api/AuthControllerIntegrationTest.java`

- [ ] **Step 1: Write failing bootstrap test**

Test that default tenant, unit, department, platform roles, admin user, and admin relationships are readable through `IdentityStore`.

- [ ] **Step 2: Run test and verify failure**

Run: `mvn -q -Dtest=AuthControllerIntegrationTest test`

Expected: fails because auth domain and store do not exist.

- [x] **Step 3: Implement minimal runtime identity records and MyBatis persistence**

Add records/services for user principals, runtime identity context, third-party caller context, sessions, audit logs, and integration app validation.

- [ ] **Step 4: Implement bootstrap service**

Ensure default records exist at startup and encode admin password from `AGI_ADMIN_PASSWORD`, defaulting to `admin123` for local development tests.

- [ ] **Step 5: Run bootstrap test**

Run: `mvn -q -Dtest=AuthControllerIntegrationTest test`

Expected: pass.

### Task 3: Local Login, JWT, Refresh, Logout, And Me

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/auth/service/PasswordHasher.java`
- Create: `server/src/main/java/com/mw/ai/agi/auth/service/JwtTokenService.java`
- Create: `server/src/main/java/com/mw/ai/agi/auth/service/AuthService.java`
- Create: `server/src/main/java/com/mw/ai/agi/auth/api/AuthController.java`
- Test: `server/src/test/java/com/mw/ai/agi/auth/api/AuthControllerIntegrationTest.java`

- [ ] **Step 1: Write failing login test**

Test `POST /api/auth/login` with `admin/admin123` returns access token, refresh token, `tenant_default`, `unit_default`, `activeUnitId`, `dept_default`, and `platform_admin`.

- [ ] **Step 2: Write failing auth error test**

Test bad password returns an API error with `AUTH_LOGIN_FAILED`.

- [ ] **Step 3: Run tests and verify failure**

Run: `mvn -q -Dtest=AuthControllerIntegrationTest test`

Expected: fails because endpoint does not exist.

- [ ] **Step 4: Implement login and JWT service**

Use HMAC SHA-256 JWT implemented with JDK crypto and Base64URL, without adding new dependencies. Include `tenantId`, `unitIds`, `activeUnitId`, `departmentIds`, `roleIds`, `userType`, and `tokenType`.

- [ ] **Step 5: Implement refresh, logout, and me**

Persist refresh token hashes in `agi_login_session`, revoke on logout, and parse Bearer access token for `/api/auth/me`.

- [ ] **Step 6: Run auth tests**

Run: `mvn -q -Dtest=AuthControllerIntegrationTest test`

Expected: pass.

### Task 4: Runtime Identity Context And Third-Party API Key Validation

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/auth/service/RuntimeIdentityContext.java`
- Create: `server/src/main/java/com/mw/ai/agi/auth/service/IntegrationAppService.java`
- Create: `server/src/main/java/com/mw/ai/agi/auth/api/IntegrationAppAdminController.java`
- Test: `server/src/test/java/com/mw/ai/agi/auth/service/IntegrationAppAuthenticatorTest.java`

- [ ] **Step 1: Write failing API Key validation test**

Test that an enabled `ARCHIVE_SYSTEM` integration app with a secret and `UNIT` scope validates `X-AGI-App-Code`, API key, `unitId`, `departmentIds`, `roleIds`, and external `userId` into a `RuntimeIdentityContext`.

- [ ] **Step 2: Write failing denial test**

Test invalid key writes `APP_SECRET_INVALID` audit log and throws a validation exception.

- [ ] **Step 3: Run tests and verify failure**

Run: `mvn -q -Dtest=IntegrationAppAuthenticatorTest test`

Expected: fails because service does not exist.

- [ ] **Step 4: Implement IntegrationAppService**

Hash API keys, compare securely, check app status, secret status, expiration, and `UNIT` scope. Record `API_KEY_USED`, `APP_DENIED`, and error codes in `agi_auth_audit_log`.

- [ ] **Step 5: Run service tests**

Run: `mvn -q -Dtest=IntegrationAppAuthenticatorTest test`

Expected: pass.

### Task 5: Verification And Service Restart

**Files:**
- No new files.

- [ ] **Step 1: Compile**

Run: `mvn -q -DskipTests compile`

Expected: pass.

- [ ] **Step 2: Run focused tests**

Run: `mvn -q -Dtest=AuthControllerIntegrationTest,IntegrationAppAuthenticatorTest,AuthPersistenceStyleTest,FlywaySchemaMigrationTest,DamengSchemaMigrationContractTest test`

Expected: pass.

- [ ] **Step 3: Restart backend**

Stop the process listening on 8080 and start `mvn spring-boot:run -q` with logs redirected to `server/target/codex-spring-boot-run.log`.

- [ ] **Step 4: Probe API**

Run: `Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/auth/me`

Expected: 401 or an auth error without crashing, proving the service is up and the auth controller is loaded.
