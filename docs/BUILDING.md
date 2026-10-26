# 构建与签名 / Building and signing

## 环境

- JDK17+；建议JDK21/Android Studio JBR。本次APK及Android28 JVM检查使用JDK17。
- Android SDK Platform36、Build Tools36.0.0；Gradle8.13由wrapper使用。
- Python3用于根构建脚本。APK本身不依赖Python、Node、Expo、NDK、CMake或模拟器。
- 首次构建需访问Google/Maven依赖仓库；源包含既有 `libs/` 和许可，不含SDK/Gradle缓存。

Android Studio打开 `android/`，选择JDK、SDK后同步构建。命令行：

```sh
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
python3 setup_tools.py
python3 build.py --variant release
```

根目录 `setup_tools.py` 只做 JDK/SDK 预检，不会替你下载或安装 Android SDK。请先安装 Platform 36 与 Build Tools 36.0.0，并设置 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT`。源码 smoke 检查会依次使用 `JAVA_HOME`、包内开发 JDK、系统 `PATH` 上的 JDK。

也可写本地、不可提交的 `android/local.properties`：

```properties
sdk.dir=/absolute/path/to/android-sdk
```

然后 `cd android && ./gradlew :app:assembleRelease --no-watch-fs -Pkotlin.incremental=false`。产物为 `app/build/outputs/apk/release/app-release.apk`；根build.py另复制为 `Qingzhuan-1.5.4-refined.apk`。

## 签名不能丢

当前预览使用仓库外 `../private-signing/qingzhuan-preview.keystore`，或 `QINGZHUAN_DEBUG_KEYSTORE` 指定文件。Gradle配置的本地开发别名是androiddebugkey，开发口令android；这是预览构建方式，不代表生产发布密钥管理方案。

本次证书SHA256：

```text
f253392041358c9edff32e76590aaa8de02725b1806183ae6ab6ba3eb8173286
```

恢复同一私钥才能更新已安装同包名的预览版。根build.py在缺失时生成新的独立开发证书，不能借此覆盖不同签名。签名不在源码或Git bundle中；旧包名 `cn.qingzhuan.app` 的密钥不在本工程中，不要删除旧应用来规避签名问题。

## 最必要的检查

```sh
python3 scripts/check_image_policy.py
python3 scripts/check_localization.py
cd android
./gradlew :app:testReleaseUnitTest --tests cn.qingzhuan.app.Refined154Test
./gradlew :app:assembleRelease
```

前两项需要JAVA_HOME指向完整JDK。2项Android28 JVM检查使用Robolectric，没有启动安卓模拟器；首次可能下载对应Android测试框架。更高SDK的JVM测试可能需要JDK21，本次未运行它们，也未重跑历史整套UI回归。

若DEX报告生成资源R类重复，先保留日志，再 `:app:clean :app:assembleRelease --no-watch-fs -Pkotlin.incremental=false`；只清理构建产物，不能删除源码、结果或签名。资源/缓存失败不能当成设备崩溃证据。

交付时核对apksigner证书、versionCode/package/label、ZIP对齐、资源/DEX及文件哈希。源码包 `handoff/` 记录实际构建源提交、签名输出、构建日志和JVM测试XML。源码归档提交可以另含文档修改；应用代码差异必须检查为空。

## English quick guide

Use JDK17+ (21 recommended), SDK36 and Build Tools36. SetJAVA_HOME/ANDROID_HOME, run setup_tools.py then build.py --variant release, or openandroid/ in Android Studio. No emulator/Node/Expo/NDK is required. First dependency resolution needs network access.

Restore the separately backed-up preview keystore before updating an installed preview. A new key cannot replace another signature. The source bundle deliberately excludes private keys and APKs.

The two targeted Robolectric tests are JVM checks on Android28 shadows, not a physical device. Build/signature checks do not prove visual quality, launch on an OEM device or FPS. Exact evidence and remaining checks are in RELEASE_1_5_4.md.
