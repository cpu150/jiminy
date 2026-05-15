---
name: git-conventions
description: |
  Git commit message conventions following the Conventional Commits specification. Use this skill when summarizing changes, creating commit messages, or documenting the history of the project. Trigger on phrases like "commit message", "git convention", "summarize changes", or "write a commit".
---

# Git Commit Conventions

## Specification
Messages must be structured as follows:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

## Types
- **`feat`**: A new feature (correlates with MINOR in Semantic Versioning).
- **`fix`**: A bug fix (correlates with PATCH in Semantic Versioning).
- **`docs`**: Documentation only changes.
- **`style`**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc).
- **`refactor`**: A code change that neither fixes a bug nor adds a feature.
- **`perf`**: A code change that improves performance.
- **`test`**: Adding missing tests or correcting existing tests.
- **`build`**: Changes that affect the build system or external dependencies (example scopes: gradle, dependencies).
- **`ci`**: Changes to our CI configuration files and scripts.
- **`chore`**: Other changes that don't modify src or test files.
- **`revert`**: Reverts a previous commit.

## Rules
- **Subject Line**:
    - Limit to **50 characters**.
    - Use the **imperative, present tense**: "change" not "changed" nor "changes".
    - Do not capitalize the first letter.
    - Do not end with a period.
- **Body**:
    - Separate from the subject with a blank line.
    - Wrap at **72 characters**.
    - Use the body to explain **what** and **why** (the motivation for the change) rather than how.
- **Breaking Changes**: Indicated by a `!` after the type/scope (e.g., `feat!: ...`) and must have a `BREAKING CHANGE:` footer.

## Examples
- `feat(ui): add volume slider to mixer`
- `fix(server): resolve race condition in recording lock`
- `docs: update setup instructions in README`
- `refactor!: migrate presentation layer to MVI`
