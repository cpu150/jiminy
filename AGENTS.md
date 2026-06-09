# AI Agent Context

Welcome! This file provides context for AI agents working on the Jiminy project.

## Skills and Conventions

Project-specific coding standards, architecture patterns, and conventions are documented in the `skills/` directory at the root of this project. Each subdirectory in `skills/` contains a `SKILL.md` file that defines a specific area of expertise or rule set.

Before suggesting or implementing changes, please refer to the following skills:

- **Universal Code Style**: Located in `skills/code-style/SKILL.md`. Includes rules like 4-space indentation, 120-char line limit, and mandatory trailing commas.
- **Presentation MVI**: Located in `skills/presentation-mvi/SKILL.md`. Defines the project's reactive UI pattern.
- **Dependency Injection (Koin)**: Located in `skills/di-koin/SKILL.md`. Defines how dependency injection is managed.
- **Git Conventions**: Located in `skills/git-conventions/SKILL.md`. Defines the conventional commit format for this project.
- **Data Layer**: Located in `skills/data-layer/SKILL.md`. Defines repository and data source patterns.
- **Error Handling**: Located in `skills/error-handling/SKILL.md`. Defines the `Result` type and error management.
- **Module Structure**: Located in `skills/module-structure/SKILL.md`. Defines the project's module hierarchy.
- **Navigation**: Located in `skills/navigation/SKILL.md`. Defines the type-safe navigation and current tab-based system.
- **Testing**: Located in `skills/testing/SKILL.md`. Defines testing patterns and libraries.
- **Compose UI**: Located in `skills/compose-ui/SKILL.md`. Defines best practices for Jetpack Compose UI.

## Core Rules

Always prioritize the rules defined in the `skills/` folder, specifically:

1.  **Return Point At The End Of The Function Only**: All functions should have a return exit point at the end of the function where possible. It could be several returns in a `when` or `if` instructions, but it should be the last instruction of the functions
2.  **Trailing Commas**: Mandatory for all multi-line declarations, function calls, and collections.
3.  **Conventional Commits**: Use the format `type(scope): description` for all commit messages. Provide a commit message after applying a change.
