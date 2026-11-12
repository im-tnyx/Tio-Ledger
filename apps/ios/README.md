# iOS debug build

`apps/ios` contains the Kotlin framework and a minimal SwiftUI host. The Xcode
project is generated from `project.yml` with XcodeGen. The host starts the
shared bootstrap with `IosDatabaseDriverFactory` and displays `TioAppShell`.

The `iOS simulator and iPhone debug` GitHub Actions workflow builds a Debug app for the
iOS Simulator on a macOS runner, launches it, and uploads `TioLedger.app` and
a launch screenshot as artifacts.
That artifact is for the simulator; it cannot be installed on a physical
iPhone. The workflow also builds an unsigned device IPA. On Windows, install
AltServer and its Windows requirements, download the IPA artifact, connect the
iPhone by USB, unlock it, and accept Trust. In AltServer's tray menu, hold
Shift while clicking and select `Sideload .ipa…`; choose the downloaded IPA
and enter your Apple Account when prompted. AltServer signs and installs the
app. On iOS 16+, enable Developer Mode on the iPhone. A free Apple Account
signature expires after seven days, so directly sideloaded apps need to be
installed again after expiry. Installing AltStore too allows it to refresh
apps while AltServer is available. See the [AltServer Windows guide](https://faq.altstore.io/altstore-classic/how-to-install-altstore-windows)
and [AltServer IPA sideload notes](https://faq.altstore.io/release-notes/altserver).
The IPA is an Actions artifact and must first be downloaded to Windows.

Paid Apple Developer Program signing with a registered device is a separate
Ad Hoc distribution path. No signing material belongs in the repository.

The iOS shell is an initial build and launch path. iOS-specific settings,
notifications, and persistence for preferences need separate acceptance
testing before claiming feature parity with Android.
