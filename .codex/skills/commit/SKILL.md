---
name: commit
description: Create a git commit that follows the user's commit-subject instructions. Use when the user asks to commit changes, make a git commit, or prepare a commit after coding work. Prefer Conventional Commit subjects such as feat:, fix:, docs:, refactor:, test:, chore:, perf:, and style: unless the user or repository context specifies a different rule. Stage only the intended files, review the staged diff, and write a subject that matches the provided instructions.
---

# Commit

Create a git commit that follows the user's commit-subject instructions already provided in the conversation or repository context.

## Workflow

1. Inspect the current git status.
Identify which files changed and separate intended work from unrelated edits.

2. Determine the commit-subject rules.
Read the current conversation first. If the repo contains explicit commit-message guidance nearby, follow that too. Default to Conventional Commits when no conflicting rule is present.

3. Stage only the intended files.
Do not sweep unrelated user changes into the commit.

4. Review the staged diff.
Make sure the staged content matches the work that should be committed.

5. Write the commit subject using the provided rules.
If the user has given specific subject instructions, follow them exactly. Otherwise use Conventional Commit format: `<type>: <short description>`.

6. Create the commit with a non-interactive git command.
Prefer a single clear subject unless the user asked for a longer body.

## Guardrails

Prefer these Conventional Commit prefixes unless the user or repo says otherwise:

- `feat:` for new features
- `fix:` for bug fixes
- `docs:` for documentation-only changes
- `refactor:` for internal code changes without intended behavior change
- `test:` for adding or updating tests
- `chore:` for maintenance, tooling, or housekeeping
- `perf:` for performance improvements
- `style:` for formatting-only changes

Keep the subject short, specific, and in lowercase unless a proper noun requires capitalization.

Do not amend existing commits unless the user explicitly asks for it.

Do not include unrelated modified files just to make the working tree clean.

If the change set is mixed, summarize the staging boundary before committing so the user can confirm the scope.
