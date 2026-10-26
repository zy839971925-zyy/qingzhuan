<p align="center">
  <img src="docs/assets/qingzhuan-icon.svg" width="104" alt="Qingzhuan Toolbox app icon">
</p>

# Qingzhuan Toolbox · 轻转工具箱

<p align="center">Convert, preview and organise everyday files on Android.</p>

<p align="center"><a href="README.md">简体中文</a> · <a href="README.en.md">English</a></p>

<p align="center"><a href="https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/tag/v1.5.4-preview">Download the 1.5.4 preview</a> · <a href="https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases">All releases</a></p>

Qingzhuan Toolbox is a local-first Android file converter that brings together tools for images and PDFs, documents and spreadsheets, video and motion photos, audio, and archives. Files are processed on your device rather than uploaded to an online conversion service. Android system pickers let you choose what to import, where to save it, and what to share.

The current source tree corresponds to **preview version 1.5.4-refined**, package ID `cn.qingzhuan.app.preview`, `versionCode` 12. It is a separate app from the older production package `cn.qingzhuan.app`; do not uninstall the older app or clear its data to install this preview. The Gradle `release` variant in this source still uses the preview package ID and development signing key; it is not a production-store-signed build.

## What you can do

| Task | Tools | Important limits |
| --- | --- | --- |
| Convert images and PDFs | JPG, PNG and WebP conversion; images to PDF; export PDF pages as images; merge or split PDFs | Transparent images are flattened onto white when converted to JPG; existing PDF digital signatures are not preserved |
| Work with documents and tables | Text conversion among TXT, HTML, DOCX and PDF; table conversion among XLSX, CSV, TSV and JSON | Document conversion rebuilds text rather than preserving full layout or images, and does not provide OCR. Tables read the first worksheet; formulas are not calculated and styles are not preserved |
| Process video and motion photos | Video to MP4, compression/encoding, frame capture; video or motion photo to GIF; extract available Motion Photo / Live Photo video | H.264, HEVC and AV1 depend on device encoders. GIF has no audio and is limited to 60 seconds; a still image cannot restore missing motion data |
| Handle archives and audio | Create ZIP and TAR.GZ archives, extract supported archives, extract M4A or WAV audio, calculate SHA-256 / MD5 checksums | ZIP supports AES passwords; TAR.GZ does not. RAR5, split RAR archives and MP3 encoding are not supported |
| Review and manage batches | Import multiple files, view thumbnails and native previews, save/share/delete in batches | Actual support depends on file contents and device capabilities |

For same-format batches, redundant source-format destinations are hidden; mixed batches show only operations supported by every selected item. The app can preview images, PDFs, media and bounded document content. Office previews show readable content but do not promise full layout, graphics or formula fidelity.

## A typical workflow

1. Select files from Photos or the system file picker. Cancelling a selection does not clear existing inputs.
2. Review thumbnails; swipe through a batch or open the native preview.
3. Choose an output format and adjust options for the current tool.
4. Press and slide the glass cover towards the centre, then release to start conversion. The completion mark appears only after a real successful result.
5. Preview the output before saving. Images and videos go to the system media collection by default; other files can be saved to a location chosen in the system picker. Results can also be shared or deleted in batches.

A foreground service continues work during ordinary backgrounding. Force-stop, process death or system execution limits may interrupt encoding; completed outputs are retained and unfinished work is marked interrupted.

## Image output

By default, the app keeps the source dimensions after orientation correction and uses the highest encoding quality. Turn on Advanced options to choose Original, 4K, 1080p, 720p or custom dimensions, and adjust JPEG / WebP quality independently. Presets keep aspect ratio and do not upscale. With aspect-ratio preservation disabled, custom dimensions can match exactly but may stretch the image.

Dimensions are limited to 1–32768 per side and 64 megapixels, subject to available memory. If the requested output cannot be handled, the app reports a clear failure rather than silently downscaling it.

“Highest quality” does not mean byte-for-byte preservation: JPEG remains lossy, and image processing uses an orientation-corrected 8-bit sRGB bitmap without guaranteeing full EXIF, ICC, HDR or wide-colour-gamut retention. Transparent images are flattened onto white for JPEG; the image tool processes only the first GIF frame. Highest-quality WebP can use a lossless path on Android 10 and later; the older Android 8/9 encoder is not guaranteed lossless.

## Appearance and language

Choose system, light or dark theme; select the system language or switch between Chinese and English. Backgrounds include an animated glow, original wallpapers, solid colours, HEX values and a user-selected photo, with separate light and dark overlays. Full RuntimeShader refraction requires Android 13 / API 33 or later; older systems use a visual fallback while retaining conversion features. System animation-reduction or battery-saving settings may reduce motion.

## Version and device requirements

- [Download the 1.5.4 preview APK](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/tag/v1.5.4-preview) or [browse all releases](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases). Preview and production builds are identified separately.
- Minimum configured version: Android 8.0 / API 26. Full glass refraction requires Android 13 / API 33 or later.
- Video encoding depends on hardware and vendor encoders available on the device; listing a codec does not guarantee every device, resolution or parameter combination will work.
- Version 1.5.4 has build and package checks recorded. Physical-device launch, visuals, touch behaviour, TalkBack, background behaviour and performance still need device acceptance. A successful build or package check is not device acceptance.

## Build from source

Requires JDK 17 or later (JDK 21 recommended), Android SDK Platform 36, Build Tools 36.0.0 and Python 3. The first build needs network access to Google / Maven dependencies.

```sh
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
python3 setup_tools.py
python3 build.py --variant release
```

You can also open `android/` in Android Studio. The current source's Gradle `release` variant still uses `cn.qingzhuan.app.preview` and a preview development key; it does not create a signing identity that can update the older production app. A production release needs a signing configuration and key matching its intended application ID. Never commit signing keys to the repository.

More detail: [Architecture](docs/ARCHITECTURE.md) · [Build and signing](docs/BUILDING.md) · [1.5.4 validation record](docs/RELEASE_1_5_4.md) · [Device acceptance plan](docs/DEVICE_TEST_PLAN.md) · [Current checkpoint](docs/CURRENT_CHECKPOINT.md)

## Repository layout

- `android/`: current Android app and Gradle build entry point.
- `app/`: conversion engine, task sessions and platform bridge.
- `docs/`: architecture, build guide, release notes, validation plans and license review.
- `libs/`, `licenses/`, `THIRD_PARTY_NOTICES.txt`: bundled dependencies and notices.
- `tests/`: test source, fixtures and check scripts.
- `src/`, `App.tsx` and npm configuration contain historical implementations or test material; the current APK is built from `android/`.

## Privacy and licensing

The app requests neither INTERNET nor broad storage access. It keeps inputs, outputs, backgrounds and diagnostics in app-private storage; media uses MediaStore, documents use the system picker, and sharing grants access only to the selected output URI. Limits are up to 20 input files, 512 MiB per file and 1 GiB total. Private app data is not cloud-backed up; export it before uninstalling.

The project declares GPL-3.0-only. The existing license review records unresolved compatibility between GPL and the Junrar / UnRAR terms when distributed together, so distribution of the complete APK should not be described as cleared. Kyant is Apache-2.0; original wallpapers and generators are CC0. See the [license review](docs/LICENSE_REVIEW.md), [`licenses/`](licenses/) and [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt). This is not legal advice.
