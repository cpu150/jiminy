---
name: module-structure
description: Module layout and dependency rules for the Jiminy Universal project. Use this skill whenever deciding where code should live or understanding the project architecture.
---
 
# Universal Module Structure (Jiminy)
 
## Core Philosophy
 
- **Platform Separation**: Code is divided by its execution environment: Frontend (Web), Backend (Server), and Shared logic.
- **Shared First**: Any logic, model, or interface that can be shared between the frontend and backend lives in the \`:shared\` module.
 
---
 
## Module Layout
 
\`\`\`
app module      ← Compose Multiplatform frontend (Web: Wasm/JS)
:server          ← Ktor Backend (JVM)
:shared          ← Shared business logic, models, and platform abstractions
\`\`\`
 
### 1. :shared (Common Logic)
Contains domain models (\`JiminyDevice\`, \`JiminyCommand\`), constants, and interfaces (\`JiminyLoggerI\`) that are used by both the frontend and the backend.
- **commonMain**: Most shared code.
- **jvmMain / jsMain / wasmJsMain**: Platform-specific implementations of shared interfaces or platform checks.
 
### 2. app module (Frontend)
The UI layer using Compose Multiplatform.
- **viewmodel**: MVI ViewModels.
- **screen**: UI Composables and screen logic.
- **service**: Frontend-specific services (e.g., \`MainService\` for API calls).
 
### 3. :server (Backend)
The Ktor server application.
- **Controller**: Business logic for the server.
- **Application.kt**: Server entry point and routing.
 
---
 
## Dependency Rules
 
| Module | Depends on |
|---|---|
| \`app module\` | \`:shared\` |
| \`:server\` | \`:shared\` |
| \`:shared\` | Nothing |
 
---
 
## Key Libraries
 
| Concern | Library |
|---|---|
| DI | Koin |
| Networking | Ktor Client |
| Serialization | KotlinX Serialization |
| UI | Compose Multiplatform |
| State | Jetpack Lifecycle (Universal) |
| Logging | Custom (JiminyLoggerI) |
| Async | Coroutines + Flow |
| Testing | Ktor TestApplication, Kotlin Test |
