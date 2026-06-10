🤖 AI Agents: Please read AGENTS.md before contributing to this project to understand our specific coding standards and architecture patterns.

This is a Kotlin Multiplatform project targeting Web, Server.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:
- for the Wasm target (faster, modern browsers):
  - on macOS/Linux
    ```shell
    #  Update and Clean
    ./gradlew kotlinWasmUpgradeYarnLock
    ./gradlew clean

    # Build and package
    ./gradlew :composeApp:wasmJsBrowserDistribution
    ./gradlew :server:buildFatJar

    # Run (accessible at http://localhost:${DEBUG_SERVER_PORT}/)
    java --enable-native-access=ALL-UNNAMED -XX:+UseZGC -DDEBUG=true -jar server/build/libs/server-all.jar
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
    ```
- for the JS target (slower, supports older browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:jsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
    ```

### Testing

#### Ktor Server Tests

The server module includes HTTP endpoint tests using Ktor's `testApplication` engine
with a `MockController` and `DebugLogger` to avoid real hardware dependencies.

```shell
# on macOS/Linux
./gradlew :server:test

# on Windows
.\gradlew.bat :server:test
```

**Test coverage** (`ApplicationTest.kt`):

| Test | Endpoint | Validates |
|------|----------|-----------|
| `testGetDevices` | `GET /devices` | Returns device list with correct size |
| `testGetDeviceLinks` | `GET /link-devices` | Returns current device links |
| `testGetRecordings` | `GET /recordings` | Returns recording file list |
| `testServerLogsAndFlush` | `GET /server-logs`, `POST /flush-server-logs` | Log retrieval, flush clears all entries |
| `testDeleteRecordings` | `POST /delete-recordings` | Accepts file list, returns success |
| `testDownloadRecordingsEmpty` | `POST /download-recordings` | Rejects empty file list with 400 |
| `testLinkDevices` | `POST /link-devices` | Accepts link commands, returns success |
| `testStartStopRecordingLockedEndpoints` | `POST /start-recording`, `POST /stop-recording`, `GET /devices` | Recording lock/unlock lifecycle, endpoints return 423 while recording |

#### ComposeApp ViewModel Tests

The Compose web application includes ViewModel unit tests that run on both `wasmJs` and `js` targets.
Tests use `FakeMainService` (implementing the `MainService` interface) and `FakeLogger` to decouple
from real network and hardware dependencies.

```shell
# WebAssembly target (wasmJs) — on macOS/Linux
./gradlew :composeApp:wasmJsTest

# Javascript target (js) — on macOS/Linux
./gradlew :composeApp:jsTest

# on Windows
.\gradlew.bat :composeApp:wasmJsTest
.\gradlew.bat :composeApp:jsTest
```

**Test coverage**:

**`LogsViewModelTest`** — log aggregation and lifecycle:

| Test | Validates |
|------|-----------|
| `testInitialStateIsEmpty` | Empty logs on startup, no error |
| `testCombinedLogsSortedByTimestamp` | Client + server logs merged and sorted descending |
| `testLoadServerLogs` | Server logs fetched and displayed correctly |
| `testFlushLogsClearsBothClientAndServer` | Flush empties both client and server logs |
| `testResetError` | Error state resets to null |

**`RecordingScreenViewModelTest`** — recording workflow:

| Test | Validates |
|------|-----------|
| `testInitialState` | Empty devices, no selection, recordings hidden |
| `testDeviceCollectionFiltersCorrectly` | PW Recorder device filtered out |
| `testOnDeviceClickAndDismissDetails` | Detail dialog show/dismiss |
| `testNodeSelectionLimit` | Channel limit enforced, error on overflow |
| `testStartAndStopRecordingFlow` | Full record/stop lifecycle |
| `testRecordingSelectionAndDeletion` | Multi-select and batch delete |

**`ConnectionScreenViewModelTest`** — connection management:

| Test | Validates |
|------|-----------|
| `testInitialStateHasOneEmptyRow` | Starts with one empty connection row |
| `testAudioDeviceFiltering` | PW Recorder device filtered out |
| `testDragActionStateUpdates` | Drag start/move offsets tracked |
| `testEnsureOneEmptyRowEnforcement` | New empty row appended after fill |
| `testUnlinkAllAction` | Unlink confirmation dialog show/dismiss |

**`ThemeViewModelTest`** — theme management:

| Test | Validates |
|------|-----------|
| `testInitialThemeIsDarkByDefault` | Starts with DARK theme |
| `testInitialThemeLoadedFromLocalStorage` | Respects saved theme preference |
| `testToggleThemeCyclesThroughAllThemes` | Cycles through DARK, LIGHT, and IRIS |

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).