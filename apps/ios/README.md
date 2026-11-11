# iOS debug build

`apps/ios` contains the Kotlin framework and a minimal SwiftUI host. The Xcode
project is generated from `project.yml` with XcodeGen. The host starts the
shared bootstrap with `IosDatabaseDriverFactory` and displays `TioAppShell`.

The `iOS simulator debug` GitHub Actions workflow builds a Debug app for the
iOS Simulator on a macOS runner, launches it, and uploads `TioLedger.app` and
a launch screenshot as artifacts.
That artifact is for the simulator; it cannot be installed on a physical
iPhone. A device build still needs an Apple team, a signing certificate, a
provisioning profile for the device, and a separate signed build workflow.
No signing material belongs in the repository.

The iOS shell is an initial build and launch path. iOS-specific settings,
notifications, and persistence for preferences need separate acceptance
testing before claiming feature parity with Android.
