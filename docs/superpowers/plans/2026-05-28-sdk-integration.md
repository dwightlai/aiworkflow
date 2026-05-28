# SDK Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a third-party integration surface through a typed TypeScript SDK and concise integration documentation.

**Architecture:** The SDK wraps the same REST APIs used by the admin app and stays framework-neutral so React, Vue, Node.js services, and embedded Web Component consumers can share it.

**Tech Stack:** TypeScript, Vitest, existing workflow schema package, REST API envelope.

---

## Tasks

- [ ] Add SDK tests for workflow definition and run API calls.
- [ ] Update `@aiworkflow/workflow-sdk` to expose typed list/create/publish/run/get-run methods and optional bearer auth.
- [ ] Add a concise integration guide for React, Vue/Web Component, and server-side SDK usage.
- [ ] Run SDK tests, frontend workspace build, backend tests, and clean status check.

