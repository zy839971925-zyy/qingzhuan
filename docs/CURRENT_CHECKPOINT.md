# 当前检查点：Qingzhuan 1.5.5 正式版已构建核验

2026-10-30。源码和双语 README 已配置为正式稳定身份；Release APK 已使用原正式签名密钥构建并完成包名、版本、证书、权限和 16 KiB ZIP 对齐校验。下一步是提交/推送源码与文档，并发布官方 `v1.5.5` Release 资产。

- 包名 `cn.qingzhuan.toolbox`，versionCode 2，versionName `1.5.5`；与正式 1.5.4 APK 的包名及签名指纹一致，版本号递增。
- APK：`Qingzhuan-Toolbox-1.5.5.apk`，15,220,813 字节；SHA-256 `81ff97463de6c1222434a6fe72dee36d29c0408cc7ee413a7796678df7a78dff`。
- 签名指纹：`a077d92842d12b607fede22c6168bc0fff92dcbf9c515529f60d5361f4bd2198`；签名和 16 KiB ZIP 对齐已验证。
- 12 项图片策略检查、9 项实际 Java 文案检查及静态覆盖检查、10 项定向 JVM 测试通过；完整记录见 `docs/RELEASE_1_5_5.md`。
- 未做模拟器或真机安装、视觉、触摸、无障碍、后台/帧率和数据迁移验收；GPL/Junrar 组合许可问题仍未解决。

原生产签名密钥已存在于私有存储，未生成替代密钥；绝不将密钥、口令或本机私有路径提交到公开仓库。