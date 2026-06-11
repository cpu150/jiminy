# Release Procedure

This document outlines the steps to tag and publish a new release for the Jiminy project.

## Prerequisites

- [GitHub CLI (gh)](https://cli.github.com/) installed and authenticated (`gh auth login`).
- All changes committed and pushed to the `main` branch.
- Java 21+ installed (required for the build process).

## Step 0: Safety Check

Before starting the release, ensure your local repository is up to date and your working directory is clean. Any uncommitted changes will cause the process to abort.

1.  **Sync Repository:**
    ```bash
    git fetch --all -Ptpmf --recurse-submodules
    git pull
    git submodule update
    ```

2.  **Verify clean state:**
    ```bash
    git status --porcelain
    ```
    If this command returns any output, you must commit or stash your changes before proceeding.

---

## Step 1: Version Confirmation & Bumping

Before starting, the current version must be identified from `jiminy/build.gradle.kts`.

### Current Versioning Scheme: Two Digits (e.g., `1.0`)

1.  **Check current version:**
    ```bash
    grep "version =" build.gradle.kts
    ```

2.  **Decide on New Version:**
    Propose the following options based on the current version (e.g., if current is `1.0`):
    - **Keep:** `1.0` (Current)
    - **Minor Bump:** `1.1`
    - **Major Bump:** `2.0`

3.  **Update Gradle (if bumping):**
    Edit `jiminy/build.gradle.kts`:
    ```kotlin
    version = "1.x" 
    ```
4.  **Commit and push (if changed):**
    Ensure commit messages follow the **Conventional Commits** specification (e.g., using `chore` type for version bumps):
    ```bash
    git add build.gradle.kts
    git commit -m "build(release): bump version to 1.x"
    git push origin main
    ```

5.  **Git Tagging:**
    The best way to handle the tag is to let `gh release create` (in Step 3) do it for you. It will automatically create the tag on GitHub and associate it with the correct commit.

    However, if you want to create it manually on your local machine first:
    ```bash
    git tag -a v1.x -m "release v1.x"
    git push origin v1.x
    ```

## Step 2: Build Artifacts

Generate the "Fat Jar" which includes the server and the bundled Wasm frontend.

1.  **Clean and Prepare:**
    ```bash
    ./gradlew kotlinWasmUpgradeYarnLock
    ./gradlew clean
    ```

2.  **Build:**
    ```bash
    ./gradlew :composeApp:wasmJsBrowserDistribution
    ./gradlew :server:buildFatJar
    ```

The output will be located at:
`server/build/libs/server-all.jar`

## Step 3: Review Release Notes

Before publishing, generate a preview of the release notes to ensure they are correct.

1.  **Generate Preview:**
    ```bash
    gh release create v1.x --generate-notes --draft --title "Release v1.x"
    ```
    *Note: Using `--draft` ensures the release is not visible to the public yet.*

2.  **Review and Edit:**
    Check the draft on GitHub or via CLI. If changes are needed, edit the draft:
    ```bash
    gh release edit v1.x --notes "Manual notes here..."
    ```

3.  **Confirm:** Ask the user if the notes are acceptable or if they want to provide a manual description.

## Step 4: Finalize and Upload Assets

1.  **Upload Assets:**
    Attach the built JAR file to the draft release:
    ```bash
    gh release upload v1.x server/build/libs/server-all.jar
    ```

2.  **Publish:**
    Once everything is ready, publish the draft:
    ```bash
    gh release edit v1.x --draft=false
    ```

---

## Automation (Gemini CLI)

You can ask Gemini CLI to handle this entire process for you:
> "Tag and publish release v1.0"
