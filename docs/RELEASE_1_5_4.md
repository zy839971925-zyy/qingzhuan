# 轻转1.5.4 · Refined image / bilingual UI

版本1.5.4-refined / code12；包名cn.qingzhuan.app.preview。开发证书与1.5.2/1.5.3相同。实现基线3581822，应用Java/Kotlin通过测试的提交74dded3，后续资产许可说明与文档单独记录；最终APK构建身份以handoff/APK-build-154.json为准。

## 实施

- 照片白/黑遮罩分别保存0–100%强度；原模糊、纯色和预设保留。
- 删除格式控件重复小字；图片高级选项默认关闭，原尺寸/质量100成为真实引擎默认，不再暗用旧2048/4096尺寸或旧质量。
- 开启后选择原图/4K/1080P/720P/自定义范围，保留比例或显式拉伸；JPEG/WebP质量可调，PNG无损不显示伪质量选项。PDF栅格尺寸/质量独立。
- 底部提示为“滑动以开始转换”/“Swipe to convert”，放在玻璃罩底部安全区，随同一拖动p模糊、位移和消失。
- system/zh/en语言选项：Compose、无障碍、预览、常用原生通知/错误同一目录呈现，保留真实文件名/文档/会话原数据。API33+接公开应用语言API，首页不重建转换状态。
- 多文件保留跟手分页，叠层增加弹簧滞后和轻微扇形反馈，沿用两端阻尼回弹；单张静止、减少动效保留。
- 参考用户tingxiejian-android文档结构重整双语README、技术架构、构建、变更/恢复记录；源码包形成时尚未推送；当前仓库的发布状态以 `v1.5.4-preview` Release 记录为准。

## 本轮真实证据

- 12项实际ImageTransformPolicy断言：大于4096原图、默认忽略旧参数、横竖预设、自定义fit/stretch、无放大、Exif采样、边界拒绝。
- 9项实际Java TextCatalog断言及静态Kotlin/原生错误/文件名/无障碍调用边界审查。
- Refined154Test：**2项，0失败、0错误、0跳过**。实际Engine在Android28 JVM影子上输出5000×20，legacyedge2048不生效；image/raster质量隔离断言通过。实际资产目录的中英文切换和中文文件名捕获保留通过。XML附在handoff日志中。
- 第一次编译2m10s失败：UiText按位置转发与解析后的Material3 Text(autoSize)签名不符。改为命名参数后，目标JVM检查及assembleRelease **BUILD SUCCESSFUL3m12s，60tasks**。失败日志保留，不回写为成功。
- 只读审查发现的默认质量/跨工具串扰、完成文件名翻译、原生错误和无障碍/计数翻译遗漏已修正，追加检查覆盖；没有用审查意见代替执行结果。
- 最终资产更新后assembleRelease **BUILD SUCCESSFUL43s，50tasks**，日志build-154-final.log。
- 最终APK15,204,145字节，SHA256 `3ad0fcbff7a0121b91bfc15ade1736181c27e0940c8e09fe3e02fb5c6074c6f6`；证书SHA256 `f253392041358c9edff32e76590aaa8de02725b1806183ae6ab6ba3eb8173286`。版本12、包名、深浅/双语资源、私有组件、无INTERNET、16KiB ZIP对齐均核对。
- 语言目录、预设PNG实际解码尺寸、DEX业务/新增类、ARM64原生ELF PT_LOAD对齐≥16KiB通过。README/架构/构建相对链接和2-testXML结果核对；未把ZIP对齐当成真机启动证明。

## 未验证与边界

没有安装安卓模拟器或连接真机。小米Android15/17实际安装/启动、语言切换时OEM行为、后台省电、TalkBack、照片遮罩的对比度、底部提示在各种屏幕的位置、弹性触感、光学/FPS均待真机验收；不能声称稳定60/90/120FPS或与视频逐帧相同。

图像尺寸检查不等于所有格式解码/编码保真或峰值原生内存测试。最高质量JPEG仍有损，sRGB8位处理不保证EXIF/ICC/HDR完整保留；大图会受像素/堆预算约束并明确失败。GIF图片工具只取首帧，透明JPEG铺白；更低API的WebP不承诺无损。

原媒体/文档边界不变：HEVC/AV1需要设备编码器；只有静态Live Photo不能恢复视频；强停/系统杀进程是中断而非续编码；Office内容预览不是完整排版/OCR/公式引擎。原第三方GPL/Junrar兼容风险未解决，不能称整个APK公开分发已合规。

私钥和本轮用户参考图/视频不入源码。每个重要阶段已提交并持久检查点，精确恢复入口为CURRENT_CHECKPOINT.md、DEVELOPMENT_1_5_4.md和handoff/RESUME.json。


## 2026-10-26 repository update

本次更新按用户直接指示，将提供的 `Qingzhuan-1.5.4-refined.apk` 原字节作为 Release 资产；没有重新打包 APK，也没有重新运行 Gradle/Android SDK 下载、许可或构建步骤。独立复核：ZIP完整性通过；包名 `cn.qingzhuan.app.preview`、versionCode `12`、versionName `1.5.4-refined`；APK Signature Scheme v2有效，证书SHA256与上方一致；zipalign检查通过；文件SHA256与上方一致。

本轮不依赖 Android SDK 的检查：`check_image_policy.py` 的12项尺寸/默认/Exif/范围断言通过；`check_localization.py` 的9项Java目录断言和静态UI覆盖检查通过；静态错误/文件名/无障碍边界检查通过；Python脚本编译、Markdown相对链接扫描和 `git diff --check` 通过。**本轮没有重跑Android28 JVM集成测试或 `assembleRelease`**；源码包中的旧日志/XML仅是此前结果，不代表本次提交重新验证。

真实设备启动、触摸/视觉/无障碍/性能验收仍未进行。GPL/Junrar分发兼容问题仍未解决。

## 2026-10-26 standalone formal build

Following the later request for a production package that can coexist with the older app, the project was configured with application ID `cn.qingzhuan.toolbox`, display name `轻转工具箱`, versionName `1.5.4` and versionCode `1`. Android SDK Platform 36 and Build Tools 36.0.0 were installed after the owner authorised acceptance of the SDK licence. `:app:testReleaseUnitTest` passed, and `:app:assembleRelease` completed successfully.

The release APK was signed with the dedicated production key kept outside the repository. Package name, label, version, v2 signature, alignment and ZIP integrity were verified. Artifact: `Qingzhuan-Toolbox-1.5.4.apk`, 15,204,105 bytes; SHA-256 `40d220c2616c7488e35fb346cdccfedbbe068def15b3d44379f5a70fa0c0cda2`. Signing certificate SHA-256: `a077d92842d12b607fede22c6168bc0fff92dcbf9c515529f60d5361f4bd2198`.

No physical-device or emulator acceptance has been performed. The APK build and unit tests do not establish device-specific launch, accessibility, performance or visual behaviour. Combined-distribution compatibility of GPL and Junrar / UnRAR remains unresolved.
