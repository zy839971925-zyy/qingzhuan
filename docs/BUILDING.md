# 构建与签名 / Building and signing

## 环境

- JDK 17 或更高版本（建议 JDK 21 / Android Studio JBR）。
- Android SDK Platform 36、Build Tools 36.0.0；Gradle 8.13 由 wrapper 管理。
- Python 3 用于仓库根目录构建脚本。APK 不需要 Node、Expo、NDK、CMake 或模拟器。
- 首次构建需要网络访问 Google / Maven 依赖仓库；`setup_tools.py` 只检查环境，不会下载 SDK。

```sh
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
python3 setup_tools.py
```

## 正式版

当前 1.5.4 正式包使用独立应用 ID `cn.qingzhuan.toolbox`、versionCode `1` 和 versionName `1.5.4`，可与旧包 `cn.qingzhuan.app` 和预览包 `cn.qingzhuan.app.preview` 并存。Java/Kotlin namespace 仍是 `cn.qingzhuan.app`。

正式 APK 必须使用专用发布 keystore。构建前设置 `QINGZHUAN_RELEASE_KEYSTORE`、`QINGZHUAN_RELEASE_STORE_PASSWORD`、`QINGZHUAN_RELEASE_KEY_ALIAS`、`QINGZHUAN_RELEASE_KEY_PASSWORD`，从本地安全的密钥管理方式加载；不要把口令写进命令历史、仓库、日志或聊天，也不要上传 keystore 到公开仓库。缺少签名配置时，根脚本会拒绝 release 构建，不会退回 Android debug key。

```sh
python3 build.py --variant release
# Qingzhuan-Toolbox-1.5.4.apk
```

同一 application ID 的后续版本必须继续使用这把 keystore；备份丢失或泄露都会影响后续更新，应在独立安全位置备份。APK 发布前核对包名、版本、签名、ZIP 对齐、资源/DEX 和文件哈希。

## Debug 与历史预览

`python3 build.py --variant debug` 仅用于本机调试，不能当作正式分发包。1.5.4-preview 的 APK 使用不同的包名和开发证书；旧预览安装更新需要原预览 keystore。当前正式包不是对预览包签名升级，而是独立的新应用。

历史 1.5.3/1.5.4-preview 构建记录保存在 `docs/RELEASE_1_5_3.md`、`docs/RELEASE_1_5_4.md` 与 `handoff/`，不代表当前正式包已重新构建或设备验收。

## 测试

```sh
python3 scripts/check_image_policy.py
python3 scripts/check_localization.py
cd android
./gradlew :app:testReleaseUnitTest --tests cn.qingzhuan.app.Refined154Test
./gradlew :app:assembleRelease
```

前两项为源码策略/本地化检查；Robolectric 测试是在 JVM 的 Android 28 shadow 环境执行，不是模拟器或真机验收。Release assembly 需要所有 `QINGZHUAN_RELEASE_*` 签名变量。若出现 dex/resource 重复，保留日志后可清理构建目录并重试，不要删除源码、结果或 keystore。

## English quick guide

Use JDK 17+ (21 recommended), Android SDK Platform 36 and Build Tools 36.0.0. `setup_tools.py` only checks the toolchain. The production app ID is `cn.qingzhuan.toolbox`, distinct from both the old app and the preview. Set all four `QINGZHUAN_RELEASE_*` variables from a secure local secret manager before `python3 build.py --variant release`. The script refuses unsigned/debug-signed release builds. Keep the keystore backed up securely; never commit keys or passwords. Run the two targeted checks and Gradle unit test before release; Robolectric is not device acceptance.
