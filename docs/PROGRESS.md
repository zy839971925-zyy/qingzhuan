# 当前1.5.4

以CURRENT_CHECKPOINT.md、RELEASE_1_5_4.md与DEVELOPMENT_1_5_4.md为准。下文为历史记录。

# 当前1.5.3

以CURRENT_CHECKPOINT.md、RELEASE_1_5_3.md和DEVELOPMENT_1_5_3.md为准。以下为历史记录。

# 1.5.2当前进度

详见RELEASE_1_5_2.md、CURRENT_CHECKPOINT.md和handoff/RESUME.json。完整媒体/后台与玻璃原生预览实现已编译；必要GIF独立解码通过；真机验收未做。过程保存两个可恢复WIP版本，最终源码保留完整历史。

---

# 轻转 1.5.0 · Aurora 原生预览交付

2026-10-25。当前Kotlin + Jetpack Compose + Kyant Backdrop1.0.6，转换由原Java AppSession/Engine负责。历史RN文件不参与当前APK。versionCode8，应用ID `cn.qingzhuan.app.preview`，名称“轻转预览 1.5”。与1.4预览使用同一私有开发签名，可覆盖预览1.4；不覆盖日志中的旧1.3应用。原项目与完整Git历史保留。

## 已实施

- 导入来源：系统照片选择器或文件选择器；取消不清空原输入，自定义背景独立导入。
- 输入/结果采用真实解码缩略图；单张保持静止，多张圆角细边框阴影卡片重叠并可左右滑动，跟随当前文件名与计数。
- 全批次共同源格式从快捷目标中排除；JPG批次显示PNG/WebP/PDF，PDF走实际imagepdf引擎。保留其他工具与转换能力。
- 格式选项和设置/添加按钮增加间距；统一细线矢量图标，重绘自适应应用图标。按钮采样独立的背景层，主球采样背景与内容，避免自采样回路。
- 背景：动态淡蓝/淡紫/薄荷/杏色光晕、静态默认、自定义图片+0–40模糊。深浅色、低电量/减少动画适配；背景最多32MiB输入并缩至1600边，缩略图384边与12MiB缓存。Android8–11用400边三次box blur近似高斯，Android12+使用RenderEffect模糊。
- 原参考仓库glass.ts的GLASS_SKSL和LENS_SKSL文本原样移植到Android RuntimeShader，Kyant负责真实背景捕获、缓存、高光。径向RGB折射在球顶部也有分离；焦散是原仓库解析艺术模型，非完整物理光谱路径追踪。保留连续进度、可逆拖动和有效中央松手逻辑。
- 秒完成仍播放最短1200ms可见反馈，真实成功后才走满→绿勾→原路退回→结果；未知进度只在等待区间模拟，不先报成功。
- 图片/视频结果默认MediaStore相册保存，图片Pictures/轻转、视频Movies/轻转；文档用SAF。多媒体+文档混选先保存媒体，再由当前已恢复Activity接续文档选择，应用工作线程不保留旧Activity。保存失败保留源结果、报告部分成功计数并删除当前未完成条目。
- Android10+不需要广泛相册/存储权限；Android8–9仅请求maxSdk28的WRITE_EXTERNAL_STORAGE，拒绝后可保存到文件。
- 自定义背景导入为应用级任务，Activity重建不取消文件与偏好提交；原子替换避免半写图片。失败保留原背景。
- 启动：pending marker和精确匹配的旧合成“上次启动未完成”报告不阻挡；真实异常报告继续保留诊断入口。第一可见帧标记启动完成。用户提供的reason10/SwipeUpClean是进程被清理记录，本身不能证明原生崩溃。

## 验证证据

- Java会话/诊断/选择/反馈/路由/终态71项断言通过（本轮新增合成报告识别）；见handoff/logs/aurora-startup-green.log。
- 从当前原生源码提取三段Shader，经桌面CanvasKit/Skia编译；原GLASS/LENS逐字匹配参考，输出深浅色各3帧参数。该证据不等于Android设备截图，RGB顶端采样间距在p=0/.5/1约6.15/8.95/4.98逻辑像素，仅为当前参数计算。
- assembleRelease已成功；最终APK/签名/哈希记录见handoff/APK-build.json、日志及校验文件。中间失败为Kotlin导入/方法签名和过期派生DEX重复，已按日志修正并清理派生产物重建。
- 按用户要求不使用模拟器，不进行新的大范围回归；1.4的API35 JVM流程和20066引擎断言是历史证据，不能当成1.5新功能的完整验收。旧NativeFlowTest的选择器/保存断言尚未适配1.5，勿称当前通过。

## 尚未验证 / 边界

没有连接真机。未验证小米Android15/17启动、PhotoPicker/云盘Provider、相册可见性、混合导出重建、低版本CPU模糊视觉、GPU叠层和触摸帧时间。不能声称稳定60/90/120FPS，不能声称与参考整屏完全一致。Android13以下无RuntimeShader，玻璃和动态背景降级。静态默认是纯色背景。

