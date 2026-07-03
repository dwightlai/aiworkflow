# Knowledge Original File Download Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add authenticated original-file downloads beside knowledge document names.

**Architecture:** Resolve original files in the knowledge service after ownership and permission checks, stream them from the controller, and expose only an availability flag to the admin UI. The frontend downloads with the authenticated API client as a Blob.

**Tech Stack:** Java 17, Spring MVC, React, TypeScript, Ant Design, JUnit 5, Vitest.

---

### Task 1: Backend download contract

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeBaseService.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/api/KnowledgeBaseController.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/service/KnowledgeOriginalFileDownloadTest.java`

- [ ] Write a failing service test for an uploaded original file.
- [ ] Implement safe resolution and availability checks.
- [ ] Add the authenticated streaming endpoint and response headers.
- [ ] Run focused backend tests.

### Task 2: Admin download link

**Files:**
- Modify: `web/apps/admin/src/api/knowledge.ts`
- Modify: `web/apps/admin/src/pages/knowledge/KnowledgeDocumentsPage.tsx`
- Test: `web/apps/admin/src/pages/knowledge/KnowledgeDocumentPages.test.tsx`

- [ ] Write a failing page test for download visibility.
- [ ] Add the authenticated Blob API.
- [ ] Render a download icon beside eligible document names.
- [ ] Run focused tests and the admin build.

### Task 3: Verification

- [ ] Run backend tests and package.
- [ ] Run frontend tests and build.
- [ ] Restart 8080 and verify 5173, 5174, and 8080 listeners.
