> 更新：实机诊断已定位 PanGesture 被 worklet 闭包捕获的启动错误；1.2.2 已修改并通过编译闭包回归测试，等待手机复验。详见 PAN_GESTURE_STARTUP_FIX.md。以下保留历史调查。

# 启动闪退调查（2026-10-25）

## 已知失败

用户在实机反馈：交付的 1.2.0 RN/Skia 测试包点击后立即退出，未进入首页。启动验收失败。根因尚未确认，不能将构建/签名通过当作软件运行通过。暂停其他功能扩展，优先处理此故障；不启动模拟器。

## 稳定基线对照

- 原始 `Qingzhuan-source (1).zip`：Android Java Activity + WebView，没有自定义 ReactApplication；target34，PDFBox 在 Activity 中初始化。用户确认此源码构建可正常打开。原始文件保留，未修改。
- [听写间](https://github.com/zy839971925-zyy/tingxiejian-android)：已读取当前 main 的 AndroidManifest.xml、TingxiejianApp.java、MainActivity.java、build.sh 与架构文档。Java + Android Views；compile36/min26/target35，非 RN。TingxiejianApp 在 attachBaseContext 安装 Report；MainActivity 捕获界面初始化异常并显示原生 fallback；extractNativeLibs=true。
- 本次新增层：ReactApplication → SoLoader/新架构 → ReactActivity/AppCompat → Hermes 离线包 → 自定义模块 → Skia/Reanimated。target36，默认 mmap APK 内原生库。稳定基线证明原设备能运行既有项目，不能证明新增 RN/Skia 链路已兼容；目标SDK/库提取方式差异尚未被证明是根因，未盲目改动。

对照文件 SHA：TingxiejianApp.java `0872db3fc0be67f3a1128abeb43bf94d73a552e5`；MainActivity.java `d2b3a048649c4f24d3110a3384f1856358af66a5`；AndroidManifest.xml `827255c1085acbbcc1922dfb6b6537dd372204c0`；build.sh `7f1d9ab4f21fd86e1d0461d37e543dae795ccf0d`。这是内容 blob SHA，不冒称已固定整个仓库提交。

## 已检查的边界

- 第一个 APK：DEX 文件长度、内部 SHA-1/Adler32 正确；15 个 ELF 未截断、16KB对齐、动态依赖和非弱 C++ 符号可在包内/系统解析；JNI库为未压缩条目。没有从这些检查得到可以直接解释闪退的证据。
- Launcher、Application、ReactAppTheme、Fabric/Bridgeless flags、离线 Hermes、自动链接包、传统模块互操作路径已阅读；不以静态阅读冒称已成功启动。
- 全量 Android Lint 在 Worklets 的 Kotlin build script/K2 UAST 分析中崩溃，`Cannot find a KaModule for the VirtualFile`。上游源码已有 lintVital 规避注释（Google issue 432144179）；未宣称全量 Lint 通过。

## 诊断构建 1.2.1-startup-diag / versionCode4

- 沿用原测试签名、包名，可覆盖刚才的测试包，不要求卸载或清除转换数据。
- 将 Java 异常记录前移至 attachBaseContext；记录启动阶段、真实异常栈、设备型号/系统/ABI、可取得的本应用进程退出原因。
- 增加不依赖 RN/Skia/AppCompat 的 StartupActivity 报告页。Application 同步异常直接显示报告；ReactHost 异常转到原生报告页；未捕获 Java 异常持久化后在下次打开显示。
- 启动未完成的持久标记用于在下次启动时绕开反复失败的 RN 初始化；明确区分“未完成/被结束”与已确认崩溃，不能仅凭标记推断 native crash。
- 报告只存本机私有目录，不自动上传；可手动复制。仅清理诊断文件，不触碰输入、结果、密码或会话文件。不同诊断版本会清理旧版本的活动诊断标记，避免升级后被旧错误页困住。
- 纯 Java 诊断存储测试9项与会话测试25项通过；TypeScript、14项前端测试通过。Android 报告页仍需手机验证。

这不是根因修复完成的声明。下一步需要手机复制的实际错误报告；如果诊断包仍立即退出，重复打开一次。若仍无报告，则需要系统崩溃详情/logcat（可能发生于 Application 挂接前、Provider 或 native signal 路径）。未经该证据，不猜测性替换渲染架构或删除转换功能。
