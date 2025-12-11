## ADDED Requirements
### Requirement: Glasses Tab Navigation
The app SHALL provide a dedicated Glasses tab accessible from Settings for managing Rokid glasses.

#### Scenario: Open glasses tab from settings
- **WHEN** a user opens Settings and selects the Glasses tab entry
- **THEN** the app navigates to the Glasses tab showing glasses status and controls

### Requirement: Glasses Connection Lifecycle
The Glasses tab SHALL show current Rokid glasses connection status and allow users to initiate connect/disconnect with clear feedback on success or failure.

#### Scenario: Connect to glasses successfully
- **WHEN** the user taps Connect on the Glasses tab while a compatible Rokid glasses device is available
- **THEN** the app attempts connection via the Rokid CXR client, updates status to connected on success, and surfaces any errors if the attempt fails

### Requirement: Bluetooth Discovery and Permissions
The Glasses tab SHALL guide users through Bluetooth permission/enablement and scan for Rokid glasses devices using the Rokid service UUID filter, surfacing discovered or bonded devices for selection.

#### Scenario: Request permissions and scan for Rokid glasses
- **WHEN** the user opens the Glasses tab without required Bluetooth permissions or enabled Bluetooth
- **THEN** the app requests the necessary permissions (including Android 12+ scan/connect), prompts to enable Bluetooth if disabled, and once granted/enabled starts scanning for devices advertising the Rokid service UUID, listing any discovered or bonded Rokid glasses

### Requirement: Rokid Bluetooth Initialization
When a Rokid glasses device is selected, the app SHALL initialize Bluetooth via the Rokid CXR client, obtain connection info, and proceed to connect or report errors.

#### Scenario: Initialize via CXR and connect
- **WHEN** the user selects a Rokid glasses device from the list
- **THEN** the app calls `CxrApi.getInstance().initBluetooth(context, device, BluetoothStatusCallback)`, handles `onConnectionInfo(socketUuid, macAddress, …)` by invoking `connect(context, socketUuid, macAddress)`, updates status on `onConnected`/`onDisconnected`, and surfaces failures using `ValueUtil.CxrBluetoothErrorCode`

### Requirement: Rokid Bluetooth Connection Handling
The Glasses tab SHALL connect to Rokid glasses over Bluetooth using the provided socket UUID and MAC address, reflecting connection lifecycle events and errors in UI state.

#### Scenario: Connect with lifecycle callbacks
- **WHEN** the app has obtained `socketUuid` and `macAddress` for a Rokid glasses device
- **THEN** it calls `CxrApi.getInstance().connectBluetooth(context, socketUuid, macAddress, BluetoothStatusCallback)`, marks connected on `onConnected`, handles `onDisconnected` by updating status, and surfaces `onFailed` errors (including `ValueUtil.CxrBluetoothErrorCode` details) to the user

### Requirement: Rokid Bluetooth Auto-Reconnect
If a Rokid glasses device is persisted and was previously connected, the app SHALL attempt to reconnect automatically on app start/resume when Bluetooth and permissions are available.

#### Scenario: Auto-reconnect on app start/resume
- **WHEN** the app starts or resumes and a Rokid glasses device with saved connection identifiers is present
- **THEN** the app checks Bluetooth permission/enablement, and if available, reuses stored identifiers to call `connectBluetooth`, surfacing success/failure in the UI; if unavailable, it prompts to enable/allow before attempting reconnect

### Requirement: Rokid Connection Status Reporting
The Glasses tab SHALL surface the current Rokid Bluetooth connection status and gate actions based on whether a device is connected.

#### Scenario: Read connection status
- **WHEN** the Glasses tab needs to display or validate connection state
- **THEN** it reads `CxrApi.getInstance().isBluetoothConnected` to determine connected vs disconnected, updates the UI accordingly, and prevents connect-only actions when disconnected

### Requirement: Connected Glasses Info Card
When glasses are connected, the Glasses tab SHALL display an info card with the device name, connection status, and all available metadata from the connection callback.

#### Scenario: Show connected device info
- **WHEN** a Rokid glasses device is connected
- **THEN** the Glasses tab shows a card with device name and status, and surfaces all available metadata from connection callbacks (e.g., Rokid account, glasses type, socket UUID, MAC address)

### Requirement: Rokid Bluetooth Deinitialization
The Glasses tab SHALL provide a way to cleanly disconnect from Rokid glasses and release Bluetooth resources when the app exits or the glasses entry is deleted, not during routine tab navigation.

#### Scenario: Deinit connection
- **WHEN** the app is exiting or a Rokid glasses entry is deleted
- **THEN** the app calls `CxrApi.getInstance().deinitBluetooth()` to tear down the connection and release resources; normal tab navigation keeps the connection active

### Requirement: Glasses Integration Options
The Glasses tab SHALL expose toggles for enabling or disabling Rokid glasses integration features and persist the user’s choices.

#### Scenario: Persist glasses integration options
- **WHEN** the user enables or disables a glasses integration option (e.g., use glasses input/output in chat)
- **THEN** the selection is saved and the new setting is applied to the relevant glasses-integrated chat behaviors
