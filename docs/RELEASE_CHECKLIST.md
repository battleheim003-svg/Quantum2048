# Release Checklist

Use this checklist before publishing to Google Play, Cafe Bazaar, or another Android store.

## Build And Versioning

- Confirm `versionCode` and `versionName` in `app/build.gradle.kts`.
- Produce a signed release AAB/APK with the real release keystore.
- Verify release signing variables are configured outside source control.
- Run `testDebugUnitTest`, `assembleDebug`, and the final release build task.
- Record final APK/AAB size.

## Device QA

- Test on at least one real Android phone.
- Test on at least one small-screen device or emulator.
- Test Persian RTL and English LTR flows.
- Verify first launch, main menu, tutorial, Classic, Quantum, Daily Challenge, Achievements, Statistics, Settings, About, and Privacy Policy.
- Verify app resume after closing during a daily challenge.
- Verify Reset Progress behavior.

## Store Readiness

- Capture real gameplay screenshots.
- Prepare feature graphic and promotional art.
- Replace `[Coming soon on Google Play]` in `README.md` after store approval.
- Review `store-listing/fa.md` and `store-listing/en.md` against final store character limits.
- Publish or host `PRIVACY_POLICY.md` at a public URL if the store requires one.

## Privacy And Permissions

- Recheck `AndroidManifest.xml` permissions before release.
- Confirm whether `INTERNET` and `ACCESS_NETWORK_STATE` are still required while offline adapters are active.
- Update `PRIVACY_POLICY.md` before enabling real ads, analytics, billing, Play Games, cloud sync, or online leaderboards.

## Assets

- Review adaptive icon on launcher backgrounds, dark mode, themed icon, and round icon.
- Verify splash screen on Android 12+ and older supported devices.
- Ask a designer to replace the current vector launcher art if a final brand asset is available.
