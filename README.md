<p align="center">
  <img src="docs/assets/qingzhuan-icon.svg" width="104" alt="轻转工具箱图标">
</p>

# 轻转工具箱

<p align="center">在 Android 手机上转换、预览与整理常见文件。</p>

<p align="center"><a href="README.md">简体中文</a> · <a href="README.en.md">English</a></p>

<p align="center"><a href="https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/tag/v1.5.4-preview">下载 1.5.4 预览版 APK</a> · <a href="https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases">所有版本</a></p>

轻转工具箱是一款本地优先的 Android 文件转换工具，把图片与 PDF、文档与表格、视频与动态照片、音频和压缩包工具放在同一个应用里。文件在设备上处理，不会上传到在线转换服务；导入、保存和分享都通过 Android 系统界面，由你选择文件与目标位置。

当前源码为正式包配置了独立应用 ID `cn.qingzhuan.toolbox`，可与旧版 `cn.qingzhuan.app` 和 1.5.4 预览包并存。当前版本为 **1.5.4**；正式 `release` 构建必须使用单独配置的发布签名密钥。

## 可以用它做什么

| 任务 | 工具 | 适用边界 |
| --- | --- | --- |
| 转换图片与 PDF | JPG、PNG、WebP 互转；图片转 PDF；PDF 按页导出图片、合并或拆分 | 透明图片转 JPG 会铺白；PDF 原有数字签名不会保留 |
| 处理文档与表格 | TXT、HTML、DOCX、PDF 的文字转换；XLSX、CSV、TSV、JSON 表格转换 | 文档转换重建文字，不保留完整排版或图片，也不做 OCR；表格读取首个工作表，不计算公式或保留样式 |
| 处理视频与动态照片 | 视频转 MP4、压缩/编码、截帧；视频或动态照片转 GIF；提取可用的 Motion Photo / Live Photo 视频 | H.264、HEVC、AV1 取决于设备编码器；GIF 无声音且最长 60 秒；静态照片不能恢复缺失的动态内容 |
| 整理压缩包与音频 | ZIP、TAR.GZ 打包和受支持的解压；从媒体提取 M4A 或 WAV；SHA-256 / MD5 校验 | ZIP 可设 AES 密码，TAR.GZ 无密码；不支持 RAR5、分卷 RAR 或 MP3 编码 |
| 批量查看与管理 | 多文件导入、缩略图和原生预览、批量保存/分享/删除 | 实际格式取决于文件内容与设备能力 |

同格式批次会隐藏无意义的源格式目标；混合文件只显示共同支持的操作。图片、PDF、影音和有限范围内的文档可在应用中预览。Office 预览展示可读取内容，不保证完整还原版式、图形或公式。

## 从导入到保存

1. 在首页从照片或文件中选择要处理的内容。取消选择不会清空已有输入。
2. 查看缩略图；多文件可左右滑动，也可打开原生预览。
3. 选择目标格式。当前工具的参数从设置入口调整。
4. 按住并上滑玻璃罩，移动到中央后松手开始转换；转换成功后才会出现完成标记。
5. 预览结果后再保存：图片和视频默认写入系统媒体库，其他文件可用系统文件选择器选择位置；结果也可批量分享或删除。

普通切到后台时，前台服务会继续处理。强制停止、进程被系统终止或系统运行时限到达时，不保证编码继续；已完成结果会保留，未完成任务会标记为中断。

## 图片输出

默认保留方向校正后的原始尺寸，并使用最高编码质量。需要控制文件大小时，可打开高级选项，选择原图、4K、1080P、720P 或自定义宽高，并单独调整 JPEG / WebP 质量。预设保持比例且不会放大；关闭保持比例后，自定义宽高可以精确匹配，也可能拉伸图像。

尺寸限制为宽高各 1–32768、最多 64 Mi 像素，并受设备内存影响。资源不足时会明确失败，不会悄悄缩小尺寸。

“最高质量”不代表输出字节与原文件完全相同：JPEG 仍有损；图片处理使用方向校正后的 8 位 sRGB 位图，不保证完整保留 EXIF、ICC、HDR 或广色域信息。透明图片转 JPEG 会铺白；GIF 图片工具只处理首帧。Android 10 及以上的最高质量 WebP 可使用无损路径，Android 8/9 的旧编码器不承诺无损。

## 外观与语言

可以选择系统、浅色或深色主题；界面语言可跟随系统或手动切换中文/English。背景支持动态光晕、原创壁纸、纯色、HEX 和自选照片，也可分别调整照片的浅色与深色遮罩。Android 13 及以上提供完整 RuntimeShader 折射效果；较旧系统会降级显示，但转换功能仍可用。系统减少动画或省电设置可能减弱动效。

## 版本与设备要求

- [下载轻转工具箱 1.5.4](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/tag/v1.5.4-preview)；[查看所有版本](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases)。安装包与历史预览版分开提供。
- 最低 Android 8.0 / API 26。完整玻璃折射效果需要 Android 13 / API 33 或更高版本。
- 视频编码依赖手机实际提供的硬件/厂商编码器；列出某种编码格式不代表所有设备、分辨率和参数组合都能成功。
- 1.5.4 的真机启动、视觉、触控、TalkBack、后台行为及性能仍需设备验收；编译或包体检查通过不等于真机验收通过。

## 从源码构建

需要 JDK 17 或更高版本（建议 JDK 21）、Android SDK Platform 36、Build Tools 36.0.0 和 Python 3。首次构建需联网获取 Google / Maven 依赖。

```sh
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
python3 setup_tools.py
python3 build.py --variant release
```

也可在 Android Studio 中打开 `android/`。正式包使用 `cn.qingzhuan.toolbox`，与旧正式版和预览版分开安装。`release` 构建必须显式提供独立发布 keystore 及其签名环境变量；脚本不会退回开发密钥。不要把密钥、口令或签名文件提交到仓库。

更多信息： [架构](docs/ARCHITECTURE.md) · [构建与签名](docs/BUILDING.md) · [1.5.4 验证记录](docs/RELEASE_1_5_4.md) · [设备验收计划](docs/DEVICE_TEST_PLAN.md) · [当前检查点](docs/CURRENT_CHECKPOINT.md)

## 仓库结构

- `android/`：当前 Android 应用与 Gradle 构建入口。
- `app/`：转换引擎、任务会话与平台桥接代码。
- `docs/`：架构、构建、版本记录、验证计划与许可审查。
- `libs/`、`licenses/`、`THIRD_PARTY_NOTICES.txt`：第三方依赖与声明。
- `tests/`：测试源码、夹具与检查脚本。
- `src/`、`App.tsx` 和 npm 配置包含历史实现或测试材料；当前 APK 由 `android/` 构建。

## 隐私与许可

应用不申请 INTERNET 或全盘读取权限。它将输入、结果、背景和诊断保存在应用私有目录；媒体通过 MediaStore 处理，文档使用系统文件选择器，分享只授予所选结果的 URI。支持最多 20 个输入文件、单文件 512 MiB、总计 1 GiB；私有数据不是云备份，卸载前请先导出。

项目声明采用 GPL-3.0-only。现有许可审查将 GPL 与 Junrar / UnRAR 条款的组合分发兼容性记为未解决；不能据此宣称整个 APK 的公开分发已确认合规。Kyant 使用 Apache-2.0；原创壁纸与生成器为 CC0。详见[许可审查](docs/LICENSE_REVIEW.md)、[`licenses/`](licenses/) 与 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt)。本文不构成法律意见。
