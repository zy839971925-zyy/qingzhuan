<p align="center"><a href="ARCHITECTURE.md">简体中文</a> · <strong>English</strong></p>

# Architecture · File to result

Actual1.5.4 implementation: Kotlin/Compose, Kyant Backdrop1.0.6 and the existing Java engine/session. Historical RN/WebView sources are not built. MinSDK26, compile/targetSDK36.

`NativeHomeActivity` owns pickers/preferences/subscriptions/export routing. `NativeHome` owns selection and reversible presentation. `AppSession` owns task/files on a single worker. `Engine` and `NativeMediaEngine` produce staged outputs, then session state commits results before notifying UI. `SessionStore` persists requests/progress/unacknowledged outcomes. `ConversionService` manages notifications/liveness, not a second copy of task state.

## Motion and optics

Phases are EMPTY, READY, CONVERTING, RETURNING and SUCCESS. Chosen targets arm the dome. Shared progressp controls sphere geometry and content/hint fade/blur. Early release returns by spring; only valid central release submits. Accessibility uses the same submission entry.

The visible ring has a minimum duration; unknown engine progress is waiting feedback. Real success for the matching task ID is required for a checkmark. Persisted completion is independent of animation playback.

Controls sample a background-only layer; the sphere samples background plus scene while remaining outside capture. Selected controls combine independent text/background layers without sampling the lens itself. Sheets share the Activity window and actual scene backdrop across handle/header/rows/insets, rather than cropping another wallpaper. Input/Back shielding lasts through exit; focus cannot escape to background controls.

Image stacks keep native Pager touch/tap handling and add depth-dependent spring lag. Edge rebound only consumes residual horizontal scroll. Single cards stay static. Full shader optics require Android13+; earlier systems retain conversion and fallback rendering.

## Image policy

Pure ImageTransformPolicy resolves oriented dimensions and explicit requested bounds. Advanced off ignores legacyedge/quality, retaining original size and quality100. Presets fit without upscaling; custom can fit or stretch. Sampling is only for requested smaller targets. Pixel/heap guards fail explicitly instead of silently shrinking originals.

Image encoding and PDF raster quality are separate. PNG is lossless; JPEG100 is still lossy. Processing uses8-bit sRGB, without complete metadata/HDR/wide-gamut retention guarantees. Inputs stay untouched.

## Language and state

Android13+ uses public app locales; home handles locale/layoutDirection changes without clearing selection or conversion. Earlier Android uses stored preference. UiLanguageProvider/UiText/ui/Glyph and AppText are explicit presentation boundaries, including native notices/media controls. TextCatalog precompiles dynamic patterns and bounds its cache; filenames/user documents/original licenses/canonical engine state stay raw. Error causes can translate through bounded recursion.

BackgroundStyle persists mode, blur, revision, color, preset and independent light/dark overlays. Photo import uses bounded decode and atomic replacement. Wallpaper assets are original/reproducible.

Private data lives under `filesDir/inputs`, `filesDir/results` and `session-v2.json`. The foreground service continues ordinary background work with a timed wake lock; force-stop/process death can interrupt. It does not promise byte-level encoder continuation. MediaStore, SAF and controlled URI grants handle export/share. Internal preview/service components are not exported; there is no INTERNET or full-disk permission.

## Build and recovery

Host/Compose source: `android/app/src/main/java`; Java business source: `app/src/main/java`; assets: `native-assets`. Build uses JDK17+, Gradle8.13 and SDK/Build Tools36 without Node/Expo/NDK.

Pure policy/catalog tests, two targeted Android28 JVM tests and release/package checks provide distinct narrow evidence. They do not prove physical launch, optical fidelity, touch, codec fidelity or60/90/120FPS. Major stages are committed and durably checkpointed with a flat source tree, full Git bundle, precise metadata, logs/test XML. Private keys and user reference media are excluded; read the recovery ledger before resuming.
