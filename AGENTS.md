# Persistent EECS3311 Stage 2 Git Workflow

These instructions apply to this repository and its subdirectories across future Codex sessions. Preserve existing project files and instructions. Use the existing repository, current branch, and configured GitHub remote.

## Explicit workflow triggers

When the user explicitly requests "commit", "git", or "save progress" as an instruction to save project progress, execute the workflow below without unnecessary confirmation when existing permissions allow it. Do not automatically commit after code modifications. Mentions in questions, quotations, documentation, prohibitions, or workflow setup requests are not authorization to commit or push. Follow any explicit narrower instruction (for example, status only or commit without pushing).

## Git workflow

1. Inspect the current branch, Git status, existing staged changes, and relevant working-tree changes. Identify the implementation work being saved.
2. Review the actual diffs and appropriate existing validation results. Include only appropriate project files relevant to that work. Do not mix unrelated changes or overwrite existing work.
3. Exclude API keys, credentials, secret-bearing files, `.env` files, temporary files, and generated artifacts that should not be tracked. Inspect staged content as well as unstaged content; do not print secrets in output. Do not assume an ignore rule protects files already tracked.
4. Stage only explicitly selected relevant files or hunks. Review the final staged diff before committing. If unrelated or sensitive content is already staged, resolve the scope without discarding user work; ask only if a safe resolution needs user input.
5. Create one coherent commit with a clear, meaningful, descriptive English message explaining the actual implementation changes. Do not create empty commits or artificially split trivial changes into multiple commits. If there are no meaningful changes to save, inform the user instead of creating an unnecessary commit.
6. Push the new commit to the existing GitHub repository using the current branch. Do not create a repository, switch branches, rewrite history, force-push, or alter Git configuration or commit timestamps unless explicitly authorized. If the destination is ambiguous or unavailable, explain the issue instead of guessing.
7. Verify the created commit's hash, message, branch, and contents. Verify push success against the remote branch, including that the new commit is present there; do not rely solely on a local commit or a stale remote-tracking reference. If verification is unavailable, report that limitation rather than claiming success.
8. Report the commit hash, commit message, branch, push status, and the current reporting-period meaningful-code-commit count described below.

If any Git operation fails, clearly explain which step failed, preserve completed work, and suggest a concrete solution. Distinguish a successful local commit from a failed or unverified push. Avoid unnecessary confirmation requests, but never bypass required permissions or claim unverified success.

## EECS3311 Stage 2 reporting periods

The user's course requirements are:

| Reporting period (inclusive) | Minimum meaningful code commits |
| --- | --- |
| October 20-26, 2026 | 5 |
| October 27-November 2, 2026 | 5 |
| November 3-9, 2026 | 5 |

Each meaningful code commit must represent genuine implementation or testing progress, such as implementing a feature, fixing a defect, performing a substantive refactoring, or adding relevant tests. Report-only changes, formatting-only changes, empty commits, and artificially divided trivial modifications must not be counted. This workflow-instruction setup alone is not a meaningful code commit.

Use descriptive commit messages and push qualifying commits to GitHub within the corresponding reporting period. Never manufacture work, dates, or commit history to reach a quota.

Whenever the user requests a commit:

- Determine the current reporting period using the actual current date and the project/user timezone (America/New_York unless the user specifies otherwise). Outside the listed periods, report that no listed period is currently active.
- Inspect available Git history and actual diffs, not just commit titles, to identify qualifying Stage 2 implementation/testing commits. Count unique commits once; do not count merge-only records or duplicated changes as additional implementation progress.
- State the date window, timezone/date basis, history scope, qualifying count, and progress toward the minimum of five. Include the new commit only when it qualifies.
- Distinguish local qualifying commits from those verified present on GitHub. Git commit timestamps do not prove when a push occurred; if historical push timing or history completeness cannot be established, state that limitation. Do not present an inferred count as guaranteed professor acceptance.

## Scope of setup and future work

Creating or updating this file does not authorize a commit or push during setup. Do not change application code merely to establish this workflow. Preserve existing instructions when updating this file. On future explicit workflow requests, execute the steps above rather than only proposing them.
