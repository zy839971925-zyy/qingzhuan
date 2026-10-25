# Qingzhuan polished flow implementation plan

> Execute inline, user delegated decisions and opted out of questions. No emulator.

Goal: fresh upload landing on reopen; reliable vector file representations; elegant restrained warm/sage UI; real conversion with guaranteed clock/check/return; batch select/save/delete.
Architecture: preserve Java session and conversion engine, RN + same-canvas Skia optics. Replace SVG DOM icons with direct Lucide paths. Persist history but never resurrect historical presentation. SAF folder export for multiple files; manifest-first selected deletion. Android 15 failure remains unconfirmed without device log.

## Review focus
- Old complete snapshots must not become current success on cold launch; new imports and resumed active tasks must still work.
- Cancellation/errors must never reveal green success; late callbacks cannot acknowledge newer tasks.
- Selection must survive list changes without accidentally selecting new records; deletion must only remove manifest members.
- Folder chooser cancellation leaves originals intact; partial directory save reports count and removes unfinished destination only.
- Fast startup close must not create a permanent crash gate; real failures retain copyable report and recovery.

## Tasks
- [ ] 1. Add failing actual-App tests for cold launch, fresh import, direct result-save and selected bulk actions; add path validation/pixel icon test, startup recovery/session selection tests.
- [ ] 2. Implement direct path icons, warm neutral theme, subtle animated upload illustration, result icon with exactly two icon actions; improve transition timing without changing reference geometry.
- [ ] 3. Separate current presentation from persistent history in App.tsx; fresh launch stays empty; finish/again behavior verified.
- [ ] 4. Add selected deletion and folder save through bridge/module/AppSession, persisted picker inbox; selectable icon-based lists with select all.
- [ ] 5. Remove false interruption lock, add recovery and richer local exit diagnostics; investigate Android 15 compatibility against official docs/artifact inspections without guessed SDK changes.
- [ ] 6. Run TS/frontend/session/engine/desktop Skia visual checks; compile release APK and inspect signature, ABI, 16KB/DEX/resources. No device/FPS claim. Save source and deliverable.

Evidence limit: Product Design screenshot audit blocked by no connected device and explicit no-emulator instruction. Use source review and same-code desktop Skia frames, labelled as desktop rather than Android. Do not replace them with generated mockups.
