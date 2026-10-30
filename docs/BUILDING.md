# 构建与签名 / Building and signing

## 环境

- JDK17+；建议JDK21/Android Studio JBR。正常APK编译可用JDK17；本次Compose JVM检查用独立JDK21执行器。
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

也可写本地、不可提交的 `android/local.properties`：

```properties
sdk.dir=/absolute/path/to/android-sdk
```

调试包可直接运行 `python3 build.py --variant debug`，输出 `Qingzhuan-1.5.5-debug.apk`。正式版需通过环境变量提供私有生产密钥路径、口令和别名：`QINGZHUAN_RELEASE_KEYSTORE`、`QINGZHUAN_RELEASE_STORE_PASSWORD`、`QINGZHUAN_RELEASE_KEY_ALIAS`、`QINGZHUAN_RELEASE_KEY_PASSWORD`；然后运行 `python3 build.py --variant release`，输出 `Qingzhuan-Toolbox-1.5.5.apk`。不要把密钥、口令或本机私有路径提交进仓库。缺少任一生产凭据时Release构建会停止，不会使用debug证书替代。

## 生产签名必须保留

正式版应用ID为 `cn.qingzhuan.toolbox`，versionCode 2；它与正式1.5.4使用同一生产证书，故Android应用身份和签名升级链连续。正式密钥不在仓库、源码归档或APK内，必须从授权的私有存储取得，并仅通过本机环境变量交给构建工具。不要生成替代密钥，也不要把密码写进脚本、终端日志或仓库。新密钥不能更新已安装的旧证书应用。

开发APK可以使用独立调试签名；Release目标明确要求生产密钥，缺失时失败关闭。发行APK应单独核对包名、标签、versionCode、证书指纹和ZIP对齐。

## 最必要的检查

```sh
python3 scripts/check_image_policy.py
python3 scripts/check_localization.py
cd android
QZ_TEST_JAVA=/path/to/jdk21/bin/java ./gradlew :app:testReleaseUnitTest --tests cn.qingzhuan.app.Motion155Test --tests cn.qingzhuan.app.Refined154Test
# Configure the private release signing environment, then:
python3 ../build.py --variant release
```

前两项需要JAVA_HOME指向完整JDK。Compose UI回归在Robolectric Android35原生图形影子上运行，另保留2项Android28引擎/语言检查；没有启动安卓模拟器。Kyant Backdrop发布的JVM字节码为class65，因此UI测试执行器需要JDK21（QZ_TEST_JAVA），即使正常APK的D8/Gradle构建用JDK17也可成功。首次会下载Robolectric框架和原生图形支持。只运行上面必要测试，不默认跑历史整套端到端回归。

若DEX报告生成资源R类重复，先保留日志，再 `:app:clean :app:assembleRelease --no-watch-fs -Pkotlin.incremental=false`；只清理构建产物，不能删除源码、结果或签名。资源/缓存失败不能当成设备崩溃证据。

交付时核对apksigner证书、versionCode/package/label、ZIP对齐、资源/DEX及文件哈希。源码包 `handoff/` 记录实际构建源提交、签名输出、构建日志和JVM测试XML。源码归档提交可以另含文档修改；应用代码差异必须检查为空。

## English quick guide

Use JDK17+ (21 recommended), SDK36 and Build Tools36. SetJAVA_HOME/ANDROID_HOME, run setup_tools.py then build.py --variant release, or openandroid/ in Android Studio. No emulator/Node/Expo/NDK is required. First dependency resolution needs network access.

The stable application ID and production certificate must match the previous official release. Keep the production keystore outside source control and provide it through the four QINGZHUAN_RELEASE_* environment variables; a missing key fails closed. A new certificate cannot update an existing installation.

The selected Robolectric cases use Android35 Compose graphics/state and Android28 engine/locale shadows, not a physical device. UI tests require JDK21 for Backdrop class65; normal APK compilation may use JDK17. Build/signature checks do not prove visual quality, launch on an OEM device or FPS. Exact evidence and remaining checks are in RELEASE_1_5_5.md.

## 本轮构建环境恢复

源码包handoff/environment-bootstrap保留本轮非密钥恢复脚本。云端恢复时将其复制为源码目录的同级tooling/，restore_toolchain.py安装JDK17/Gradle8.13/SDK36；需要JVM UI检查时再运行restore_test_jdk.py取得JDK21执行器。run_gradle.py仅用于此环境代理/CA配置；普通Android Studio用户无需这些包装脚本。SDK路径与私钥位置必须根据本机重新设置。构建缓存不属于恢复源码，不打包。

若compileReleaseArtProfile报告Failed requirement，先保存完整错误与测试XML，清理派生build目录后执行 `:app:clean :app:assembleRelease --no-watch-fs -Pkotlin.incremental=false`；不要把失败APK当交付，也不要删除源码/私钥或以关闭诊断代替修复。该处置用于使生成DEX与ART profile从一致的干净输入重建，不代表已证明所有同名错误都由缓存引起。
