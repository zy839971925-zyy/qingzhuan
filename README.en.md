<p align="center"><a href="README.md">简体中文</a> · <strong>English</strong></p>

# Qingzhuan · 轻转

**Choose a file, choose a format, and swipe the glass dome to convert.**

Native Compose UI and real backdrop refraction, backed by the existing Java engine. Processing stays on device, without accounts or a server.

Current official stable release: **1.5.5 / versionCode 2**, package `cn.qingzhuan.toolbox`. Its package ID and signing-certificate fingerprint match official 1.5.4, and its version code is higher; installation over an existing copy and app-data migration were not tested on a device.

[Chinese guide](README.md) · [Architecture](docs/ARCHITECTURE.en.md) · [Build guide](docs/BUILDING.md) · [Release evidence](docs/RELEASE_1_5_5.md)

## What's new

| Interaction | Behavior |
| --- | --- |
| Glass pressing | Fixed underlying track; elastic deformation stays on the active lens |
| Formats and menu | Short staggered entrances and consistent spring feedback without delaying actions |
| Settings | Animated advanced/custom-size and background disclosure with inactive-content guards |
| Previews | Loaded thumbnails fade in; input and completed batches use the same browsable stack |
| Lists | Stable-ID entry, removal and placement transitions |
| Navigation | Explicit parent Back; deletion confirmation is cancelled first; Close dismisses the panel |

Original-size image defaults, advanced quality/size, per-theme overlays, bilingual UI and existing tools are preserved. The research record distinguishes source review from device acceptance.

## Use

Import from Photos or Files, review thumbnails/native preview, choose a target, and swipe the dome to the center. Early release returns by spring; reversing is supported. Real success is required before the checkmark. Images/videos save to Photos; other files use the system picker. Lists support select-all, batch save/delete and share.

Ordinary backgrounding uses a foreground service and persisted task state. Force-stop, process death and system limits can interrupt; byte-level encoding continuation is not promised.

## Image policy

Advanced off means original oriented size and quality100. PNG is lossless with no quality slider. PDF raster quality is independent. Presets fit oriented boxes3840×2160,1920×1080 or1280×720 without upscaling; non-16:9 images may not fill the box. Custom locked size fits; unlocked size may stretch/upscale. Dimensions are limited to1–32768 and64Mi output pixels plus a heap check. Excessive originals fail clearly instead of being silently reduced.

Maximum-quality JPEG is still lossy. Processing uses oriented8-bit sRGB, without a guarantee of full EXIF/ICC/HDR/wide-gamut retention. Transparent JPEG regions are white. Image conversion processes the first GIF frame. Android10+ can use maximum-quality lossless WebP; older legacy encoders are not claimed lossless.

## Existing tools

JPEG/PNG/WebP, images-to-PDF and PDF rendering/merge/split; trimmed video-to-GIF; video compression, mute, frame export and device-supported H.264/HEVC/AV1 MP4; compatible Motion Photo video or matching photo/MOV pairs; bounded text/first-sheet spreadsheet conversion; ZIP/TAR.GZ, supported extraction, checksums and AAC/M4A or WAV audio extraction.

No OCR, full Office layout/formula reproduction, MP3 encoding, RAR5 or multipart RAR. A still photo alone cannot restore missing Live Photo video. GIF is limited to60seconds with processing/output budgets. Unsupported video codecs are not silently substituted.

## Appearance and language

Choose System/Light/Dark and System/中文/English. Automatic language uses Chinese for Chinese phone languages and English otherwise. Filenames, user content, canonical session data and original license text stay untouched. Language changes do not restart conversion.

Backgrounds include Aurora, default, original wallpapers, HEX colors and a custom photo with blur. Light/dark overlay strengths are saved separately. Very low overlay can reduce readability. Reduced-motion/battery settings are respected. Full shader optics require Android13+; earlier Android keeps conversion and visual fallback.

## Build and evidence

JDK17+ (JDK21 recommended), SDK36, Build Tools36, Python3 and Gradle8.13. No Node, Expo, NDK, CMake or emulator.

```sh
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
python3 setup_tools.py
python3 build.py --variant debug
# Produces Qingzhuan-1.5.5-debug.apk
# Official releases require the private production signing credentials
# outside the repository: python3 build.py --variant release
# Produces Qingzhuan-Toolbox-1.5.5.apk
```

Download the [official 1.5.5 APK](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/download/v1.5.5/Qingzhuan-Toolbox-1.5.5.apk) or read the [release notes](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/tag/v1.5.5). Android Studio can open `android/`. Release signing requires the production keystore outside the repository; builds fail closed when it is unavailable.

Static checks, targeted JVM/Compose tests and production APK verification are recorded in the release notes. **No emulator or physical-device acceptance was performed.** Xiaomi Android15/17 launch, optical fidelity, touch and60/90/120FPS remain unverified.

There is no INTERNET or full-disk access permission. Private data is not a cloud backup; export important results before uninstalling. Third-party licenses remain independent: Kyant is Apache-2.0, historical Shader sources includeGPL-3.0, and existing GPL/Junrar compatibility concerns remain unresolved. Original wallpapers/generator useCC0. See the license review and `licenses/`.
