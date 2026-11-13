# Android debug build

In GitHub Actions, run the `Mobile debug build` workflow and select `android`.
The workflow builds `:apps:android:assembleDebug` and uploads the installable
debug APK as a separate artifact. Download that artifact from the completed run
to transfer and install it on an Android test device.
