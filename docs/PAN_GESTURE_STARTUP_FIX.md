# 1.2.2 启动修复：PanGesture 无法序列化

用户设备：Xiaomi 25060RK16C，Android17/API37，ARM64。1.2.1诊断报告给出 ReactHost/JS 初始化异常 `[Worklets] Cannot copy value of type PanGesture`。

根因：GlassHome.tsx 的 orbTextTransform 与 hintOpacity 两处 useDerivedValue 闭包引用整个 useGlassMotion 返回对象 m。该对象包含手势实例 pan，Worklets 编译后的 __closure 实际捕获 m，向 UI 运行时复制时递归遇到不可序列化的 PanGesture，首页初始化失败。

修复：先解构 orbCx/orbCy/hintFade/visibility 四个 SharedValue，闭包仅引用这些值，手势对象留在 JS 组件的 GestureDetector 中。不改原始Shader、物理、转换引擎、输入和结果处理。不因这份错误修改Android目标SDK。

回归：使用项目实际 Babel/Worklets 插件编译 GlassHome 并遍历 __closure，确认不再捕获含手势的 m。改前测试失败，改后通过；15项前端测试、TypeScript、25项会话和9项诊断存储测试通过。原测试只模拟手势数学，未执行实际Worklets序列化，故未发现此启动错误；现增加编译闭包层的检查。

versionCode5 / 1.2.2-startup-fix 保留本机诊断页和同一测试证书，覆盖更新1.2.0/1.2.1测试包，不需卸载或清数据；版本变更会清掉旧诊断阻断标记，不触碰转换数据。此修复针对日志确认的问题；手机成功启动和后续转换仍需设备复验。不启动模拟器。
