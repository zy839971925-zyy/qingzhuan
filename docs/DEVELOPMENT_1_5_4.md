# 1.5.4 development / recovery ledger

Baseline restored from final1.5.3 source checkpoint version16, commit3581822e1263b4a91653feeba556aded2c5bdc74. Preview signing certificate restored separately; originals/history preserved. Runtime workspace had been cleared; checkpoint recovery succeeded.

Scope: selectable light/dark photo overlay intensity; remove redundant format-card subtitles; default image conversion preserves original dimensions with highest encoding quality; opt-in advanced original/4K/1080p/720p/custom resolution and quality; short gesture hint at the bottom of dome, sharing drag blur/opacity; automatic/manual Chinese and English throughout UI; elastic multilayer card feedback; necessary checks only, no emulator.

Evidence: front-end defaults edge2048/quality88 and Engine.image clamps edge4096; both must change. Preserving source resolution is not preserving file bytes or guaranteeing lossless JPEG. Video reference inspected locally (WeChat-like stacked card fan and elastic release). Existing pager only has uniform scale and edge elasticity; add spring-lagging fan response during normal paging. Screenshots/video remain private scratch inputs, excluded from source/delivery.

Design: retain Compose/Backdrop/Java engine and FGS. Advanced image options are collapsed/disabled by default, independent of PDF-raster options. 4K/1080p/720p fit oriented bounding boxes3840×2160/1920×1080/1280×720 without upscaling; custom can lock aspect (fit) or explicitly stretch. Reject excessive bitmap memory demand rather than silently shrinking originals. Overlay strength is persisted separately for light/dark themes,0–100%; low contrast at very low strength is an explicit user choice.

Language: persistent system/zh/en preference, Android13+ public per-app language API with locale/layoutDirection config handling; older Android uses same app preference. One bilingual catalog and explicit presentation boundary cover Compose, accessibility and native notices/notifications; user filenames/document content and stored engine state are not translated. Original canonical engine errors remain unchanged and are localized at presentation.

Documentation: inspected user's tingxiejian-android README onGitHub; follow its clear feature/usage/build/architecture/privacy/limitations structure, with Chinese/English README and accurate module/state-flow docs. At source-archive time, repository publication or push had not yet been requested; publication was authorized later for this update.

Stages: [x] image/overlay/hint/hierarchy, [x] bilingual UI/elastic deck, [x] necessary checks/review/release/package. At each significant stage commit and durably replace canonical source checkpoint; keep exact check outcomes and remaining work. Current stage: implementation, bilingual docs and final Release/package checks complete. This archive is the final source checkpoint; successful persistence is established by the file write receipt/version metadata. Physical-device acceptance remains pending. No device validation claimed.

## Checkpoint1: original image policy / hierarchy
Removed old image-only4096 cap and default2048 resizing, added real original/preset/custom geometry afterExif orientation, full-size memory refusal with explicit recoverable error, highest default quality and explicit WebP lossless onAPI30+. Bounded CPU decode sampling only for requested smaller sizes. Advanced panel collapsed/off by default, PNG quality control hidden because lossless. PDF raster options remain separate. Added persisted per-theme photo overlay0–100%, removed format-card subtitles, bottom dome hint shares drag alpha/blur.

Actual check: scripts/check_image_policy.py,12 production-policy assertions passed (original>4096, portrait/presets, no-upscale, custom lock/stretch, Exif sample, invalid dimensions/quality and pixel cap). Not an image decoder/device test. UI has not yet been compiled.

## Checkpoint2: bilingual UI / elastic stack
Added system/zh/en app language choice, public API33 LocaleManager plus locale/layoutDirection config handling to preserve home selection and active task; older API preference fallback. UiText/ui/Glyph are explicit localization boundaries; native Toast, diagnostics labels, notification channels/progress and media controls use same catalog. Actual filenames, document contents, source session/error data and licenses stay unchanged. English catalog includes dynamic counts/progress patterns; patterns precompiled once with bounded cache. Android8-compatible stream reading is used (not InputStream.readAllBytes).

Stack fan now adds spring-lagged per-depth displacement/rotation, alongside direct pager following and existing edge rebound; single file remains static. User video is an appearance/interaction reference, not proof of pixel/curve equivalence.

Actual checks:12 geometry assertions and8 real Java TextCatalog assertions passed, static Kotlin UI phrase catalog coverage passed. Added two narrow Robolectric checks for real image Engine dimensions with legacy edge present and Android28 manual language/catalog/filename preservation; not yet run. No Android emulator or device.

## Checkpoint3: fresh review and compile correction
Fresh read-only review found image default quality still reading legacy quality, shared PDF-raster quality leakage, completion filename routed through translation, several accessibility bypasses, multi-file metadata and native error/progress catalog gaps. Corrected all: explicit qualityFor(image/raster) and saveBitmap quality argument, raw Text for names, translated count fragment/semantics, expanded native catalog and cause-only nested localization (filenames untouched). Added quality-isolation assertions to the real Engine JVM check and native static-error/call-site audit.

First compile failed2m10s in UiText: resolved Material3 Text has autoSize before fontSize, so positional forwarding did not match. Changed the wrapper to named arguments; no dependency/version rollback. Exact build-154-compile.log retained. This is an actual failed attempt, not a passed build. Next: run two targeted JVM tests and assembleRelease.

Checks after fixes:12 image-policy and9 real Java matcher assertions pass; static Kotlin phrase/native error/call-site coverage checked. Android/JVM integration and APK still pending at this checkpoint.

## Checkpoint4: targeted integration / release build passed
Command :app:testReleaseUnitTest --tests cn.qingzhuan.app.Refined154Test :app:assembleRelease --no-watch-fs -Pkotlin.incremental=false → BUILD SUCCESSFUL3m12s,60tasks. Actual XML:2tests,0failures,0errors,0skipped. On Android28 JVM shadows the actual Engine retained5000×20 despite legacy edge2048 and quality20; image/raster quality isolation asserted. Actual asset-backed AppText manual en/zh plus Chinese filename preservation passed. This is JVM evidence, not a real Android device or encoder-fidelity benchmark.

Next: finish bilingual README/architecture/build docs; update license/version asset heading; final asset-only release build, APK signature/version/ELF/resource/archive checks and durable delivery. No application Java/Kotlin changes planned after successful build74dded3.

## Final checkpoint
Final asset-only Release succeeded43s,50tasks. APK identity/signature matched existing preview key; version12, English/Chinese labels/catalog, native/media classes, six decoded preset PNGs,16KiB ZIP alignment and ARM64 ELF load alignment passed. SHA2563ad0fcbff7a0121b91bfc15ade1736181c27e0940c8e09fe3e02fb5c6074c6f6,15,204,145bytes. README/README.en, ARCHITECTURE/ARCHITECTURE.en, BUILDING, CHANGELOG and release/recovery records updated and links checked.

APKbuildSourceCommit46c2725 contains the final assets and tested app source; archive commit may additionally contain these factual documentation updates only. The final validation asserts no android/app/native-assets differences. Source checkpoint includes exact2-testXML, all current build logs, Git history and matching APK record; key and user reference media excluded.

Final review: all six actionable findings addressed; no remaining reviewer minor deferred. Device-only visual/launch/locale/OEM/TalkBack/touch/FPS and broad codec fidelity remain explicitly pending. Full historical regression was intentionally not rerun per user minimal-check request.
