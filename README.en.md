<p align="center"><a href="README.md">简体中文</a> · <strong>English</strong></p>

# Qingzhuan · 轻转

**Choose a file, choose a format, and swipe the glass dome to convert.**

Native Compose UI and real backdrop refraction, backed by the existing Java engine. Processing stays on device, without accounts or a server.

[Download the 1.5.4 test preview APK](https://github.com/zy839971925-zyy/qingzhuan/releases/tag/v1.5.4-preview) · [All releases](https://github.com/zy839971925-zyy/qingzhuan/releases)

Version **1.5.4-refined / code12**, package `cn.qingzhuan.app.preview`, retains the private development certificate used for1.5.2/1.5.3. The original `cn.qingzhuan.app` is separate; keep its app/data.

[Chinese guide](README.md) · [Architecture](docs/ARCHITECTURE.en.md) · [Build guide](docs/BUILDING.md) · [Release evidence](docs/RELEASE_1_5_4.md)

## What's new

| Feature | Behavior |
| --- | --- |
| Image defaults | Original oriented dimensions and maximum encoding quality, without hidden2048/4096 downscaling |
| Advanced options | Off by default; Original/4K/1080P/720P/custom size and independent JPEG/WebP quality |
| Background | Separate0–100% light/dark photo overlays, blur, colors and original wallpapers |
| Hierarchy | No redundant format subtitles; advanced parameters appear only when enabled |
| Language | System/中文/English for normal UI, accessibility, previews and common task notifications |
| Card stack | Direct paging, spring-lagged layers and damped edge rebound; single cards stay static |

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
python3 build.py --variant release
# Qingzhuan-1.5.4-refined.apk
```

Android Studio can open `android/`. Restore the separately backed-up preview key to update an installed preview. The source ZIP excludes private keys/APKs and includes Git history, checks and recovery metadata. The GitHub Release contains the supplied preview APK; source code is published separately. For this repository update, the supplied APK was used unchanged; at the user’s direction, Gradle/Android SDK tests and builds were not rerun. Historical package build records and this update’s independent checks are distinguished in the release record.

Necessary checks include12image-policy assertions,9actual Java catalog checks, static boundary checks and2Android28 JVM tests, followed by release/package checks. Exact evidence is in the release record. **No emulator or physical-device acceptance was performed.** Xiaomi Android15/17 launch, optical fidelity, touch and60/90/120FPS remain unverified.

There is no INTERNET or full-disk access permission. Private data is not a cloud backup; export important results before uninstalling. Third-party licenses remain independent: Kyant is Apache-2.0, historical Shader sources includeGPL-3.0, and existing GPL/Junrar compatibility concerns remain unresolved. Original wallpapers/generator useCC0. See the license review and `licenses/`.
