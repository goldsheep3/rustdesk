# Android physical keyboard capture

The Android client can optionally use the existing RustDesk Input accessibility
service to capture physical keyboard events before Android handles system
shortcuts. The switch is off by default and applies only to the current remote
desktop page.

## Using the switch

1. Connect to a remote desktop with keyboard control permission.
2. Tap the keyboard-capture icon in the bottom toolbar. Its tooltip is
   `Win / Alt+Tab / Esc`.
3. If the RustDesk Input accessibility service is not enabled, Android's
   accessibility settings open. Enable the service manually, return to the
   session, and tap the switch again.
4. The icon turns amber when capture is selected. Physical keyboard capture is
   requested only while the remote input view is focused, the app is in the
   foreground, and the session permits keyboard control.
5. Tap the icon again to turn capture off. Closing the remote page also disables
   it. Backgrounding or losing input focus releases captured pressed keys.

Disable the separate keyboard diagnostic app's accessibility service before
testing RustDesk. Android can grant key-event filtering to only one accessibility
service at a time; another enabled key filter can prevent RustDesk from receiving
the events.

Captured events are passed into the existing Flutter keyboard input path, not
injected into Android globally. Returning `true` from the accessibility callback
prevents the same original event from also reaching the ordinary Activity path.
The physical Escape key reported by Xiaomi as Android Back with scan code 1 is
converted to Escape; other Back events are not rewritten.

## Verification

On a remote Windows desktop, verify Win alone, Win+E, Alt+Tab (including repeated
Tab while Alt is held), and Escape. Check that the corresponding local Android
actions do not also occur. Then verify ordinary text, left/right modifiers,
shortcuts, key repeat, toolbar dialogs, chat input, view-only mode, backgrounding,
disconnecting while a key is held, and capture off.

The Flutter regression tests cover requesting capture only for an opted-in,
focused, connected controlling session and releasing it when those conditions
are lost. The user verified the built client on the Xiaomi 24018RPACC tablet:
Win, Win+E, Alt+Tab, Escape and restoring local shortcuts with capture off work.

No server, wire-protocol, JNI export, or generated bridge changes are required.
Operating-system shortcuts reserved before accessibility filtering may remain
unavailable on some Android versions or devices.

## Local build verification

The ARM64 Rust library and full debug APK were built successfully with Rust
1.75.0, Flutter 3.24.5, Java 17 and Android NDK r28c. APK signature verification
passed, and the package contains the ARM64 Rust library and accessibility service
configuration. The three Flutter capture-gating tests passed. The APK was
installed successfully over the existing installation and launched without a
startup crash. The user confirmed the requested behavior on the physical tablet
and accepted the implementation as complete.

## Known limits

On the tested Xiaomi tablet, Win reaches the remote device but also triggers an
additional local system action. This implementation does not suppress that
Xiaomi behavior; the user accepted it as a known platform limitation.

The debug APK uses `com.carriez.flutter_hbb` with a local debug signing key. It
cannot update an official installation signed with a different key. Preserve the
existing installation and its data if Android reports a signature conflict.

## Regression surface

- `InputService.kt`: adds physical-key filtering and cleanup to the existing
  accessibility service; the filter flag is requested only when capture is on.
- `MainActivity.kt`: attaches the capture channel and releases captured keys on
  focus loss, pause and destruction, so remote modifiers do not stay pressed.
- `accessibility_service_config.xml`: declares the capability required for
  Android to deliver filtered key events.
- `strings.xml`: explains the new optional keyboard use of accessibility.
- `remote_page.dart`: adds the opt-in switch and gates capture on remote focus,
  lifecycle, connection and keyboard permission. Capture defaults off.

These are the only existing files changed. The keyboard capture implementation
and channel wrapper are new feature-specific files. Existing Rust, JNI, protocol,
generated bridge and non-Android input paths are unchanged.
