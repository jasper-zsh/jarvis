## Context
- Add a Glasses tab to manage Rokid glasses connectivity and integration options in-app.
- New dependency: `com.rokid.cxr:client-m` provides the client for glasses connection; we need a thin integration layer to manage lifecycle and expose state to Compose.
- Current app uses a simple navigation graph with separate screens for chat and settings; no existing glasses capability.

## Goals / Non-Goals
- Goals: Surface a dedicated tab reachable from Settings; show connection status; allow connect/disconnect; expose a small set of integration toggles with persistence; handle basic errors; support basic Bluetooth discovery/pair/connect flow for Rokid glasses.
- Non-Goals: Full-featured AR/streaming UX, deep device diagnostics, or multi-device management; advanced background services beyond what CXR requires.

## Decisions
- Create a glasses-specific state holder/viewmodel that wraps the Rokid CXR client to manage connection lifecycle.
- Persist glasses integration options alongside existing settings storage (reuse current data layer if available; otherwise add a simple preference-backed store).
- Add navigation entry from Settings to the Glasses tab, keeping the rest of the navigation unchanged.
- Reuse the Rokid sample Bluetooth flow as a reference: request required Bluetooth/location permissions, prompt to enable Bluetooth, scan for bonded/connected devices with Rokid service UUID `00009100-0000-1000-8000-00805f9b34fb`, and surface device discovery + connect/disconnect actions in the tab.
- For connection, use `CxrApi.getInstance().initBluetooth(context, device, BluetoothStatusCallback)` to obtain `socketUuid` and `macAddress`, then call `connect(context, socketUuid, macAddress)`; surface lifecycle callbacks (`onConnected`, `onDisconnected`, `onFailed` with `ValueUtil.CxrBluetoothErrorCode`) in UI state.
- When connecting, use `CxrApi.getInstance().connectBluetooth(context, socketUuid, macAddress, BluetoothStatusCallback)` mirroring the sample: capture connection info (including `rokidAccount`, `glassesType`), set UI to connected on `onConnected`, reset on `onDisconnected`, and map `onFailed` error codes to user-friendly messages.
- For status checks, poll or observe `CxrApi.getInstance().isBluetoothConnected` to display connected/disconnected state and guard actions on the Glasses tab.
- Provide a clean disconnect via `CxrApi.getInstance().deinitBluetooth()` only when the app is exiting or the glasses entry is deleted, avoiding unnecessary teardown during normal tab navigation.
- Auto-reconnect: if a glasses device is persisted and was previously connected, attempt reconnect on app start/resume by reusing stored socket UUID/MAC, gating on Bluetooth being enabled and permissions granted.
- Surface an info card when connected, showing device name, status, and available metadata (Rokid account, glasses type, socket UUID, MAC) since all fields are approved as safe to display.

## Risks / Trade-offs
- Dependency behavior is unknown in the current codebase; we may need to adapt threading/lifecycle once implementation starts.
- Device availability is hardware-dependent; testing will rely on mocks/emulators where possible and manual validation on device.

## Open Questions
- Which specific integration options are required (e.g., enabling glasses audio input, camera feed to chat, notifications on glasses)? Initial plan uses a minimal enable/disable set.
- Should the tab support multiple Rokid devices or just a single active connection?
- Is Bluetooth pairing handled elsewhere, or should the tab include prompts for pairing/bonding as in the sample helper?
- Metadata display clarified: show all available fields (socket UUID, MAC, Rokid account, glasses type) on the info card.
- Auto-reconnect clarified: attempt reconnect if glasses are persisted and were connected; otherwise require manual connect.
- Confirm deinit trigger points (app exit or device deletion only) to avoid dropping connections during normal navigation.
