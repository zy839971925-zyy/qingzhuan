# 当前实际架构

`NativeHomeActivity` / `QingzhuanApplication` 提供 React Native Fabric + Hermes 宿主。`App.tsx` 管理原生快照、工具/文件/参数/结果面板。`GlassHome` 使用同一 Skia 画布绘制文件、格式文字、背景，然后应用原版 `lensEffect` BackdropFilter，再叠 `glassEffect`。Android 点击/读屏命中区覆盖同位置，不以透明 RN 元素冒充可被 Skia 采样的背景。

`useGlassMotion` 在 UI 线程维护 p、半径、中心、偏移、slosh、焦散。单一 p 同步文字位移/模糊/退场与玻璃几何。只有有效松手后调度 JS 请求。读屏按钮先移动到中央，再提交。

| 边界 | 内容 |
|---|---|
| Native → JS `QingzhuanState` | JSON Snapshot：protocol/revision/inputs/outputs/busy/task/warning |
| input | id/name/file/size/kind/tools/audioTargets；工具能力来自原生探测 |
| task | id/seq/kind/status/message/value/files；value=-1 不可量化 |
| JS → Java `startTask` | 唯一 ID、工具、参数 JSON、输入 ID JSON |
| `cancelTask` | 提交取消请求；status=cancelling，直到原生终态才恢复界面 |
| 保存与分享 | 继续使用 SAF 与只读 OutputProvider；不申请全盘或网络权限 |
| 生命周期 | Application 级 AppSession 不持有 Activity，跨屏幕重建；进程死亡恢复 interrupted |
| 系统选择结果 | Activity 先写 DocumentInbox，模块就绪后一次消费；绕开 ReactContext 未创建时丢弃结果 |

原 Java `Core`、`Engine`、`OutputProvider` 保留。`AppSession` 串行工作队列执行导入/转换/导出，所有耗时引擎工作在后台线程。SessionStore 原子替换小型元数据记录，不保存文件密码。输入采用 files/inputs，而不是启动即清空的 cache。结果提交和终态一起检查点保存。失败删除对应任务结果目录。

## 原生工具能力

图片由 BitmapFactory 验证解码能力；音频由 MediaExtractor / MediaCodecList 缩小到本机支持的 M4A/WAV。PDF/常见压缩按文件签名，Office 检查 ZIP 内部部件，文本按内容排除二进制。内部副本按类型规范后缀供旧引擎路由，显示名保留用户原名。

保留图片转换/图片PDF/PDF图片/合并拆页/文字重建/表格/ZIP与TAR.GZ打包/解压/音频提取/校验。RAR4 仍使用原 Junrar；其 GPL 许可兼容风险未隐藏，见 LICENSE_REVIEW.md。

## 与参考仓库的差异

- glass.ts 原样；普通首页背景与品牌为轻转原创文件界面，无参考贴纸/视频。
- 复用主要几何、弹簧、跟手阻尼、侧偏、焦散、slosh、文本 blur 区间；取消速度提前提交。
- 中文按 grapheme 错峰/模糊/轻翻转；用 Skia 实际文字图层模糊替代 Expo BlurView。
- 横屏采用两列与基于可用高度的玻璃缩放，保证操作可达；不照搬锁定竖屏。
- 不使用 orb-field 粒子：实际任务不需要贴纸喷发；原文件保留供对照，未进入运行时。
- 加入真实任务进度、取消确认、成功原路返回与可访问操作。

以上代码路径已实现；Android 全链路与真机光学/帧率是否通过，以 VALIDATION.md 中实际记录为准。
