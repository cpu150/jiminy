# AI Agent Context

Welcome! This file provides context for AI agents working on the Jiminy project.

## Skills and Conventions

Project-specific coding standards, architecture patterns, and conventions are documented in the `skills/` directory at the root of this project. Each subdirectory in `skills/` contains a `SKILL.md` file that defines a specific area of expertise or rule set.

Before suggesting or implementing changes, please refer to the following skills:

- **Android Code Style**: Located in `skills/android-code-style/SKILL.md`. Includes rules like 4-space indentation, 120-char line limit, and mandatory trailing commas.
- **Android Presentation MVI**: Located in `skills/android-presentation-mvi/SKILL.md`. Defines the project's reactive UI pattern.
- **Android DI Koin**: Located in `skills/android-di-koin/SKILL.md`. Defines how dependency injection is managed.
- **Git Conventions**: Located in `skills/git-conventions/SKILL.md`. Defines the conventional commit format for this project.
- **Android Data Layer**: Located in `skills/android-data-layer/SKILL.md`. Defines repository and data source patterns.
- **Android Error Handling**: Located in `skills/android-error-handling/SKILL.md`. Defines the `Result` type and error management.
- **Android Module Structure**: Located in `skills/android-module-structure/SKILL.md`. Defines the project's module hierarchy.

## Core Rules

Always prioritize the rules defined in the `skills/` folder, specifically:

1.  **Return Point At The End Of The Function Only**: All functions should have a return exit point at the end of the function where possible. It could be several returns in a `when` or `if` instructions, but it should be the last instruction of the functions
2.  **Trailing Commas**: Mandatory for all multi-line declarations, function calls, and collections.
3.  **Conventional Commits**: Use the format `type(scope): description` for all commit messages. Provide a commit message after applying a change.
