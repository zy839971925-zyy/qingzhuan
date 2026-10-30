# 轻转 1.5.5 正式版发布记录

日期：2026-10-30。正式稳定版，`versionCode 2`，`versionName 1.5.5`，包名 `cn.qingzhuan.toolbox`。

## 本版更新

固定分段玻璃底座并保留上层镜片压力/拉伸；整理格式和菜单错峰、按钮居中弹性、预览载入和元信息过渡、稳定ID列表重排及参数披露。多结果复用原生堆叠预览；补齐层级返回、删除优先取消及旧页/同帧动作保护。中英文界面和原有转换能力保留。详细实现过程见 `docs/DEVELOPMENT_1_5_5.md` 与 `docs/superpowers/plans/2026-10-30-motion-1.5.5.md`。

## APK 与包身份

- 文件：`Qingzhuan-Toolbox-1.5.5.apk`，15,220,813 字节。
- SHA-256：`81ff97463de6c1222434a6fe72dee36d29c0408cc7ee413a7796678df7a78dff`。
- 包名/版本：`cn.qingzhuan.toolbox` / `2` / `1.5.5`。
- 签名：APK Signature Scheme v2 验证通过；证书 SHA-256 `a077d92842d12b607fede22c6168bc0fff92dcbf9c515529f60d5361f4bd2198`。
- 正式 1.5.4 APK 的包名及签名指纹与本版一致，本版 `versionCode` 更高。此身份信息符合 Android 同一应用更新的基本条件；本次未在真机上实装覆盖安装，亦未验证应用数据迁移。
- Build Tools 36 `zipalign -c -P 16 -v 4` 验证通过；APK 中没有 `INTERNET` 权限。

正式下载：[`Qingzhuan-Toolbox-1.5.5.apk`](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/download/v1.5.5/Qingzhuan-Toolbox-1.5.5.apk) · [GitHub Release](https://github.com/zy839971925-zyy/qingzhuan-android-toolbox/releases/tag/v1.5.5)。

## 验证

- `scripts/check_image_policy.py`：12 项尺寸、默认值、Exif 与范围检查通过。
- `scripts/check_localization.py`：9 项实际 Java 文案检查、静态 UI 文案覆盖、原生错误文案及文件名/无障碍边界检查通过。
- `:app:testReleaseUnitTest` 定向检查：`Motion155Test` 8 项、`Refined154Test` 2 项；共 10 项，0 失败、0 错误、0 跳过。测试 XML 位于 `handoff/logs/final155-Motion155Test.xml` 与 `handoff/logs/final155-Refined154Test.xml`。
- `python3 build.py --variant release`：Gradle `BUILD SUCCESSFUL`，50 项任务（34 执行、16 已是最新）；Release lint 通过。
- `apksigner` 核对包签名；`aapt` 核对包名、版本和权限；16 KiB ZIP 对齐校验通过。

构建过程有 AndroidX `libandroidx.graphics.path.so` 未剥离、旧 API/Gradle 弃用提示及 SDK XML 版本提示；构建仍成功。未跑完整回归套件。

## 未验证与许可

未使用模拟器或真机；小米设备上的启动、视觉/触摸、TalkBack、大字体/横屏、后台行为与帧率没有验收结论。GPL 与 Junrar/UnRAR 的组合许可问题仍未解决；详见 `LICENSE_REVIEW.md`，本次发布不代表已取得兼容性或法律结论。
