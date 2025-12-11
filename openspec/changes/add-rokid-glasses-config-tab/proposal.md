# Change: Add Rokid Glasses Config Tab

## Why
Rokid glasses need a dedicated place in the app to connect, monitor status, and manage integration options without mixing them into existing chat or provider settings.

## What Changes
- Add a “Glasses” tab reachable from settings with UI for Rokid glasses connection status, connect/disconnect, and error feedback.
- Add Bluetooth discovery/permission/enablement flow following the Rokid sample (request permissions, prompt to enable Bluetooth, scan/filter Rokid devices via service UUID, list bonded/connected devices).
- Expose basic integration options (e.g., enabling glasses input/output usage in chat flows) with persisted settings.
- Wire the new tab to the Rokid CXR client dependency for connection lifecycle handling.

## Impact
- Affected specs: manage-rokid-glasses
- Affected code: navigation graph, settings/glasses UI, viewmodel/state for glasses, Rokid CXR client integration layer
