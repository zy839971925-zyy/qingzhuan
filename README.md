<p align="center">
  <img src="docs/assets/qingzhuan-icon.svg" width="96" alt="轻转工具箱图标">
</p>

<h1 align="center">轻转工具箱</h1>

<p align="center">在 Android 手机上转换、预览与整理日常文件。</p>
<p align="center">本地处理 · 批量操作 · 自由定制</p>

<p align="center"><a href="README.md">简体中文</a> · <a href="README.en.md">English</a></p>
<p align="center"><a href="https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/download/v1.5.5/Qingzhuan-Toolbox-1.5.5.apk">下载轻转工具箱 1.5.5</a> · <a href="https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases">所有版本</a></p>

轻转工具箱把图片与 PDF、文档与表格、视频与动态照片、音频和压缩包工具放进一个应用。文件在设备本地处理，不需要上传到在线转换服务；通过 Android 系统界面选择文件、查看结果，再决定保存或分享的位置。

## 一个工具箱，覆盖常见文件

| 类型 | 可以做什么 |
| --- | --- |
| 图片与 PDF | JPG、PNG、WebP 互转；图片转 PDF；PDF 按页导出图片、合并或拆分 |
| 文档与表格 | 转换 TXT、HTML、DOCX、PDF 中可读取的文字；转换 XLSX、CSV、TSV、JSON 表格 |
| 视频与动态照片 | 视频转换、压缩、截帧；导出 GIF；提取 Motion Photo / Live Photo 中可用的视频 |
| 音频与压缩包 | 从媒体提取 M4A / WAV；创建或解压 ZIP、TAR.GZ；计算 SHA-256 / MD5 |
| 批量整理 | 一次导入多个文件，浏览缩略图和预览，再批量保存、分享或删除结果 |

## 转换很直接

1. 从照片或 Android 文件选择器导入文件。
2. 查看缩略图；多文件可左右浏览，也可打开原生预览。
3. 选择目标格式，按需调整当前工具的参数。
4. 按住玻璃面板向上滑动，移动到中央后松手开始转换。
5. 预览结果，再保存到媒体库或自己选择的文件夹，也可以分享。

图片默认使用方向校正后的原始尺寸和最高编码质量。高级选项可选原图、4K、1080P、720P 或自定义尺寸，并分别调节 JPEG / WebP 质量；图片和 PDF 输出参数相互独立。

## 为顺手操作打磨的界面

玻璃滑动底座保持稳定，活动镜片会弹性跟手；格式和菜单依次出现，按钮有轻柔按压反馈。缩略图载入渐显，多个输入或结果都可逐项预览并管理；设置等子页面支持返回上一级，删除确认默认先取消。

主题可跟随系统或切换浅色、深色；界面支持系统语言、简体中文和 English。背景可选动态光晕、内置壁纸、纯色、HEX 色值或自己的照片，并可分别调节浅色与深色照片遮罩。

## 下载与兼容性

- [下载轻转工具箱 1.5.5 APK](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/download/v1.5.5/Qingzhuan-Toolbox-1.5.5.apk) · [查看所有版本](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases)
- 最低 Android 8.0 / API 26；完整玻璃折射效果需要 Android 13 / API 33 或更高版本。
- 视频编码能力由手机提供的硬件或厂商编码器决定，实际支持因设备、分辨率和参数而异。

<details>
<summary>支持范围与重要限制</summary>

- 透明图片转 JPEG 会铺白；PDF 合并、拆分或转换不会保留原 PDF 的数字签名。
- 文档转换重建可读取的文字，不等同于完整排版转换，也不包含 OCR；表格读取首个工作表，不计算公式或保留样式。
- GIF 无声音，最长 60 秒；静态照片不能恢复已经缺失的动态内容。
- ZIP 可设 AES 密码，TAR.GZ 不支持密码；不支持 RAR5、分卷 RAR 或 MP3 编码。
- 图片高级选项支持每边 1–32768 像素、最多 64 Mi 像素，但仍受手机内存限制。JPEG 仍有损；图片输出不保证完整保留 EXIF、ICC、HDR 或广色域信息。
- 普通切到后台时，前台服务可继续处理；强制停止、进程被系统终止或系统运行时限到达时，转换可能中断。

</details>

## 隐私与源码

应用不申请 INTERNET 或全盘读取权限。它使用系统文件选择器读取你选中的文件，并将输入、结果和设置保存在设备本地；媒体写入系统媒体库，分享只开放你选中的结果。最多支持 20 个输入文件、单文件 512 MiB、总计 1 GiB；卸载前请先导出需要保留的文件。

- [架构与实现](docs/ARCHITECTURE.md) · [构建与签名](docs/BUILDING.md) · [设备验收计划](docs/DEVICE_TEST_PLAN.md) · [变更记录](CHANGELOG.md)
- 项目声明采用 GPL-3.0-only；GPL 与 Junrar / UnRAR 条款组合分发的兼容性仍未解决。依赖清单见 [`licenses/`](licenses/) 与 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt)。

正式版使用应用 ID `cn.qingzhuan.toolbox`，与旧预览包可并存；升级既有正式版需要沿用同一签名密钥。正式构建密钥与口令不得提交到仓库。
