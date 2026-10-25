<p align="center">
  <img src="docs/assets/qingzhuan-icon.svg" width="104" alt="Qingzhuan app icon">
</p>

# Qingzhuan · 轻转

<p align="center">Convert and organise images, video, documents and archives on Android.</p>

<p align="center"><a href="README.md">简体中文</a> · <a href="README_EN.md">English</a></p>

Qingzhuan brings everyday media and file tools together in one Android app: convert images, work with PDFs, compress video, export motion-photo clips, extract audio and handle archives. Processing uses Android media capabilities and the app's local conversion engine rather than an online conversion service. Files are imported through Android's system picker; media is saved to its media collection, while other documents can be saved to a location chosen in the system picker.

This source snapshot is **preview version 1.5.3**. It retains the image and document tools, batch-file workflow and Java conversion engine, with customizable backgrounds, glass-style panels and continuous transitions in the home and preview UI. The current project is a source release candidate; no APK has been published as a final Release.

## What it does

| Task | Tools | Important limits |
| --- | --- | --- |
| Convert images and PDFs | JPG, PNG and WebP conversion; images to PDF; PDF pages to images; merge and split PDFs | Transparent images are flattened onto white when converted to JPG; PDF digital signatures are not preserved |
| Work with documents and tables | Text conversion among TXT, HTML, DOCX and PDF; table conversion among XLSX, CSV, TSV and JSON | Document conversion rebuilds text only: layout and images are not preserved, and there is no OCR. Table conversion reads only the first worksheet; it does not calculate formulas or preserve styles |
| Process video and motion photos | Video to MP4, compression/encoding, frame capture; video or motion photo to GIF; extract an available clip from Motion Photo / Live Photo | H.264, HEVC and AV1 depend on encoders actually available on the device. GIF has no audio and is limited to 60 seconds; a still image cannot restore missing motion data |
| Handle archives and audio | Create ZIP and TAR.GZ archives; extract archives; extract M4A or WAV audio from media | ZIP supports AES passwords; TAR.GZ does not. RAR4 extraction is supported, but not RAR5, split volumes or links |
| Check and organise files | SHA-256 / MD5 checksum text; batch selection, preview, save, share and delete | Supported operations depend on the file contents and device capabilities |

For same-format batches, source formats are removed from the destination choices. Mixed batches are routed by their shared supported operations. The app reads files explicitly selected through Android's system picker; media is written to the system media collection, while other documents can be saved to a location chosen in the system picker.

## Version and compatibility

- Source snapshot: `1.5.3-glass`; preview application ID: `cn.qingzhuan.app.preview`. This is a different package from the older production app. Do not uninstall the older app or clear its data just to install this preview.
- Minimum configured Android version: Android 8.0 / API 26. Full RuntimeShader glass effects require Android 13 / API 33 or later; older versions use a fallback.
- Video encoding depends on the hardware/vendor encoders available on the device. Listing a codec does not guarantee every phone, resolution or parameter combination will work.
- The source records build and package checks, but 1.5.3 has not been accepted on a physical device for visual fidelity, accessibility, touch latency or frame rate. A successful build is not device acceptance.

## Build from source

Requires Android SDK Platform 36, Build Tools 36.0.0 and JDK 17 or later (JDK 21 recommended). From the repository root:

```sh
python3 setup_tools.py
python3 build.py
```

Alternatively, open `android/` in Android Studio. The first build needs access to Google / Maven repositories. `build.py` defaults to the preview release variant; if no development signing key exists, it creates one outside the source directory. This key is not the older production signing key. Never commit it or any other signing private key.

See [`docs/VALIDATION.md`](docs/VALIDATION.md), [`docs/DEVICE_TEST_PLAN.md`](docs/DEVICE_TEST_PLAN.md) and [`docs/RELEASE_1_5_3.md`](docs/RELEASE_1_5_3.md) for test scope and recorded checks. Historical results do not mean a check was rerun for a new commit or verified on a device.

## Repository layout

- `android/`: current Android app and Gradle build entry point.
- `app/`: Java conversion engine, task sessions and platform bridge.
- `docs/`: architecture, version notes, validation plans and license review.
- `libs/`, `licenses/`, `THIRD_PARTY_NOTICES.txt`: bundled dependencies and their notices.
- `tests/`: test source, fixtures and runner scripts.
- `src/`, `App.tsx` and npm configuration include historical implementations or test material; the 1.5.3 APK is built from `android/`.

## Licensing and publication status

The project declares GPL-3.0-only. The supplied [`docs/LICENSE_REVIEW.md`](docs/LICENSE_REVIEW.md) records unresolved compatibility between GPL and the Junrar/UnRAR terms used by the app when distributed together. The UnRAR terms allow using it to handle RAR archives and distributing it with other software, while prohibiting reuse to recreate the RAR compression algorithm or develop a RAR-compatible archiver. This notice states that third-party restriction; it is not a GPL linking exception and cannot replace permission from the relevant rights holders or a supported license review. See the original third-party terms in [`licenses/`](licenses/) and [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt). This is not legal advice.

<!-- The displayed SVG is a documentation rendering of the bundled adaptive icon's exact background and foreground vector paths. -->
