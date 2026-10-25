# 轻转1.5.3 · Glass continuity

Package `cn.qingzhuan.app.preview`, versionCode11, versionName1.5.3-glass, label轻转预览1.5.3. Uses the same private development certificate as1.5.2. Do not uninstall the old original application/data; preview and original have different package IDs. Runtime remains Kotlin/Compose/Backdrop1.0.6 plus the original Java conversion/session/foreground service. No React Native, Expo, emulator or new conversion dependency added in this release.

## Implemented

- All home-owned menus/settings/import/options/history/result/license sheets now share an in-window GlassSheetHost. The true existing page backdrop is sampled through24dp blur and restrained lens/highlight; there is no recropped duplicate photo, opaque Material handle strip, opaque list-row slab, or second sheet window. The material includes handle/title/body/navigation inset. Android's system photo/file pickers retain their own system UI.
- Entry/exit slides and fades, section fade/translation/size transitions, interrupted open/close handling, scrim/Back/close button and downward handle drag-to-dismiss. Short handle drags spring back. Reduced motion bypasses animated transitions. Modal shielding lasts through exit; background semantics/focus are disabled while active and the sheet focus group prevents keyboard escape.
- Existing Aurora/default/photo backgrounds remain. Added validated six-digit RGB input,48dp color swatches, persistent preset/color settings, and three original soft abstract720×1280 PNG wallpapers (雾青/暖砂/鸢尾), with135×240 thumbnails. Generator is included; no scraped Apple/proprietary wallpaper. Solid colors softly adapt to light/dark mode to preserve readability; photo blur remains0–40.
- Glass segments retain real backdrop sampling. Press/drag adds subtle panel give and selected-lens expansion (+7.5% x /12% y at rest pressure), bounded velocity stretch up to16%, then elastic relaxation. Positional spring damping.68/stiffness460. Direct drag remains reversible; cancel/stop restores committed selection, fresh press sources prevent stuck contact, RTL offset is applied once.
- Overlapping image previews retain native preview taps, labels and page count. Single preview stays static; multiple previews use spring snap (.68/320), slight settling scale, and32dp bounded tanh edge motion with release spring (.62/300). Edge motion only consumes otherwise-unhandled horizontal scroll. Reduced motion skips elasticity; lifecycle stop cancels/reset travel.
- Delete confirmation appears above the file list, on the same material, with reduced-motion support. Existing bounded thumbnail lists, multi-select/delete/save, gallery export, native file/media preview, GIF/video/MotionPhoto tools, Java conversion state and background notifications are preserved; no engine changes were made in1.5.3.

## Actual verification

- Kotlin compilation after sheets: BUILD SUCCESSFUL36s; background28s; motion27s; interaction27s. Full available logs accompany source.
- Fresh read-only review660c1bf→1060eb8 identified interrupted-close shielding, keyboard focus escape, offscreen delete confirmation and reduced-motion mismatch. All four source issues were addressed before final build. This review is not phone evidence.
- First assembleRelease failed58s at mergeDexRelease: two generated project archive JARs contained androidx.activity.compose.R. A clean app build resolved it; no application dependency/source removal. Final `:app:clean :app:assembleRelease --no-watch-fs -Pkotlin.incremental=false` **BUILD SUCCESSFUL1m42s**,51 tasks. Exact first/final logs retained.
- APK signature verified against the existing private certificate SHA256f253392041358c9edff32e76590aaa8de02725b1806183ae6ab6ba3eb8173286; ZIP alignment16KiB, version/package/label, private preview/service components and absence of INTERNET permission verified.
- Packaged new UI/media classes and all six wallpaper resources verified. AAPT shortened file paths, so the check resolves the resource table and independently decodes each PNG at its expected dimensions. APK ZIP integrity verified. No broad regression suite rerun; previous release tests are historical evidence only.
- APK SHA256e6e2d37c7aeaa874aaea98e9bb761c2ab51d2eac494469117641e17779b017ba,15,176,409 bytes. Exact machine-readable record and signing output are in handoff.

## Pending / actual limits

No real Android device was connected and no emulator was installed. Installation/launch on Xiaomi Android15/17, repeated lifecycle/keyboard/TalkBack behavior, custom-photo registration at actual system bars, optical appearance under motion, GPU layering, touch latency and60/90/120FPS are **not device-verified**. Spring parameters adapt the supplied video; no pixel-perfect or measured motion-curve identity is claimed. Android13+ provides full RuntimeShader optics; older Android has the existing fallback.

Existing media limits remain: hardware/vendor encoder support governs HEVC/AV1; absent LivePhoto video cannot be recreated; force-stop/OS process kill is reported interrupted rather than promising encoder continuation. Office preview is bounded content preview. Existing GPL/Junrar distribution-license compatibility risk remains documented in LICENSE_REVIEW.md; this release does not resolve it. Private signing key and user screenshots/video are excluded from source.

## Recovery

Read DEVELOPMENT_1_5_3.md, CURRENT_CHECKPOINT.md, handoff/RESUME.json and APK-build-153.json. Source ZIP is a flat project snapshot plus full Git bundle and build logs. Each major stage was committed and durably saved into the same rolling source item. Obsolete temporary1.5.2 WIP copies were removed only after the newer checkpoint was successfully saved. Original source, releases and Git history remain intact. Restore the separately backed-up preview key to update an installed preview; a new key cannot replace it.
