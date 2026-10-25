package cn.qingzhuan.app

import android.graphics.Bitmap
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class HomeFile(
    val id: String,
    val name: String,
    val size: Long,
    val kind: String = "",
    val tools: List<String> = emptyList(),
    val audioTargets: List<String> = emptyList(),
    val format: String = "",
    val videoTargets: List<String> = emptyList(),
    val durationMs: Long = 0,
)

data class HomeTask(
    val id: String = "",
    val seq: Long = 0,
    val kind: String = "",
    val status: String = "idle",
    val message: String = "",
    val value: Int = -1,
    val files: List<HomeFile> = emptyList(),
)

data class HomeSnapshot(
    val revision: Long = -1,
    val inputs: List<HomeFile> = emptyList(),
    val outputs: List<HomeFile> = emptyList(),
    val busy: Boolean = false,
    val task: HomeTask = HomeTask(),
    val warning: String = "",
    val request: JSONObject = JSONObject(),
    val pendingConversion: Boolean = false,
)

fun JSONArray.strings() = List(length()) { optString(it) }

fun JSONArray.files() =
    List(length()) {
        val f = optJSONObject(it) ?: JSONObject()
        HomeFile(
            f.optString("id"),
            f.optString("name"),
            f.optLong("size"),
            f.optString("kind"),
            f.optJSONArray("tools")?.strings() ?: emptyList(),
            f.optJSONArray("audioTargets")?.strings() ?: emptyList(),
            normalizedSuffix(f.optString("file", f.optString("name"))),
            f.optJSONArray("videoTargets")?.strings() ?: emptyList(),
            f.optLong("durationMs"),
        )
    }

fun JSONObject.homeTask() =
    HomeTask(
        optString("id"),
        optLong("seq"),
        optString("kind"),
        optString("status"),
        optString("message"),
        optInt("value", -1),
        optJSONArray("files")?.files() ?: emptyList(),
    )

fun JSONObject.homeSnapshot(): HomeSnapshot {
    return HomeSnapshot(
        optLong("revision"),
        optJSONArray("inputs")?.files() ?: emptyList(),
        optJSONArray("outputs")?.files() ?: emptyList(),
        optBoolean("busy"),
        (optJSONObject("task") ?: JSONObject()).homeTask(),
        optString("warning"),
        optJSONObject("request") ?: JSONObject(),
        optBoolean("pendingConversion"),
    )
}

data class HomeTool(
    val id: String,
    val title: String,
    val formats: List<String>,
    val description: String,
)

val HOME_TOOLS =
    listOf(
        HomeTool("gif", "视频 / 动态照片转 GIF", listOf("gif"), "截取片段，调整尺寸、帧率和质量；不含声音。"),
        HomeTool("video", "视频压缩与编码", listOf("h264", "hevc", "av1"), "MP4输出；可调分辨率、码率、帧率和截取范围，仅使用本机实际编码器。"),
        HomeTool("motionvideo", "动态照片导出视频", listOf("mp4"), "从Motion Photo或Live Photo配对中取出视频部分并输出MP4。"),
        HomeTool("videoframe", "视频截图", listOf("jpg", "png"), "导出截取起点的一帧画面。"),
        HomeTool("image", "图片转换", listOf("jpg", "png", "webp"), "透明图片转 JPG 会铺白；GIF 只处理首帧。"),
        HomeTool("imagepdf", "图片成册", listOf("pdf"), "按所选顺序，每张图片一页。"),
        HomeTool("pdfimage", "PDF 转图片", listOf("jpg", "png"), "每页导出为独立图片。"),
        HomeTool("merge", "合并 PDF", listOf("pdf"), "至少两份 PDF；原数字签名不保留。"),
        HomeTool("split", "PDF 拆页", listOf("pdf"), "每页一个 PDF，可指定页码。"),
        HomeTool(
            "document",
            "文档文字转换",
            listOf("txt", "html", "docx", "pdf"),
            "只重建文字，不保留版式或图片；无 OCR。",
        ),
        HomeTool("table", "表格转换", listOf("xlsx", "csv", "tsv", "json"), "仅首个工作表，不计算公式，不保留样式。"),
        HomeTool("unpack", "解压文件", listOf(""), "不支持 RAR5、分卷或链接；最多 2000 项 / 1 GiB。"),
        HomeTool("audio", "音频提取", listOf("m4a", "wav"), "M4A 提取 AAC；WAV 使用手机解码器。"),
        HomeTool("pack", "压缩打包", listOf("zip", "tar.gz"), "ZIP 支持 AES 密码，TAR.GZ 无密码。"),
        HomeTool("hash", "文件校验", listOf("txt"), "SHA-256 与 MD5 校验文本。"),
    )

fun formatSize(n: Long) =
    when {
        n < 1024 -> "$n B"
        n < 1048576 -> "%.1f KiB".format(n / 1024.0)
        else -> "%.1f MiB".format(n / 1048576.0)
    }

class HomeActions(
    val pick: () -> Unit,
    val pickPhotos: () -> Unit,
    val convert: (String, String, JSONObject, List<String>) -> Boolean,
    val cancel: (String) -> Unit,
    val save: (List<String>) -> Unit,
    val share: (String) -> Unit,
    val open: (String) -> Unit,
    val removeInputs: (List<String>) -> Unit,
    val removeOutputs: (List<String>) -> Unit,
    val setTheme: (String) -> Unit,
    val readLicenses: () -> String,
    val haptic: () -> Unit,
    val notice: (String) -> Unit,
    val preview: (String, Boolean) -> Bitmap?,
    val pickBackground: () -> Unit,
    val pickBackgroundPhotos: () -> Unit,
    val setBackground: (String, Float) -> Unit,
    val backgroundFile: () -> File,
    val acknowledge: (String) -> Unit = {},
    val openInput: (String) -> Unit = {},
    val setBackgroundStyle: (BackgroundStyle) -> Unit = {},
)

fun normalizedSuffix(name: String): String {
    val n = name.lowercase()
    return if (n.endsWith(".tar.gz")) "tar.gz"
    else n.substringAfterLast('.', "").let { if (it == "jpeg") "jpg" else it }
}

fun shortcutFormats(tool: HomeTool?, files: List<HomeFile>): List<String> {
    if (tool == null || files.isEmpty()) return emptyList()
    if (tool.id == "image") {
        val common =
            files.map { it.format.ifEmpty { normalizedSuffix(it.name) } }.distinct().singleOrNull()
        return tool.formats.filter { it != common } +
            if (files.all { "imagepdf" in it.tools }) listOf("pdf") else emptyList()
    }
    return tool.formats.filter { value ->
        (tool.id != "audio" || files.all { value in it.audioTargets }) &&
        (tool.id != "video" || files.all { value in it.videoTargets })
    }
}
