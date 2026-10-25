# 1.5 原生改动只读复核

检查重点：新预览、背景捕获拓扑、图库保存与Activity生命周期。原有Java转换引擎未重写。

- 未发现Backdrop自采样循环：控件使用背景专用层，球在内容捕获层外。
- 缩略图与背景解码有边长/输入字节/缓存限制。
- 相册捕获失败/取消会删除当前partial条目，写入关闭后才解除IS_PENDING；源结果保留。
- 修正混合媒体/文档保存对旧Activity回调的引用。成功后写入续步偏好，当前RESUMED宿主消费；onPostResume通过decorView.post等待生命周期事件后再尝试，避免API29+同步恢复时序漏接。
- 修正背景导入随Activity销毁取消提交：应用context的单线程worker负责文件和偏好，当前宿主监听偏好。
- Android8–11模糊不能只用Modifier.blur，增加最大400边三次滑动box blur近似。

此为源码审查和编译范围，不等于设备生命周期/相册/GPU验收。
