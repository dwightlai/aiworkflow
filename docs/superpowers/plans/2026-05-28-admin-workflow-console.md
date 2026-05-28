# Admin Workflow Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the Ant Design React admin app into a usable workflow console that can list, create, publish, run, and inspect AI workflow executions through the backend APIs.

**Architecture:** Keep the designer framework-agnostic package intact and integrate it through the React adapter. The admin app owns API calls, page state, forms, and debug panels; backend remains the source of truth for workflow definitions and execution results.

**Tech Stack:** React 18, Ant Design 5, TanStack Query 5, Vitest, TypeScript, Vite, existing `@aiworkflow/workflow-designer-react` and `@aiworkflow/workflow-schema` packages.

---

## Tasks

- [ ] Add a typed admin API client with Vitest coverage for list/create/publish/run/get-run behavior.
- [ ] Replace the placeholder workflow list with an operations console: list table, create drawer, definition JSON editor, publish/run actions, execution detail panel.
- [ ] Fix garbled Chinese UI labels in frontend packages.
- [ ] Build the frontend workspace and run targeted admin tests.
- [ ] Commit the verified frontend console work.

