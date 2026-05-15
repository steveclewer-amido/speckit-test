<!--
Sync Impact Report

- Version change: template placeholder → 1.0.0
- Modified principles: filled template placeholders with concrete principles
	- I. Code Quality & Maintainability (NON-NEGOTIABLE)
	- II. Testing Standards (NON-NEGOTIABLE)
	- III. UX Consistency & Accessibility
	- IV. Performance Budgets & Regression Prevention
	- V. Definition of Done & Quality Gates
- Added sections:
	- Quality & Performance Requirements
	- Workflow & Reviews
- Removed sections: None
- Templates requiring updates:
	- ✅ .specify/templates/tasks-template.md
- Follow-up TODOs: None
-->

# speckit-test Constitution

## Core Principles

### I. Code Quality & Maintainability (NON-NEGOTIABLE)
All production changes MUST be readable, consistent, and maintainable.

- Prefer the simplest design that meets the spec (avoid unnecessary patterns).
- Keep modules cohesive: small, well-named functions; clear responsibilities.
- Eliminate dead code and unused dependencies.
- Public APIs MUST be documented (docstrings/comments where the language expects).
- Changes MUST keep or improve static analysis and linting results (where configured).

### II. Testing Standards (NON-NEGOTIABLE)
Every change MUST be verifiable via automated tests appropriate to the behavior.

- New behavior MUST include tests; bug fixes MUST add a regression test.
- Prefer fast unit tests; add integration/contract tests when crossing boundaries.
- Tests MUST be deterministic (no flakes, fixed clocks/random seeds where needed).
- CI MUST fail on red tests; skipping/disabling tests requires explicit justification.

### III. UX Consistency & Accessibility
User-facing behavior MUST be consistent, predictable, and accessible.

- Reuse the existing design system/components and established interaction patterns.
- Changes MUST include empty/loading/error states when applicable.
- Accessibility MUST be preserved: keyboard navigation, labels, contrast, and focus.
- Avoid breaking changes to UX flows without updating the spec and acceptance scenarios.

### IV. Performance Budgets & Regression Prevention
Performance MUST be treated as a feature with measurable budgets.

- Each feature MUST declare relevant performance budgets (what, how measured, and target)
	in the feature plan.
- Changes MUST not introduce known regressions (latency, memory, CPU, bundle size,
	or startup time) without explicit sign-off and a mitigation plan.
- Prefer measuring before optimizing; profile to find the true bottleneck.

### V. Definition of Done & Quality Gates
A task is not “done” until quality gates are met.

- Code compiles/builds and all required checks pass (lint, typecheck, tests).
- Acceptance scenarios in the spec are satisfied.
- UX consistency and accessibility checks are completed for user-facing changes.
- Performance budgets are validated for performance-sensitive changes.
- Documentation is updated when behavior or usage changes.

## Quality & Performance Requirements

- Feature work MUST define measurable success criteria and acceptance scenarios.
- Feature plans MUST state relevant performance budgets and how they are validated.
- If a performance budget cannot be met, the plan MUST include:
	- the current measured baseline,
	- the expected impact,
	- mitigations, and
	- an explicit decision record.

## Workflow & Reviews

- Work is tracked via feature specs/plans/tasks produced by Spec Kit.
- Pull requests MUST:
	- link to the relevant spec/plan/tasks,
	- pass CI checks,
	- include tests per the Testing Standards principle,
	- include UX review notes for UI changes, and
	- include performance validation notes when budgets are defined.

## Governance
This constitution governs how work is specified, implemented, and reviewed.

- Amendments MUST be made via a pull request that:
	- explains the change,
	- updates any impacted templates and guidance docs, and
	- bumps the constitution version.
- Versioning follows semantic versioning:
	- MAJOR: breaking governance changes or principle removals/redefinitions
	- MINOR: new principles/sections or materially expanded guidance
	- PATCH: clarifications and non-semantic refinements
- All reviews MUST consider constitution compliance as part of “Definition of Done”.

**Version**: 1.0.0 | **Ratified**: 2026-05-14 | **Last Amended**: 2026-05-14
