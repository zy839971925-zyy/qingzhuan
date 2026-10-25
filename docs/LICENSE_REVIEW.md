# 许可复核（持续更新，不能以附 GPL 文本冒充已解决）

- 参考仓库 package.json 明确 GPL-3.0-only；glass.ts 原样复用，运动逻辑派生。
- GPL 组合分发需提供完整相应源码、修改记录、构建安装方法和许可文本；不复制仓库品牌/素材。
- 新发现：旧引擎 `libs/junrar.jar` 的 UnRAR 许可禁止用于重建 RAR 压缩算法，属于额外用途限制；与 GPL 组合分发存在兼容风险。上游明确保留此限制（本地 licenses/Junrar-LICENSE.txt）。
- 不擅自声明取得 GPL 链接例外，不改写第三方许可，不把“测试版”当作豁免。继续开发和构建验证，发行前需替换兼容实现或取得覆盖所有相关代码的许可例外；若未解决，在最终交付明确报告。
- GSAP/WebGL 实验前端不进入新 RN APK，避免把该依赖混进 GPL 组合。

## 1.4.0 迁移

当前运行UI直接引用Kyant Backdrop1.0.6（Apache-2.0）；Maven源码与许可已保留。旧GPL Shader不进入新渲染链路，但不据此擅自更改整个项目许可，也不宣称Junrar兼容问题自动解决。

## 1.5.0 再接入原Shader

原GLASS/LENS现在实际进入原生渲染链路，位于home/ReferenceOptics.kt。文本与third-party/liquid-glass-screens/src/liquid-glass/glass.ts逐字一致，Android宿主/参数适配为本次修改。GPL相应源码、许可、变更和构建安装说明随APK共同交付；保留版权。原解析焦散不是完整光谱路径追踪。Junrar组合兼容风险仍未解决，提供源码不代表该问题已获许可豁免。

## 1.5.2

当前球回退Kyant路径，原GLASS/LENS仅保留源码历史；新增Media3 1.11.1与Square gifencoder0.10.1（均Apache-2.0）。许可证/对应源码和构建说明随交付保留。原GPL与Junrar组合兼容风险仍未解决。
