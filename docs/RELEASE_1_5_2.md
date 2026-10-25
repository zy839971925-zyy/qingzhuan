# 轻转1.5.2 · 原生媒体与后台任务

versionCode10，versionName1.5.2-media，应用ID cn.qingzhuan.app.preview，图标名称“轻转预览1.5.2”。保持私有预览签名，能覆盖预览1.4/1.5及同签名1.5.1；不能覆盖旧1.3原应用，不删除原数据。源码基于已持久保存的53497bf恢复；未持久保存的1.5.1工作区曾丢失，相关功能已在本版重做。

## 用户截图与历史需求

- 主球恢复1.4 Kyant折射(.28r/.42r)、高光/阴影/微量白色材质，移除1.5饱和蓝色焦散叠层。共享手势p、半圆收缩为球、可逆拖动、有效区域松手、真实引擎、最短1200ms非线性时钟、成功绿勾与原路返回保留。
- “转换为”入口、简短格式/主题/背景/媒体设置使用实际Kyant背景采样和透镜；少量格式可横拖选中胶囊。展开更多工具/格式或数量>4时是独立玻璃按钮网格，有界滚动；退出转场保留快照；展开时隐藏并禁用主球，避免误拖提交。
- 导入真实小预览，单张静止、多张重叠可左右滑。完成/导出列表、导入记录有图片/PDF首页/视频首帧缩略图，LazyColumn仅组合可见项、缩略图串行解码。点击缩略图/眼睛按钮直接应用内预览。
- 私有原生预览：图片缩放移动、原生GIF播放（API28+ ImageDecoder，API26/27 Movie）、PDF惰性分页、文本/HTML/JSON/CSV、DOCX文字、XLSX首个sheet1内容、ZIP前200项、Media3原生影音播放器。预览不要求保存；输出可直接分享。Office不是版式渲染/编辑器，不重建图片或计算公式；文字最多40000字符，表格最多200行16列、单XML4MiB；不支持类型明确提示。
- 照片/文件双入口、图片/视频默认相册保存、其他文件使用SAF、批量保存/全选删除、三种持久背景及自定义模糊、自动深浅色、系统栏、启动中断误判修复保留。

## 新增实际转换

- 视频/录屏转GIF：截取起止时间、保持比例的最长边128–960、5–30fps、三档质量（RGB精度预量化+median-cut/Floyd–Steinberg）、循环/一次。GIF帧时间按10ms累积误差分配，避免30fps被截成30ms而加速。逐帧处理，不把整段全部帧留内存；最多60秒、2亿处理像素、约128MiB输出预算。尺寸/fps/时长组合超预算会明确拒绝。
- Motion Photo：读取XMP MotionPhoto长度、MicroVideoOffset及HEIC/AVIF顶层mpvd，验证范围和ftyp；只提声明的视频部分。Apple Live Photo可导入其MOV，或精确同名照片/MOV配对，转换时去重为一个视频。仅静态照片、云相册剥离的视频、未支持的厂商私有封装不能恢复动态内容。
- 视频压缩与编码：Media3 Transformer1.11.1 +本机MediaCodec，MP4容器，H.264/HEVC/AV1按设备编码器开放；可调输出最长边、目标码率100–80000kbps、帧率上限1–60、片段和静音。请求非默认VideoEncoderSettings会强制视频编码，避免仅换封装忽略码率；出文件后用MediaExtractor核验实际编码。没有软件编码器兜底，不把AV1/HEVC冒充成H.264。具体分辨率/码率/HDR组合仍可能被硬件拒绝。
- 附加工具：视频帧导出PNG/JPG、动态照片视频导出MP4。原有Java图片/PDF/文档/表格/打包解包/哈希/音频提取继续保留；Motion Photo音轨先取嵌入视频再提取。

## 后台与恢复

- 每个实际文件任务由前台服务维护系统通知和有界partial wake lock；AppSession是唯一引擎/任务源，服务和Activity可分别订阅。取消、结束、失败、超时均停止前台状态并释放唤醒锁。
- 转换请求、周期进度和独立lastConversion落入原子会话检查点；页面重建或切回后绑定同一任务ID。未确认的成功结果恢复球内反馈/结果；失败、中断也恢复选择并明确提示，显示后ack。
- onResume与通知触发的onNewIntent都不能清空尚未展示的任务，避免后台已经完成后点通知反而丢失结果界面。
- 正常切后台的实现已接入。不能抵抗用户强行停止、系统杀进程或厂商限制；这些情况标为中断并提示重新转换，不假装断点续编码。Android15+前台mediaProcessing/dataSync存在系统时间配额，通常6小时/24小时，超时明确中止。通知权限被拒绝时不假定通知可见。

## 验证事实与未验证部分

必要检查：Release编译；12项GIF writer/动态照片范围/媒体参数断言；ffprobe独立解码循环和单次GIF均3帧32x24、250ms；APK签名/版本/权限/对齐。未重跑历史大回归、未安装模拟器、未连接手机。

没有真机证据认证：Android15/17启动、Xiaomi后台保活、真实MediaCodec HEVC/AV1/HDR、照片Provider/相册显示、手势/光学一致性或60/90/120FPS。源码与构建通过不等于这些设备验收通过。异常编译曾出现增量重复声明，检索只见一个声明，清理派生输出/非增量重编译后通过；不把推测原因说成已独立证实。

## 许可与构建

Kotlin/Compose/Kyant1.0.6、原Java引擎；新增Media3 1.11.1、Square gifencoder0.10.1均Apache-2.0。仍无需Node/NDK/FFmpeg/模拟器。GPL及原Junrar/UnRAR附加限制的组合分发风险未解决，完整对应源码、历史与许可证一起提供，不宣称发行许可已审定。私钥单独备份，不在源码ZIP。

## 本轮依据

- https://developer.android.com/media/media3/transformer/getting-started
- https://developer.android.com/media/media3/transformer/customization
- https://developer.android.com/media/platform/motion-photo-format
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://github.com/square/gifencoder

源码审阅确认Media3 DefaultEncoderFactory.videoNeedsEncoding以非默认VideoEncoderSettings为真。本轮没有用虚拟机替代手机验收。最终构建前清理过失效的派生ART profile；原源码/SDK依赖未因此删除。
