# Native Compose migration implementation plan

User explicitly authorizes full architecture change and native Kyant Compose library; effects, simple builds and easy edits take priority. No further design selection or emulator.

Goal: native APK with fresh upload landing, visible vector file icons, reversible dome-to-orb conversion, minimum clock/check/return, selected folder-save and selected-delete.
Architecture: ComponentActivity + Jetpack Compose + actual io.github.kyant0:backdrop:1.0.6. Java AppSession/Engine/SAF remain. RN work preserved in prior commit and excluded from runtime. AGP8.12, Gradle8.13, Kotlin/Compose2.3.10, Compose UI1.10.3, SDK36. Library1.0.6 metadata verified minCompileSdk36; newer2.0.1 uses SDK37 and different KMP setup, so pin compatible native release.

- [ ] Pure Java regression tests for visual feedback and gesture gates, then native models/host.
- [ ] Replace build entry with Compose, preserve engine/session fixes, use native vector resources.
- [ ] Implement minimal warm/sage home, file/format/result transitions, real library backdrop, continuous gesture and guaranteed feedback.
- [ ] Native lists, options, themes, selected deletion, SAF directory save; cold and foreground reopen reset presentation only.
- [ ] Build and inspect APK; run engine/session tests; source/license/checkpoint save. Device startup, optical appearance and FPS remain unverified without physical device.

Review focus: stale terminal tasks; interrupted gesture; folder chooser process lifecycle/cancellation; selection invalidation; short startup interruption; Android<33 lacks real RuntimeShader optics and must be explicitly labelled limited.