大底部半圆、手势几何和时钟为轻转业务适配；未实现可选orb-field粒子，未宣称Apple/Wabi内部实现。极小屏/大字体多行格式与半圆空间未做设备验收。MediaStore进程被强杀时可能留下系统pending项，当前捕获异常/取消会删除当前未完成条目；已保存项不回滚，结果源文件保留。

GPL-3.0-only原Shader对应源码、原始文件、修改记录、构建安装说明、许可证随包提供；Kyant为Apache-2.0。Junrar/UnRAR附加限制与GPL组合分发兼容问题仍未解决，不能宣称整体公开发行合规，见docs/LICENSE_REVIEW.md。私有开发密钥不放入源码ZIP，单独备份。


---

## 历史1.4交付记录

# 轻转 1.4.0 · 原生 Compose 交付

当前入口：Kotlin + Jetpack Compose + io.github.kyant0:backdrop:1.0.6；保留 Java AppSession/Engine。正常启动不再初始化 React Native、Hermes、Worklets 或 WebView。旧 RN 代码与完整历史保留，不参与新 APK。

当前包：Qingzhuan-1.4.0-compose.apk，11729032 字节，versionCode7，应用ID `cn.qingzhuan.app.preview`。SHA256 `78323d345704c88055ddf59f18e8aba4bb493857c7a8cb81235df4605fff3b89`。最终 assembleRelease 成功，日志 handoff/logs/native-compose-final-build-2.log。独立预览包与旧版并存，旧数据未迁移或删除；旧测试签名没有保存在原交接包中，当前预览签名独立备份为 qingzhuan-preview.keystore，开发密码 android，私钥不放入源码。

## 已实施

- 暖白/墨灰/淡雾绿主题，原生系统栏与自动深浅色；虚线上传框、轻浮动文件图标、无额外标语。
- 原生矢量文件图标和格式徽标；导入后图标缩小上移；格式错峰、逐字模糊/翻转。只有选中格式才出现半圆罩。
- 直接使用 Kyant 原生 Backdrop 库进行真实背景采样、折射、色散与高光。以连续共享进度保持半圆到球及 UI 模糊/位移可逆；提前释放回退，中央有效松手启动引擎。
- 可见帧驱动的非线性时钟进度，最短1200ms；只在真实转换成功后走满，绿勾420ms后沿原路返回结果图标。
- 完成页仅保存和继续两个图标操作；导入记录/结果列表支持全选删除，结果多选保存为文件夹中的独立文件，保留单份保存、预览、分享。
- 冷启动及重新进入回到上传页，历史保留；系统文件选择/保存返回作为例外保留当前操作。
- 修复无异常的启动中断被永久阻挡；真实报告可复制，允许清除诊断后重试。增加系统页大小和退出追踪元数据。
- 修复混合批次静默漏文件（只展示全批次共同支持的工具）；转换终态独立锁存，不被分享导入覆盖；旋转/尺寸变化清理中断手势。
- API35 JVM 流程发现并修复局部函数引用导致提交持有旧文件/目标状态的问题，改为可更新 lambda，已观测 RED→GREEN。

## 已有证据与限制

用户要求停止测试前：Java会话/诊断/选择/反馈/路由/终态共68项断言通过；原Java引擎20066项断言及独立Office/PDF检查通过。Robolectric API35 / native graphics 全流程通过：双文件导入→JPG真实编码→球内反馈与完成页→重建Activity默认上传→结果全选文件夹保存Intent→取消保存→全选删除。Robolectric需要JDK21；APK构建JDK17+可用，推荐Android Studio JBR21。

未使用安卓模拟器；没有连接真机。不能声称 Android15各厂商手机均可启动，或光学、触摸延迟、60/90/120FPS已验收。JVM截图捕获不可用，未生成运行截图，也不以生图代替运行证据。UI手势触摸尚未做完整设备测试，JVM流程以辅助功能提交动作驱动。Android13以下缺少RuntimeShader，功能可用但不是完整玻璃效果。

## 用户最新设计方向（交付后继续）

按照Apple WWDC的内容/导航分层、清晰可读、连续形变原则，更多真实玻璃可以用于导航、按钮、操作栏；不在同一Backdrop记录层中制造自采样循环，也不无节制叠玻璃。当前主转换球已使用库；更多玻璃按钮及最终设备视觉打磨仍未实施。

输入显示名后缀与真实识别类型不一致时，图标/徽标的显示优先级仍可改善；Java引擎和可用工具依据真实探测，不能把此显示问题说成已修复。实际SAF云盘/文件夹写入、失败与大文件仍需真机验证。

GPL/Junrar组合分发风险仍见 LICENSE_REVIEW.md。Kyant库为Apache-2.0，随包保留许可证；不宣称整体发行许可已审定。
