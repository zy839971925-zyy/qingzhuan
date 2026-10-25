# 1.5.3 development / recovery ledger

Baseline: 660c1bfc29de80c765ce3cc2bb87cbfa3f530447, working 1.5.2 release; original history retained.
Current stage: 1.5.3 release APK built and package-verified; this archive is the final source checkpoint. Its successful persistence is identified by the file version/write receipt, not assumed by the document. Physical-device acceptance remains pending. Source, screenshot and 7.924s reference video inspected. This is not device verification.

Workflow: user-selected Reasoning Workflow Portable 3.1.0 Ultra root and ultra-research overlay read from original repository. Reframed status-bar issue as a whole-window/material ownership issue; separate opaque panel layers and recropped background are evidenced in source, not just inferred from colors. Alternative Material dialog restyling retains separate window/bar complexities; chosen in-window host removes that boundary while retaining modal input/back/accessibility behavior.

Checkpoints: each significant stage commits code and replaces one rolling source ZIP with full Git bundle, recovery metadata, exact checks and pending work. Persist before cleanup; no automatic retry for uncertain remote writes. Original project and release artifacts remain preserved. Unbuilt stages are explicitly WIP.

Verification policy: minimum useful build/package checks. No Android emulator. No real device connected; visual fidelity, Android 15/17 launch, touch/FPS are not claimed verified.

## Checkpoint 1: sheet continuity
- Replaced separate Material sheet window with GlassSheetHost; real underlying scene is sampled once. Full panel includes handle/title/rows/navigation inset, with transparent ListItems. Entry, exit and section transitions implemented. Source inspection confirms no AmbientBackground inside NativeSheets and no ModalBottomSheet/AlertDialog left there.
- Delete confirmation is an animated in-panel confirmation; media actions and bounded file list retained. Parent content hidden from accessibility while modal is visible; Back and scrim dismiss the host.
- Actual check: :app:compileReleaseKotlin --no-watch-fs -Pkotlin.incremental=false → BUILD SUCCESSFUL in36s. This is compilation, not an APK or phone screenshot.
- Not yet device-validated: background registration, focus behavior, modal motion, Android15/17 system bars.

## Checkpoint 2: background choices
- Added solid color swatches/validated six-digit HEX, saved color/preset/mode/blur in one preferences edit, normalization of unknown mode/ID/nonfinite blur. Pure colors adapt softly to theme for contrast; this is explicitly explained in the setting.
- Three original bundled 720×1280 abstract PNGs plus135×240 thumbnails generated from analytic fields, no external/proprietary wallpaper input. Reproducible generator is included. Dynamic/default/photo backgrounds retained.
- Actual check: compileReleaseKotlin → BUILD SUCCESSFUL in28s; full log compile-153-background.log. Generator produced all three dimensions, mist viewed locally; no phone appearance/contrast claim.

## Checkpoint 3: elastic interaction
- Glass tabs enlarge on press/drag, velocity-driven bounded horizontal stretch, underdamped release to rest; scale labels, subtle panel give, positional spring. Direct drag remains immediate; cancellation/stop restores committed value and fresh interaction sources. RTL now uses absolute offset once.
- Multiple-image preview: spring snap (damping .68, stiffness320), bounded 32dp tanh edge travel with return spring (.62/300); nested scroll consumes only otherwise-unhandled horizontal deltas. Single file stays still. Reduced motion bypasses elastic travel and springs.
- Ruling: use NestedScrollConnection for residual edge motion instead of custom OverscrollEffect node; keeps built-in pager slop, click and accessibility ownership and avoids a version-sensitive node API.
- Sheet handle retains downward drag-to-close, short drags spring back. Confirmation remains inside the same frosted panel. Solid swatches use48dp wrapped touch targets.
- Actual checks: compileReleaseKotlin BUILD SUCCESSFUL27s (motion), then27s (interactions). Logs included in handoff. No device/FPS measurement. Next: fresh read-only review, package build/identity, final durable save.

## Checkpoint 4: review fixes; release build pending
Fresh read-only reviewer inspected660c1bf→1060eb8. Important: early-close transition shielding, keyboard focus escaping modal. Also found deletion confirmation below viewport and missing reduced-motion handling. All four addressed: active includes !isIdle; panel focusGroup cancels exit and underlying scene canFocus=false; confirmation appears above list and respects reduced motion. No additional concrete persistence, backdrop feedback or signing-configuration defect identified. These are reviewed source changes, not phone acceptance.

First assembleRelease compiled updated Kotlin but failed58s at mergeDexRelease: duplicate androidx.activity.compose.R in two generated project_dex_archive JARs. No application source defines R. Retain exact build-153-first.log. Next action: clean only app derived build outputs via :app:clean and rebuild; no source/dependency rollback based on this failure.

Obsolete local checkpoint-152, checkpoint-152-stage2 and1.5.2 WIP ZIP removed after checkpoint3 durable save succeeded; original restored source, release artifacts and full Git bundle retained.

## Final checkpoint / release
Clean :app:clean :app:assembleRelease succeeded1m42s (51tasks), resolving generated-resource DEX archive duplication. Existing signing certificate matched; APK package/version11/label/private components/no INTERNET/16KiB alignment/Dex classes and six decoded wallpaper resources passed. Packaging helper first assumed unshortened resource paths; corrected checker to resolve AAPT resource-table entries, with independent PNG decode. No product change was needed for that checker failure. APK hash and exact source/build identity are in handoff/APK-build-153.json.

README and build.py output name updated to1.5.3; no application source changed after the successful build at24c57e6. Source archive commit will include these documentation/output-name edits. buildSourceCommit stays24c57e6; archive commit is in RESUME.json.

Final review rulings: device-only visual/IME/TalkBack/system-bars/touch/update/FPS evidence remains pending; do not treat absent measurement as acceptance. All four concrete review findings addressed in one source pass and compiled in the clean release. No claim of UI regression tests or physical installation. User minimal-check policy retained.
