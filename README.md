<p align="center"><strong>简体中文</strong> · <a href="README.en.md">English</a></p>

# 轻转 · Qingzhuan

**选择文件，选择格式，滑动玻璃罩开始转换。**

原生Android转换工具：Jetpack Compose界面、真实背景折射和原Java转换引擎。转换在本机完成，无需账户或联网服务器。

当前正式稳定版 **1.5.5 / versionCode 2**，包名 `cn.qingzhuan.toolbox`。与正式版1.5.4的包名、签名指纹一致且版本号递增；本次未在真机验证覆盖安装或应用数据迁移。

[使用](#从导入到保存) · [图片参数](#原图与高级选项) · [构建](#获取与构建) · [架构](docs/ARCHITECTURE.md) · [验证](docs/RELEASE_1_5_5.md) · [恢复检查点](docs/CURRENT_CHECKPOINT.md)

## 1.5.5改进

| 交互 | 当前行为 |
| --- | --- |
| 玻璃按压 | 底座的布局和屏幕位置固定，活动滑块仍会放大、拉伸和回弹 |
| 格式与菜单 | 短距离错峰入场，玻璃按钮/图标统一弹性按压，选择立即生效 |
| 参数层级 | 高级/自定义尺寸、背景参数平滑展开；隐藏和退场内容不能继续操作 |
| 预览与结果 | 缩略图载入淡入；多个输入或结果都可左右浏览，文件夹图标打开完整列表 |
| 列表 | 稳定ID的出现、删除和重排过渡，批量保存/删除保留 |
| 导航 | 子页返回上一级；删除确认优先取消；关闭键直接关闭 |

保留1.5.4原尺寸默认、可选高级尺寸/质量、独立深浅背景遮罩、中英文和所有既有转换能力。研究来源、适用性与未覆盖风险见[动效审查记录](docs/RESEARCH_1_5_5.md)。

## 从导入到保存

1. 首页点击虚线区域，从照片或文件导入；只复制用户选择的文件，取消不会清空已有输入。
2. 查看缩略图，多文件可左右滑动或点击原生预览；文件夹图标打开管理列表。目标按整批共同能力匹配，同格式图片不重复提供源格式快捷项。
3. 选目标格式。少量格式用玻璃滑块，展开后是独立玻璃按钮。左下角设置入口管理当前工具参数。
4. 上拖玻璃罩至中央松手，启动真实转换；提前松手回弹，途中可反向。球内有最短可见反馈，绿勾必须等待真实成功。
5. 点击结果直接预览，无需先保存。图片/视频默认保存到相册，其他文件使用系统文件选择器；列表支持全选、批量保存、删除与分享。

普通切后台由前台服务继续处理，通知与界面读取同一会话。**强停、进程死亡或系统时限到达不保证继续编码**；已提交结果保留，未完成任务明确标记中断。

## 原图与高级选项

高级关闭时，输出尺寸等于方向校正后的源尺寸，图片编码质量100。PNG无损，不提供无效质量滑块；PDF转图片使用独立渲染尺寸/质量，不受图片参数串扰。

| 选项 | 目标边界 |
| --- | --- |
| 原图 | 保留源尺寸 |
| 4K | 横3840×2160，竖2160×3840 |
| 1080P | 横1920×1080，竖1080×1920 |
| 720P | 横1280×720，竖720×1280 |
| 自定义，保持比例 | 适配宽高范围，不放大 |
| 自定义，关闭保持比例 | 精确宽高，可拉伸/放大 |

预设保持比例且不放大，非16:9图片不一定填满边界框。宽高1–32768、最多64Mi目标像素，并受可用内存限制。原图需求过高会明确报错，允许改用较小尺寸，**不会静默降级**。

最高质量不代表原文件字节/元数据不变：JPEG仍有损，处理使用方向校正的8位sRGB位图，不保证EXIF/ICC/HDR/广色域完整往返。透明区域转JPEG铺白；图片工具对GIF只处理首帧。Android10+最高质量WebP可走无损路径，Android8/9旧编码器不承诺无损。

## 保留能力

- JPEG/PNG/WebP、图片成PDF、PDF渲染/合并/拆页。
- 视频/录屏片段转GIF：尺寸、帧率、质量、循环；最长60秒，另有处理量和128MiB输出限制。
- 视频裁剪、压缩、H.264/HEVC/AV1转MP4、静音和截图；编码器依设备，失败不暗换编码。
- 兼容Motion Photo内嵌视频或同名照片/MOV配对；静态照片不能恢复缺失的Live Photo视频。
- 文档文字及首个工作表转换，不提供OCR、版式保真或公式计算。
- ZIP/TAR.GZ、受支持解压、SHA-256/MD5、AAC音轨提取M4A和设备解码WAV；不支持MP3编码、RAR5/分卷RAR。
- 图片/GIF/PDF/影音/有界文档原生预览；Office显示内容，不完整还原版式、图形或公式。

## 外观与语言

设置可选系统/浅色/深色，以及跟随系统/中文/English。手机语言为中文时用中文，其余默认英文。切换呈现语言不翻译文件名、用户内容、会话原始数据或第三方许可原文，也不重启转换任务。

背景包括动态光晕、默认、三款原创壁纸、纯色/HEX、自定义照片及模糊。照片深浅遮罩独立记住；过低遮罩的可读性取决于照片。系统减少动画/省电模式会减弱动效。完整RuntimeShader折射需要Android13+，旧系统保留转换和视觉降级。

## 获取与构建

获取正式版：**[下载 Qingzhuan 1.5.5 APK](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/download/v1.5.5/Qingzhuan-Toolbox-1.5.5.apk)** · [Release说明](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/tag/v1.5.5)。源码仓库不含签名私钥；构建与签名校验记录见下文。

需要JDK17+（建议Android Studio的JDK21）、SDK36、Build Tools36、Python3及Gradle8.13。无需Node/npm/Expo/NDK/CMake/模拟器。首次依赖解析联网，之后可复用缓存。

```sh
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
python3 setup_tools.py
python3 build.py --variant debug
# Produces Qingzhuan-1.5.5-debug.apk
# For an official release, configure the private release signing credentials
# outside the repository before running: python3 build.py --variant release
# Produces Qingzhuan-Toolbox-1.5.5.apk
```

也可用Android Studio打开 `android/`。正式版需要外置的原生产签名密钥；缺少该密钥时Release构建会停止，绝不退回开发签名。详见[构建指南](docs/BUILDING.md)。

## 验证、隐私、许可

静态检查、JVM/Compose定向测试和正式签名APK校验结果见[发布记录](docs/RELEASE_1_5_5.md)与 `handoff/`。**未使用模拟器，也未做真机启动、光学、触摸或帧率验收**；不宣称60/90/120FPS或与参考逐帧一致。

无INTERNET和全盘读取权限。私有目录保存输入、结果、背景和诊断；媒体用MediaStore，文档用SAF，分享仅授权所选结果URI。输入最多20份、单份512MiB、合计1GiB；私有结果也有限额。私有数据不是云备份，卸载前请导出。

各第三方遵循各自许可，APK不能整体宣称MIT。Kyant为Apache-2.0；历史参考Shader有GPL-3.0来源，原文/修改记录保留。**既有GPL/Junrar分发兼容问题未解决**，见[许可审查](docs/LICENSE_REVIEW.md)和 `licenses/`。原创壁纸/生成器为CC0。

开发从[架构](docs/ARCHITECTURE.md)、[构建](docs/BUILDING.md)、[变更记录](CHANGELOG.md)、[当前检查点](docs/CURRENT_CHECKPOINT.md)和[工程日志](docs/DEVELOPMENT_1_5_5.md)进入。历史RN/WebView文件只供追溯，不参与APK。
