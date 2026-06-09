---
name: plan
description: Inspect the codebase, produce an implementation plan, identify risks, and pause for approval before large code changes. Use when a request may touch multiple files, alter architecture or data flow, introduce UI plus backend wiring, or otherwise benefit from an explicit review step before coding.
---

# Plan

Follow this workflow before implementing large or risky changes.

## Workflow

1. Understand the request.
Clarify the user goal, constraints, and definition of success from the prompt and nearby context.

2. Examine relevant files.
Inspect the code paths that are likely to be affected before proposing a solution.

3. Produce an implementation plan.
Describe the intended approach, the main files or systems involved, and the order of work.

4. Identify risks.
Call out behavioral regressions, coupling concerns, migration risk, testing gaps, or unclear assumptions.

5. Wait for approval before coding large changes.
Pause after presenting the plan and risks when the change is large, cross-cutting, or hard to reverse.

## Guidance

Treat a change as large when it affects multiple screens or services, changes data models or persistence, rewires shared flows, adds new infrastructure, or is likely to require substantial testing.

If the change is small and low risk, use the same thinking process but do not force an approval pause unless the user asks for one.

Keep the pre-implementation summary concise and decision-oriented so the user can quickly approve, redirect, or narrow scope.
