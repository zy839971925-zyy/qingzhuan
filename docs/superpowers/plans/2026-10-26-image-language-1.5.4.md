# Image fidelity / bilingual UI1.5.4 plan

Spec and rolling ledger: docs/DEVELOPMENT_1_5_4.md. Execute inline under user authorization, minimal checks/no emulator.

- [x] ImageTransformPolicy pure geometry + real Engine decode/Exif/size/quality; advanced ImageOptionsPanel, safe defaults. Necessary host tests: original>4096, portrait/landscape presets, custom aspect/stretch, invalid/pixel limits.
- [x] Photo overlay per-theme persisted strength; remove control subtitles; move short localized hint into dome bottom with shared drag alpha/blur.
- [x] AppText bilingual catalog, presentation wrappers, locale automatic/manual persistence and Android per-app API; native preview/notifications/accessibility. Audit literal coverage and preserve file/content strings.
- [x] Spring fan lag and release dynamics in multi-file deck with lifecycle/reduced motion protection; static single preserved.
- [x] Fresh read-only review; build Release and necessary behavior/package/signature checks; accurate bilingual README/ARCHITECTURE/BUILDING/CHANGELOG/recovery docs; APK/install ZIP/source checkpoint saved.

Review: original image not silently reduced, legacy options restore, PNG quality truthfulness, Exif rotations, original-memory failure, language change mid-conversion/selection, unknown system languages, dynamic text/filenames, masked/unmasked contrast, small-screen hint, fast/reverse/cancelled pager motion.

Exact verification and device-only pending items: docs/RELEASE_1_5_4.md.
