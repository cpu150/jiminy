# Remove confirmation pop-ups for node and device removal in staging area

The user wants to remove the confirmation pop-ups when removing a node or a device from a row in the staging area of the Audio and MIDI connection screens. This applies specifically to `OnDeleteNodeFromRow` and `OnDeleteDeviceFromRow`. The confirmation for un-linking active links (`LinkRow`) should remain.

## Proposed Changes

### [composeApp]

#### [ConnectionScreen.kt](file:///Users/cpu150/Development/jiminy/composeApp/src/webMain/kotlin/music/jiminy/screen/ConnectionScreen.kt)

- In `ConnectionRow`, remove `nodeToDelete` and `deviceToDelete` state variables.
- Update `onDeviceClick` and `onNodeClick` in `DeviceCardNodeDetails` within `ConnectionRow` to directly call `onDeleteDevice` and `onDeleteNode`.
- Remove the `DeleteConfirmationAlert` blocks from `ConnectionRow`.

## Verification Plan

### Automated Tests
- I will check if there are any UI tests that expect these specific pop-ups in the staging area.
- Run existing tests: `./gradlew :composeApp:test`

### Manual Verification
- Since I cannot run the app, I will verify the code changes by:
    - Ensuring references to `nodeToDelete` and `deviceToDelete` are removed from `ConnectionRow`.
    - Ensuring `onDeleteDevice` and `onDeleteNode` are called immediately upon clicking.
    - Confirming that `LinkRow` and its `UnlinkConfirmationAlert` remain untouched.
