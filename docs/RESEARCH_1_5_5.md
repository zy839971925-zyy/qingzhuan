# 1.5.5 UI / 动效研究与源码审查

研究日期2026-10-30。用户目标是稳定且直觉的原生转换软件；“更多动画”的验收不是数量，而是按压/选择/导航/载入/重排具有明确连续反馈。无设备可捕获当前运行画面，因此以下是源码审查和机制研究，不能称实屏或实机验收。

## 来源和适用性

| 来源 | 入口 | 实际用途/边界 |
|---|---|---|
| 个人资料库Workflow3.1.0 | reasoning-workflow-3.1.0(1).zip | 实际解包读取SKILL.md、Ultra overlay及research-modes；当前资料库检索未发现更高版本。 |
| AI Design Skills | https://github.com/elayadesign/ai-design-skills | 实际读取skills/landing-page-design/SKILL.md；主要是营销网页，不机械套字体、无渐变或统一500ms。 |
| UI UX Pro Max | https://github.com/nextlevelbuilder/ui-ux-pro-max-skill | 实际读取SKILL、ux-guidelines.csv、jetpack-compose.csv、pro-rules；BM25针对rapid chip animation interrupted、touch target stable press、animation semantics。 |
| Motion Primitives | https://motion-primitives.com/docs | 根文档直读403；搜索引擎返回官方索引页，并从官方GitHub ibelick/motion-primitives读取animated-group.tsx、disclosure.tsx、transition-panel.tsx。React源码仅研究机制，不复制或引入运行时。 |
| Android Compose | https://developer.android.com/develop/ui/compose/animation/composables-modifiers | 官方AnimatedVisibility/AnimatedContent语义与退出保留；value-based和handling-interactions文档用于可取消动画/PressInteraction。 |
| 用户文档范本 | https://github.com/zy839971925-zyy/tingxiejian-android | 实际读取README.md和docs/ARCHITECTURE.md，采用双语入口、真实能力/约束、架构职责和可复现构建说明。 |

资料库两个3.1.0包大小相同，选择较晚上传的(1)副本；不把平台暴露的旧2.1.1当最新版。设计库同为作者规范，彼此不是用户实测证据；平台文档约束API，源码与回归测试约束实际实现。

## 源码发现与落点

| 状态变化 | 1.5.4事实 | 1.5.5处理 |
|---|---|---|
| 分段按压 | 根Box缩放Y1+.025pressure，使底层整体边界漂移；上层滑块又叠加形变 | 底层无transform；仅滑块按中心放大，保持固定布局/纵坐标 |
| 普通玻璃按钮/图标 | 系统ripple但与玻璃滑块物理语言不统一 | 共享按压弹簧，只改绘制transform，不改测量布局 |
| 展开多格式 | 容器已有fade/SizeTransform，网格项同时硬出现 | 短错峰/轻移/淡入，反向随状态取消；不叠加长期循环 |
| 弹层菜单 | 面板已有进出动画，内部项目无次序；子页没有回到菜单 | 短错峰、返回层级和横向方向提示，关闭保持单一动作 |
| 图片缩略图 | bitmap空值变图像直接替换 | 先保留稳定占位，再加载淡入；不重播单图漂浮 |
| 多文件 | 卡片已有真实Pager弹簧/惯性，onManage未接可见入口 | 保留手势，增加明确管理入口，元信息切换淡入；结果同样可左右浏览 |
| 历史顺序/删除 | 稳定ID但元素位置突变 | Lazy item原生出现/消失/位置动画，语义仍立即更新 |
| 图片自定义参数/背景模式 | 条件内容瞬时插拔 | AnimatedVisibility披露/平滑尺寸；不让两个编辑区交叉覆盖 |
| 任务进度/绿色勾 | 已有真实绑定+最低可见反馈 | 保留，不增加额外等待或伪成功 |

## Ultra替代解释与边界
竖直跳变也可能来自设备实际捕获/像素取整、手指纵向位移或系统插入区域；当前确定源码原因是父底座transform，不宣称它能解释所有未提供视频中的像素。几何回归用真实Compose坐标区分“布局测量不变”与“屏幕绘制边界不变”。去除父transform不会关闭上层真正的折射，后台层捕获仍不包含滑块自身。

不加入所有图标永久漂浮、全部文字逐字循环、背景额外视频、菜单复杂共享元素或新增JS依赖。动画从当前值到目标值，业务选择立即提交，不能依赖动画结束事件保证数据正确。减少动态时取消位移/旋转/弹性。Android触区采用48dp，不照搬网页px或iOS44pt。

## 研究的可恢复证据
外部原始文件在研究临时目录，不把完整Skill复制进应用包。以下SHA256仅证明本轮所读字节身份，不证明作者建议正确或已实机复现。

- `compose.csv`: `699de218b286948eaec7d15767b6e1350ee299167e49bf476d82e3ff58724e0a`
- `design-skill.md`: `203569792660ab6d71dcb2e4fa673c0c7a0df7c99801d04ab419a4cb08aceb2c`
- `motion-animated-group.tsx`: `0c162bbdd3c332e7aa113cd7ddfa7ac36de3184866cb82ad9140525561e08c35`
- `motion-disclosure.tsx`: `6ff585f95b063f1fdcff31c117051f732ef247a99fe039fe500bcccb3dc8bd40`
- `motion-transition-panel.tsx`: `1b3c67058a09a560a034b7fe52ad1c205b589a63ed80055900e8a66830db0be2`
- `targeted-results.json`: `f85b5ef5e482c358d7413788e5463c4f0cb1047e3cc63f95a85f0d0c02525290`
- `user-architecture.md`: `1ff3666f83dcd844b96ffb89c2b11b26fb692aaa1ab9a53633149e6363cea6c3`
- `user-readme.md`: `ef684632fa786aa121176766b6ce33030acbdb3829adc70abc27a017ab4ea9b1`
- `ux-core.py`: `dfb34cbca66a704a5bfae48c1ab5f82d2a6573b9509c5accceb3d248715a18f6`
- `ux-guidelines.csv`: `ff81ec613f70ba9fc3fcce52dbe4ae35d44b2079dbe6dc066d2d6e38c28facd5`
- `ux-pro-rules.md`: `d28442d61c310b49c054a4ea736f89fb3008beebdbbc38c4b3a540b8f7fadf23`
- `ux-search.py`: `d54e648fe0ec2932cac66684220cc17be4bd4b4eba1d23d3195d509d398db374`
- `ux-skill.md`: `ea087c341bfb5b23195c7302027268ede86da802554c18a5c4896a6017b439f9`

## 本轮验证补充

实际Compose回归复现底座顶部63.0→60.97458px，去掉父track transform后保持不变，镜片参数未改。源码独立复查还复现单卡偏左、返回确认优先级和缓存旧页动作；回归先失败后通过。快速同帧双事件需要读取导航拥有者的当前值，不能依赖等到下一次重绘的页面参数；导入选择器必须先发出请求，再退出面板，防止主动关闭后动作被正确的旧页保护拦住。

Motion Primitives许可实际从官方LICENCE.md读取，为MIT；仅参考行为模型，本次原生实现没有复制其React源文件。其文档网站直读403的缺口由官方索引与同作者组件源码部分补足，不能声称完整观看所有示例。平台Compose APIs核对不等于设备帧时间观测。
