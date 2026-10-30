<p align="center">
  <img src="docs/assets/qingzhuan-icon.svg" width="96" alt="Qingzhuan Toolbox icon">
</p>

<h1 align="center">Qingzhuan Toolbox</h1>

<p align="center">Convert, preview and organise everyday files on Android.</p>
<p align="center">On-device processing · Batch workflows · Personalised appearance</p>

<p align="center"><a href="README.md">简体中文</a> · <a href="README.en.md">English</a></p>
<p align="center"><a href="https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/download/v1.5.5/Qingzhuan-Toolbox-1.5.5.apk">Download Qingzhuan Toolbox 1.5.5</a> · <a href="https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases">All releases</a></p>

Qingzhuan Toolbox brings together tools for images and PDFs, documents and spreadsheets, video and motion photos, audio, and archives. Files are processed on your device rather than sent to an online conversion service. Choose files through Android’s system interface, preview the results, then decide where to save or share them.

## One toolbox for everyday files

| Type | What you can do |
| --- | --- |
| Images and PDFs | Convert JPG, PNG and WebP; turn images into PDFs; export PDF pages as images; merge or split PDFs |
| Documents and spreadsheets | Convert readable text in TXT, HTML, DOCX and PDF; convert XLSX, CSV, TSV and JSON tables |
| Video and motion photos | Convert or compress video, extract frames, create GIFs, and extract available video from Motion Photo / Live Photo files |
| Audio and archives | Extract M4A / WAV from media; create or extract ZIP and TAR.GZ archives; calculate SHA-256 / MD5 checksums |
| Batch work | Import several files, browse thumbnails and previews, then save, share or delete results in batches |

## A simple conversion flow

1. Import files from Photos or Android’s file picker.
2. Browse thumbnails; swipe through multiple files or open a native preview.
3. Choose an output format and adjust the settings for that tool if needed.
4. Press and swipe the glass panel upwards; release when it reaches the centre to start conversion.
5. Preview the result, then save it to your media library or a folder you choose, or share it.

Images default to their orientation-corrected original dimensions and the highest encoder quality. Advanced options include Original, 4K, 1080p and 720p presets or custom dimensions, plus separate JPEG / WebP quality controls. Image and PDF output settings stay independent.

## Designed for a more natural flow

The glass track stays in place while the active lens responds elastically. Format choices and menus enter in sequence with light press feedback. Thumbnails fade in as they load; multiple inputs and results can be browsed and managed individually. Nested settings panels have a clear Back action, and delete confirmation defaults to Cancel.

Follow the system theme or choose light or dark mode; the interface supports system language, Simplified Chinese and English. Pick an animated glow, built-in wallpaper, solid colour, HEX colour or your own photo as a background. Photo masks have separate light- and dark-mode settings.

## Download and compatibility

- [Download Qingzhuan Toolbox 1.5.5 APK](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/download/v1.5.5/Qingzhuan-Toolbox-1.5.5.apk) · [All releases](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases)
- Minimum Android 8.0 / API 26. Full glass refraction effects require Android 13 / API 33 or later.
- Video encoding uses the hardware or vendor encoders available on your phone; supported formats and settings vary by device.

<details>
<summary>Supported formats and important limits</summary>

- Transparent images converted to JPEG receive a white background. Merging, splitting or converting a PDF does not preserve its digital signature.
- Document conversion rebuilds readable text; it is not full-fidelity layout conversion and does not include OCR. Spreadsheet conversion reads the first sheet and does not calculate formulas or preserve styles.
- GIF output has no audio and is limited to 60 seconds. A static photo cannot recover motion data that is no longer present.
- ZIP supports AES passwords; TAR.GZ does not. RAR5, split RAR archives and MP3 encoding are not supported.
- Image dimensions can be set from 1 to 32,768 pixels per side, up to 64 megapixels, subject to device memory. JPEG is lossy; image output does not guarantee preservation of all EXIF, ICC, HDR or wide-colour-gamut data.
- A foreground service can continue work when the app is moved to the background, but force-stopping the app, process termination or system execution limits may interrupt conversion.

</details>

## Privacy and source

The app does not request the INTERNET or all-files-access permission. It uses Android’s system file picker for files you select and keeps inputs, results and app settings on-device. Media is saved through the system media library; sharing exposes only the result you choose. Limits: up to 20 input files, 512 MiB per file and 1 GiB total. Export anything you need to keep before uninstalling.

- [Architecture](docs/ARCHITECTURE.en.md) · [Building and signing](docs/BUILDING.md) · [Device test plan](docs/DEVICE_TEST_PLAN.md) · [Changelog](CHANGELOG.md)
- The project declares GPL-3.0-only. Compatibility between GPL and the Junrar / UnRAR terms for combined distribution remains unresolved. See [`licenses/`](licenses/) and [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt) for dependency notices.

The formal app uses application ID `cn.qingzhuan.toolbox` and can coexist with the older preview package. Updating an existing formal installation requires the same signing key. Never commit the signing key or password to the repository.
