---
name: android-code-style
description: |
  Kotlin and Android code styling rules following official JetBrains/Google guidelines and Android Studio defaults. Use this skill whenever writing, refactoring, or reviewing code to ensure consistency in formatting, naming, and structure. Trigger on phrases like "format the code", "styling", "naming conventions", "indentation", or "refactor for style".
---

# Android / Kotlin Code Style

## Indentation & Formatting
- **Indentation**: Use **4 spaces**.
- **Line Length**: Limit to **120 characters**.
- **Trailing Commas**: Always use trailing commas in parameter lists, collection literals, and property declarations to improve diff readability.
- **Braces**: Opening braces `{` should be on the same line as the preceding code.

```kotlin
fun exampleFunction(
    param1: String,
    param2: Int, // Trailing comma
) { // Brace on same line
    val list = listOf(
        "one",
        "two",
    )
}
```

---

## Naming Conventions
- **Classes & Objects**: Use **PascalCase** (`ConnectionViewModel`, `MockController`).
- **Functions & Properties**: Use **camelCase** (`loadData`, `isRecording`).
- **Constants**: Use **SCREAMING_SNAKE_CASE** (`PW_RECORDER_NAME`).
- **Packages**: Use lowercase, dot-separated names (`music.jiminy.service`).
- **ViewModels**: Suffix with `ViewModel` (`RecordingScreenViewModel`).
- **Interfaces**: Do not use "I" prefix unless strictly required by project legacy; prefer descriptive names or `Impl` for implementations. (Note: Current project uses `JiminyLoggerI`, follow existing pattern if established).

---

## Layout & Structure
- **Blank Lines**: 
    - One blank line between properties, functions, and classes.
    - No blank lines at the beginning or end of a class or function block.
- **Arguments**: If a function call or declaration doesn't fit on one line, put each argument on its own line.
- **Visibility**: Prefer `private` by default. Use `internal` or `public` only when necessary.

---

## Imports
- **No Wildcards**: Never use wildcard imports (`import kotlinx.coroutines.*`).
- **Order**: Follow the standard Android Studio/Kotlin order:
    1. Android/Google imports
    2. Third-party library imports
    3. Project-specific imports
    4. Java/Kotlin standard library imports

---

## Control Flow
- **Single Return Point**: Avoid "return early", "fail fast", or "bail out" patterns. Functions should ideally have a single return point at the very end. Do not use `return` at the beginning or in the middle of a function; use `if/else` blocks to manage execution flow instead.

```kotlin
// Preferred style - Single return point
fun processData(data: Data?): Boolean {
    var success = false
    if (data != null) {
        if (data.isValid) {
            performProcessing(data)
            success = true
        }
    }
    return success
}

// Avoid - Multiple/early return points
fun avoidThis(data: Data?): Boolean {
    if (data == null) return false
    if (!data.isValid) return false
    performProcessing(data)
    return true
}
```

---

## Expression vs. Block Body
- For simple one-line functions, prefer expression body syntax:
```kotlin
fun isFinished() = state == Status.Finished
```
- For complex logic or side effects, always use block body:
```kotlin
fun processData() {
    val result = logic()
    updateState(result)
}
```

---

## Documentation
- Use KDoc (`/** ... */`) for documenting public APIs, complex functions, or classes.
- Use simple comments (`//`) for internal implementation details.
