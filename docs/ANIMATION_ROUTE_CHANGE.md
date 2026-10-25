# 2026-10-25 动画路线变更（用户指令优先）
- 最终新首页改为 React Native + Skia + Reanimated + Gesture Handler，复用上传开源复刻仓库的 Shader 与运动逻辑。
- 已实现的 Java AppSession/FileProbe/SessionStore/TaskLedger 与 Engine 修复继续保留并审查。
- app/src/main/assets 下 WebGL + GSAP 首页为封存的阶段性实验，不代表最终动画目标，不把它作为原 Shader 等效实现。
- 转换启动仍以中央有效区域松手为准（p>=0.92），不按参考仓库的速度冲量提前提交。
- 原始源码与两个上传 ZIP 均不修改。
- 若复用 GPL-3.0 代码，组合应用将以 GPL-3.0（按源文件原文确定 -only/-or-later）分发，并附完整相应源码/构建方法/版权许可；不复用 Wabi 商标、品牌或不明授权图片/视频。
- 用户目标为可安装、可运行且验证的 APK；构建成功、桌面渲染与 Android 真机运行必须分别报告。
