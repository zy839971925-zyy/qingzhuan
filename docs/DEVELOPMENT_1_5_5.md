# 1.5.5 可恢复进度

更新于 2026-10-30。当前采用正式稳定版身份：`cn.qingzhuan.toolbox`、versionCode 2、versionName `1.5.5`；与正式 1.5.4 APK 的包名及生产签名指纹一致。Release 构建已经用原生产签名成功，证据及精确 APK 哈希见 `RELEASE_1_5_5.md` 与 `handoff/APK-build-155.json`。私钥仅在仓库外的私有签名目录，不进入源码、Release 附件或本记录。

当前源码包括 1.5.5 动效、批量结果预览、层级返回和旧页动作保护等改进。10 项定向 JVM 测试、两组主机静态检查和 Release 构建通过。尚无模拟器/真机验收；应用安装覆盖和数据迁移没有实测结论。GPL/Junrar 组合许可疑点仍待解决，不将本次发布表述为许可已确认。

以下是实现期的阶段记录。开发期间的中间预览构建不是当前正式 APK 的签名/包身份或发布证明；正式 APK 核验以 `RELEASE_1_5_5.md`、`handoff/APK-build-155.json` 和 `handoff/Qingzhuan-1.5.5-signing.txt` 为准。历史方案与回归演进见 `docs/superpowers/plans/2026-10-30-motion-1.5.5.md`。

实现期曾针对玻璃底座的布局回归、面板返回/删除确认、旧页和同帧菜单动作隔离、文件/照片导入顺序等补充 Compose 测试；最终可复核测试 XML 为 `handoff/logs/final155-Motion155Test.xml` 与 `handoff/logs/final155-Refined154Test.xml`；`Motion155Test` 8 项与 `Refined154Test` 2 项全部通过。
