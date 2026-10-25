<p align="center">
  <img src="docs/assets/qingzhuan-icon.svg" width="104" alt="轻转图标">
</p>

# 轻转

<p align="center">在 Android 手机上转换与整理图片、视频、文档和压缩包。</p>

<p align="center"><a href="README.md">简体中文</a> · <a href="README_EN.md">English</a></p>

<p align="center"><a href="https://github.com/zy839971925-zyy/qingzhuan/releases">查看 APK 与所有版本</a></p>

轻转是一款面向 Android 的本地文件转换工具：在同一处处理图片与 PDF、文档与表格、视频与动态照片、音频和压缩包。文件由设备的媒体能力和应用内转换引擎处理，无需上传到在线转换服务；通过系统选择器导入和保存，操作范围由你选择。

当前仓库对应 **1.5.3 预览版**，包含图片、文档和批量文件流程，以及首页与预览界面的自定义背景、玻璃面板和连续动效。APK 将以测试预览构建单独发布，不代表最终正式版；该版本尚未完成真机视觉、无障碍和性能验收。

## 主要功能

| 你要做的事 | 轻转提供的工具 | 需要知道的边界 |
| --- | --- | --- |
| 转换图片与 PDF | JPG、PNG、WebP 互转；图片转 PDF；PDF 按页导出图片；PDF 合并与拆页 | 透明图片转 JPG 会铺白；PDF 原有数字签名不会保留 |
| 处理文档与表格 | TXT、HTML、DOCX、PDF 文字转换；XLSX、CSV、TSV、JSON 表格转换 | 文档转换只重建文字，不保留原排版、图片或 OCR；表格只读首个工作表，不计算公式、不保留样式 |
| 处理视频和动态照片 | 视频转 MP4、压缩/编码、截帧；视频或动态照片转 GIF；提取 Motion Photo / Live Photo 中可用的视频 | H.264、HEVC、AV1 取决于手机实际提供的编码器；GIF 无声音，最长 60 秒；静态照片本身不能恢复动态内容 |
| 管理压缩包和音频 | ZIP、TAR.GZ 打包；解压；从媒体提取 M4A 或 WAV 音频 | ZIP 可设 AES 密码，TAR.GZ 无密码；RAR 仅支持 RAR4 解压，不支持 RAR5、分卷和链接 |
| 校验与整理文件 | SHA-256 / MD5 校验文本；批量选择、预览、保存、分享和删除 | 具体可处理格式还取决于源文件内容与设备能力 |

同格式批次会过滤掉无意义的源格式目标；混合文件按它们共同支持的操作路由。应用通过 Android 系统选择器读取用户明确选取的文件；媒体写入系统媒体集合，其他文档可通过系统选择器指定保存位置。

## 版本与兼容性

- [查看 APK 与所有版本](https://github.com/zy839971925-zyy/qingzhuan/releases)（预览 APK 与最终正式版会分别标明）。

- 源码快照：`1.5.3-glass`，预览应用 ID：`cn.qingzhuan.app.preview`。它与旧版正式应用不是同一个包；安装预览版时不要为此卸载旧应用或清除旧数据。
- 最低配置 Android 8.0 / API 26。完整 RuntimeShader 玻璃效果需要 Android 13 / API 33 或更高版本，较旧系统使用降级效果。
- 视频编码由设备的硬件/厂商编码器决定；支持某种编码格式不代表任意手机、分辨率或参数组合都能成功。
- 1.5.3 有构建与包体检查记录，但尚无真机视觉、无障碍、触控延迟或帧率验收结果。编译成功不等于设备验收通过。

## 从源码构建

需要 Android SDK Platform 36、Build Tools 36.0.0 和 JDK 17 或更高版本（建议 JDK 21）。在仓库根目录运行：

```sh
python3 setup_tools.py
python3 build.py
```

也可以在 Android Studio 中打开 `android/`。首次构建需要访问 Google / Maven 仓库。`build.py` 默认生成 release 变体的预览 APK；若本机没有开发签名，会在源码目录之外创建独立密钥。该密钥不是旧版正式签名，勿将其或任何签名私钥提交到仓库。

项目测试和历史验证记录见 [`docs/VALIDATION.md`](docs/VALIDATION.md)、[`docs/DEVICE_TEST_PLAN.md`](docs/DEVICE_TEST_PLAN.md) 与 [`docs/RELEASE_1_5_3.md`](docs/RELEASE_1_5_3.md)。文档记录的历史结果不代表本次重新运行或真机验证。

## 项目结构

- `android/`：当前 Android 应用与 Gradle 构建入口。
- `app/`：Java 转换引擎、任务会话与平台桥接。
- `docs/`：架构、版本说明、验证计划及许可复核。
- `libs/`、`licenses/`、`THIRD_PARTY_NOTICES.txt`：随包依赖及其声明。
- `tests/`：测试源码、测试夹具与运行脚本。
- `src/`、`App.tsx` 和 npm 配置包含历史实现或测试材料；1.5.3 APK 的构建入口是 `android/`。

## 许可与公开状态

项目声明采用 GPL-3.0-only。源码包中的 [`docs/LICENSE_REVIEW.md`](docs/LICENSE_REVIEW.md) 将 GPL 与应用所用 Junrar/UnRAR 条款的组合分发兼容性记为未解决。UnRAR 条款允许将其用于处理 RAR 档案并随其他软件包分发，同时禁止用它重建 RAR 压缩算法或开发兼容的 RAR 压缩软件；这项限制说明本身不是 GPL 组合许可，也不能代替相应权利人的授权或有依据的许可复核。第三方原始条款见 [`licenses/`](licenses/) 与 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt)。本文不构成法律意见。

<!-- The displayed SVG is a documentation rendering of the bundled adaptive icon's exact background and foreground vector paths. -->
