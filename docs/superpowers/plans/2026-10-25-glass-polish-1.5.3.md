# Glass continuity 1.5.3 implementation plan

**Goal:** Fix fragmented app-owned sheets, add soft background choices, and make glass tabs and overlapping image previews elastic.
**Architecture:** Retain Compose / Backdrop 1.0.6 and Java conversion services. Replace separate Material sheet window with one animated, in-window frosted panel sampling the real full-page backdrop. No duplicated photo, no opaque handle/rows. All app-owned sheets use this host; system photo/file pickers remain system UI.
**Constraints:** No emulator. Only necessary checks. Same preview signing key and package. Preserve original source/history and media conversion/progress/gallery/preview functions. Frequent durable rolling source checkpoints, with exact commit, completed work and verification status. Never count source inspection as phone validation.

## Evidence and decisions
- Screenshots match three independent opaque surfaces in NativeSheets: Material handle/container, photo re-rendered at sheet dimensions, opaque ListItems. Activity transparent bars cannot configure the separate sheet window.
- Reference video inspected locally: selected capsule enlarges during contact and relaxes on release; continuous sliding is more important than adding a decorative click pulse.
- Kyant LiquidBottomTabs source uses press scale plus velocity deformation. Adapt the mechanism to current accessible choices, preserving direct drag and cancel restoration.
- PagerDefaults allows custom snapAnimationSpec; add spring snapping and bounded edge overscroll through a custom OverscrollEffect, rather than interfering with child taps.
- Original procedural abstract PNG wallpapers are bundled, with reproducible generator; no copied proprietary wallpapers. Add validated RGB input and curated solid palette. Keep current dark/light readability treatment.

## Tasks
- [x] 1. Shared in-window frosted sheet host, coherent handle/body/system bars, enter/exit and content transitions. Move deletion confirmation into this material. Check release compilation and source call graph.
- [x] 2. BackgroundStyle adds solid RGB and preset ID; persistent actions, curated palette/hex input and bundled original preset wallpapers. Validate colors/IDs at storage boundary, bounded decoding. Check generator dimensions and persistence roundtrip logic.
- [x] 3. Segmented control press, velocity stretch and spring release; lifecycle/cancel resets. Pager spring snap + damped edge movement; static single card and native preview preserved. Compile and review interruption/RTL/reduced motion.
- [x] 4. Fresh read-only whole-branch review, address material findings; assemble release, verify identity/signature/archive, deliver APK + install ZIP + source. Record real-device visual/FPS verification as pending.

## Review focus
Rapid open/close/navigation during animations; background changes while sheet visible; keyboard and long lists; gesture cancellation and fast reversal; RTL/large text/reduced motion; Activity stop while pressed; same-signature preview updates.

User explicitly delegated design decisions and requested minimal tests; do not insert design-approval pauses or broad emulator suites.

Final artifact/build facts and pending device acceptance: docs/RELEASE_1_5_3.md.
