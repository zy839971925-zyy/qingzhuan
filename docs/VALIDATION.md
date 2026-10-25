# 当前1.5.2必要验证

Release编译成功。12项原生媒体写出/范围/参数断言通过，ffprobe读回循环/一次GIF均3帧32x24/250ms。APK签名/包身份/对齐记录见handoff/APK-build.json；Media3源码确认非默认VideoEncoderSettings强制编码。没有模拟器或真机测试，未重跑旧大回归与已过时UI测试。具体支持与未知项见RELEASE_1_5_2.md。

---

# 当前1.5.0验证范围

最新事实以CURRENT_CHECKPOINT.md及handoff/APK-build.json为准。71项Java断言、原生来源三段Shader桌面编译通过，Release APK构建成功。未连接真机，不使用模拟器。新相册/照片选择器/背景和UI未完成设备端到端验收；旧1.4 NativeFlowTest尚未适配新流程，不声称当前通过。

## 历史证据（不得混同为当前原生 UI 验收）

> 最新：1.3.0黑白首页、时钟进度/绿勾/返回已实现；21项前端测试及原生会话/诊断测试通过，APK构建签名通过。手机复验待完成，详见MONOCHROME_HOME.md。

> 更新：实机诊断已定位 PanGesture 被 worklet 闭包捕获的启动错误；1.2.2 已修改并通过编译闭包回归测试，等待手机复验。详见 PAN_GESTURE_STARTUP_FIX.md。以下保留历史调查。

> **最新实机结果：用户反馈1.2.0包点击即闪退，启动验收失败；根因尚未确认。** 以下历史编译/静态验证不能覆盖这一失败。当前1.2.1诊断构建的实际结果将在下文追加，问题追踪见 STARTUP_CRASH_INVESTIGATION.md。

# 轻转 1.2.0 RN 测试交付验证

日期：2026-10-25（上海时间）。用户已明确取消模拟器，不再启动模拟器。

## 已运行

- `npm run verify`：通过 TypeScript、当时 9 项 RN 契约、原版两段 SkSL 桌面 CanvasKit 编译与 14 帧背景畸变像素断言、Paragraph 透明度合成、25 项会话/探测/收件箱/导出缓存恢复断言、Java 引擎编译、20,066 项引擎断言、独立 openpyxl/lxml/pypdf 重读。日志 `logs/verify-delivery.log`。
- 此后补充中断动画交互锁修复，先运行复现失败，再修改并通过 TypeScript 和全部 13 项前端测试。日志 `logs/motion-lifecycle-red.log`、`logs/motion-final.log`。
- 中央提交补上实际球心距离门槛：p>=0.92 且球心到目标中心距离 <= 1.75 R1，避免横向偏移和过冲仍提交。手势纯逻辑测试检查反向移动即时改变 p、提前/取消释放回弹、不因高速提前提交、一次有效释放只提交一次。
- 修复 ReactActivity 宿主的 AppCompat 主题要求；主题回归测试曾失败，修改后通过。

## 构建状态

`assembleRelease -PreactNativeArchitectures=arm64-v8a` 已以退出码 0 完成，生成实际 RN/Skia APK：

- 文件：Qingzhuan-1.2.0-RN-Skia-test.apk；37778499 字节；Android 8.0/API26 及以上，64位 ARM 手机。
- SHA-256：`ec5b4c57db8991f123323d75a529a78db68c85b59ae1d063ba3d1e64c2dcaf62`。
- apksigner 验证通过，v2 签名；独立 Android Debug RSA 2048 证书。
- 证书 SHA-256：`274c21dff88bf964318a3c89f6717e5783b824c2e3cb3abdf14bde55634f5f2b`。
- zipalign 16KB 包内对齐与全部 15 个 ELF 的 PT_LOAD >=16KB 对齐检查通过；此项不替代设备兼容测试。
- NativeHomeActivity 启动入口、ReactAppTheme、min26/target36、无 INTERNET 权限已核对。
- 包内 Hermes 离线字节码 2530756 字节；Skia/Reanimated/Worklets/Hermes/ReactNative 原生库、PDFBox资源与离线许可完整；无旧 HTML/GSAP 首页与密钥。
- Metro source map 的 useGlassMotion.ts 与 motionMath.ts 与最终修复源码逐字一致。
- APK 校验详情随源码检查点 evidence/APK-verification.json 提供。


## 既往失败的处理

旧快照总验证曾在解压目录重复处失败。本轮从干净目录串行运行完整验证已经通过，未重现；缺少证据证明先前确由并发干扰造成，因此不宣称已定位历史失败根因。当前 tests/run.py 会先清理自己的 build/test-data。

## 尚未验证及明确边界

- 无 Android 安装/启动、系统选择器/保存/分享/旋转/进程重建的设备端到端结果。JVM 测试不等于 Android 宿主运行。
- 无 Android 真机的原版光学一致性、触摸延迟、帧时间、60/90/120FPS 数据。桌面 CPU CanvasKit 像素证据只证明原 Shader 可以编译并改变真实采样背景。
- 复用原版 Shader、核心几何与物理；背景、排版为轻转原创，文字使用 Skia Paragraph 分字合成。半圆出现动画、严格中央释放规则、结果返回属于业务适配；未宣称整屏与参考完全一致。
- 未使用可选 orb-field 粒子；不加入参考品牌、视频或贴纸。
- APK 使用独立测试签名，不能直接覆盖旧发行签名；请保留旧数据并先导出重要结果。
- GPL-3.0-only 对应源码/许可/修改记录随源码包提供。原 Junrar/UnRAR 附加限制仍存在组合分发兼容风险，未取得例外，不宣称可合规公开发布，见 LICENSE_REVIEW.md。

本次交付不是“全部功能已经在手机上确保无问题”的证明；剩余设备验收清单在 DEVICE_TEST_PLAN.md。

## 首次交付后的更新

14项前端测试通过（包含真实hook深浅色/反向关键帧参数对照）；诊断存储9项、会话25项通过。新增存储/恢复异常提示和参考仓库原始文本贝塞尔曲线。全量Lint失败于Worklets Kotlin构建脚本分析器内部，未完成；release构建的app lintVital报告为No issues found，不能替代全量分析或运行验证。

## 1.2.1 启动诊断 APK 最终构建

- BUILD SUCCESSFUL，日志 logs/startup-diag-package.log（1m40s）；versionCode4、1.2.1-startup-diag，ARM64/min26/target36。
- 37795583字节；SHA256 `7050f5dbf3095efb52d1a646c4a3829ef6927631cdce3ed8599caf11a1c02401`。
- 与首个测试包相同的独立测试证书，APK v2签名、16KB ZIP/ELF、全部DEX内部长度/SHA1/Adler32、诊断/引擎ClassDefs检查通过。
- 原生StartupActivity为Launcher、ReactActivity为内部入口；仍无网络权限。
- App.tsx/bridge/useGlassMotion/CanvasText/GlassHome的Metro原始source map与当前源码一致，早期attachBaseContext调用已通过编译后javap确认。
- 诊断APK的手机启动与报告页尚未验证；1.2.0首包的实机闪退仍为未解决故障，等待设备错误报告。

1.2.2最终APK：BUILD SUCCESSFUL；37795667字节；SHA256 a487d0ea16134692198fba1d2c525af21fccd673b8cae8227abca8a173b55eaa；同测试签名、DEX/ELF/16KB检查通过，Metro源码映射与修复源码一致。设备成功启动与转换尚待复验。

1.3.0 APK SHA256: e8eebc358ffc833d192d75011e5c5e63008c6e013e1c68c296c5170aa454d2e9
