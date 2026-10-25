# Monochrome home implementation plan
User authorized direct implementation and delegated visual decisions; no further selection gate.
Goal: minimal dashed upload, monochrome native UI, real source Shader, complete clock-ring/check/return sequence even for instant conversions.
Architecture: application latches a conversion presentation independently of Java task completion; a UI-frame-driven Skia ring controls minimum visible time and completion acknowledgement. No fabricated numeric progress. Existing gesture, engine and crash diagnostics retained.
- [x] Add pure feedback state step + tests: instant/slow/unknown/error and frame timing.
- [x] Add same-canvas icons and ring, replace ornamental home with quiet upload/file/format layout.
- [x] Wire presentation latch and native dark/light surfaces; keep task IDs and data safety.
- [x] Run typecheck, compiled Worklets closure regressions, JVM tests; build/sign/inspect ARM64 APK. Save source checkpoint.
Review focus: instant terminal callbacks, background/foreground timing, stale task callbacks, cancellation during feedback, PanGesture serialization. No emulator. Android visual/FPS claims require device observations.
