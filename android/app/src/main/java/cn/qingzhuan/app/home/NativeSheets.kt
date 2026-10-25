package cn.qingzhuan.app

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.luminance
import com.kyant.backdrop.backdrops.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SmallAction(icon: String, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).controlGlass(LocalControlBackdrop.current, CircleShape),
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .13f)
            ),
        shape = CircleShape,
    ) {
        Glyph(icon, label, Modifier.size(21.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeSheet(
    name: String?,
    snapshot: HomeSnapshot,
    tool: HomeTool?,
    options: Map<String, String>,
    onOptions: (Map<String, String>) -> Unit,
    mode: String,
    background: BackgroundStyle,
    actions: HomeActions,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onUse: (List<String>) -> Unit,
    onTool: (HomeTool) -> Unit,
    available: List<HomeTool>,
    selectedFiles: List<HomeFile> = emptyList(),
    reduced: Boolean = false,
    backdrop: com.kyant.backdrop.Backdrop,
    onVisible: (Boolean) -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    GlassSheetHost(name,backdrop,reduced,onClose,onVisible) { activeName, dismiss ->
        val name=activeName
        Column(
            Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (name) {
                        "menu" -> "轻转"
                        "inputs" -> "导入记录"
                        "results" -> "已完成文件"
                        "tools" -> "转换工具"
                        "options" -> "转换选项"
                        "background" -> "背景与玻璃"
                        "import" -> "导入"
                        "background-import" -> "导入背景"
                        "licenses" -> "开源许可"
                        else -> "设置"
                    },
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                )
                SmallAction("x", "关闭", onClick = dismiss)
            }
            Spacer(Modifier.height(20.dp))
            when (name) {
                "menu" -> {
                    listOf(
                            Triple("folder", "导入记录", "inputs"),
                            Triple("clock-3", "已完成文件", "results"),
                            Triple("file-image", "背景与玻璃", "background"),
                            Triple("settings-2", "设置", "settings"),
                        )
                        .forEach { (icon, title, next) ->
                            ListItem(
                                headlineContent = { Text(title) },
                                leadingContent = { Glyph(icon) },
                                modifier = Modifier.clickable { onNavigate(next) },
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            )
                        }
                }
                "inputs",
                "results" -> FileList(snapshot, name == "inputs", actions, onUse, reduced)
                "tools" ->
                    available.forEach { t ->
                        ListItem(
                            headlineContent = { Text(t.title) },
                            supportingContent = { Text(t.description) },
                            trailingContent = { if (t.id == tool?.id) Glyph("check") },
                            modifier =
                                Modifier.clickable {
                                    onTool(t)
                                    dismiss()
                                },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )
                    }
                "options" -> {
                    if (tool?.id in listOf("gif","video","videoframe","motionvideo")) {
                        val duration=selectedFiles.ifEmpty { snapshot.inputs }.filter { tool!!.id in it.tools }.map { it.durationMs }.filter { it>0 }.minOrNull()?:0
                        MediaOptionsPanel(tool!!.id,duration,options,onOptions,reduced)
                    }
                    Text(tool?.description ?: "", color = c.onSurfaceVariant)
                    Spacer(Modifier.height(18.dp))
                    val keys =
                        when (tool?.id) {
                            "image" -> listOf("edge", "quality")
                            "pdfimage" -> listOf("edge", "quality", "pages", "password")
                            "split" -> listOf("pages", "password")
                            "document" -> listOf("encoding", "password")
                            "table" -> listOf("encoding")
                            "unpack" -> listOf("zipEncoding", "password")
                            "pack",
                            "merge" -> listOf("password")
                            else -> emptyList()
                        }
                    if (keys.isEmpty() && tool?.id !in listOf("gif","video","videoframe","motionvideo")) Text("此工具无需额外设置", color = c.onSurfaceVariant)
                    keys.forEach { k ->
                        OutlinedTextField(
                            value = options[k] ?: "",
                            onValueChange = { onOptions(options + (k to it)) },
                            label = {
                                Text(
                                    when (k) {
                                        "edge" -> "最长边（1280 / 2048 / 4096）"
                                        "quality" -> "画质（20–100）"
                                        "pages" -> "页码（留空全部，例如 1-3,5）"
                                        "password" -> "密码（可选）"
                                        "encoding" -> "编码（auto / UTF-8 / GB18030）"
                                        else -> "文件名编码（auto / GB18030）"
                                    }
                                )
                            },
                            visualTransformation =
                                if (k == "password") PasswordVisualTransformation()
                                else VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            singleLine = true,
                        )
                    }
                }
                "background-import" -> {
                    ListItem(
                        headlineContent = { Text("从照片导入") },
                        leadingContent = { Glyph("file-image") },
                        modifier =
                            Modifier.clickable {
                                onNavigate("background")
                                actions.pickBackgroundPhotos()
                            },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                    ListItem(
                        headlineContent = { Text("从文件导入") },
                        leadingContent = { Glyph("folder") },
                        modifier =
                            Modifier.clickable {
                                onNavigate("background")
                                actions.pickBackground()
                            },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
                "import" -> {
                    ListItem(
                        headlineContent = { Text("从照片导入") },
                        leadingContent = { Glyph("file-image") },
                        modifier =
                            Modifier.clickable {
                                dismiss()
                                actions.pickPhotos()
                            },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                    ListItem(
                        headlineContent = { Text("从文件导入") },
                        leadingContent = { Glyph("folder") },
                        modifier =
                            Modifier.clickable {
                                dismiss()
                                actions.pick()
                            },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
                "background" -> BackgroundSettings(background,actions,reduced){onNavigate("background-import")}
                "settings" -> {
                    Text("外观", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    GlassSegments(listOf(GlassChoice("system","自动"),GlassChoice("light","浅色"),GlassChoice("dark","深色")),mode,Modifier.fillMaxWidth(),reduced=reduced,onSelect=actions.setTheme)
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Text(
                        "所有转换在本机完成。最多 20 份输入、单份 512 MiB、合计 1 GiB。文档只重建文字，无 OCR；MP3编码暂不支持；视频转码依赖本机编码器。",
                        color = c.onSurfaceVariant,
                        lineHeight = 23.sp,
                    )
                    if (android.os.Build.VERSION.SDK_INT < 33)
                        Text(
                            "当前系统低于 Android 13：支持文件转换，但不能呈现 RuntimeShader 的完整玻璃折射。",
                            color = c.error,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    TextButton({ onNavigate("background") }) { Text("背景与玻璃") }
                    TextButton({ onNavigate("licenses") }) { Text("开源许可与修改说明") }
                    Text(
                        "${BuildConfig.VERSION_NAME} · Compose / Backdrop 1.0.6",
                        fontSize = 12.sp,
                        color = c.onSurfaceVariant,
                    )
                }
                "licenses" -> {
                    var text by remember { mutableStateOf("正在读取…") }
                    LaunchedEffect(Unit) {
                        text =
                            withContext(Dispatchers.IO) {
                                try {
                                    actions.readLicenses()
                                } catch (e: Exception) {
                                    e.toString()
                                }
                            }
                    }
                    Text(text, fontSize = 11.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun FileList(
    snapshot: HomeSnapshot,
    inputs: Boolean,
    actions: HomeActions,
    onUse: (List<String>) -> Unit,
    reduced: Boolean,
) {
    val files = if (inputs) snapshot.inputs else snapshot.outputs
    var selected by remember(inputs) { mutableStateOf(setOf<String>()) }
    var order by remember(inputs) { mutableStateOf(files.map { it.id }) }
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(files) {
        val ids = files.map { it.id }.toSet()
        selected = selected.intersect(ids)
        order = order.filter { it in ids } + files.map { it.id }.filter { it !in order }
    }
    val rows = order.mapNotNull { id -> files.find { it.id == id } }
    val selectedIds = rows.filter { it.id in selected }.map { it.id }
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallAction(
            if (selected.size == files.size && files.isNotEmpty()) "check-square" else "square",
            "全选",
            files.isNotEmpty(),
        ) {
            selected = if (selected.size == files.size) emptySet() else files.map { it.id }.toSet()
        }
        Text(
            if (selected.isEmpty()) "${files.size} 份文件" else "已选 ${selected.size} 项",
            Modifier.weight(1f),
            color = c.onSurfaceVariant,
            fontSize = 13.sp,
        )
        SmallAction(
            if (inputs) "arrow-up" else "download",
            if (inputs) "转换所选文件" else "保存所选结果",
            selected.isNotEmpty() && !snapshot.busy,
        ) {
            if (inputs) onUse(selectedIds) else actions.save(selectedIds)
        }
        SmallAction("trash", "删除所选文件", selected.isNotEmpty() && !snapshot.busy) {
            confirming = true
        }
    }
    AnimatedVisibility(confirming,enter=if(reduced)EnterTransition.None else fadeIn()+expandVertically(),exit=if(reduced)ExitTransition.None else fadeOut()+shrinkVertically()) {
        Column(Modifier.fillMaxWidth().padding(top=16.dp).controlGlass(LocalControlBackdrop.current, androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).padding(18.dp)) {
            Text("删除 ${selected.size} 项？",style=MaterialTheme.typography.titleMedium)
            Text("只删除应用内部副本，原文件和已保存的副本不受影响。",color=c.onSurfaceVariant,modifier=Modifier.padding(top=8.dp))
            Row(Modifier.align(Alignment.End)) {
                TextButton({confirming=false}){Text("保留")}
                TextButton({
                    confirming=false
                    if(inputs)actions.removeInputs(selectedIds)else actions.removeOutputs(selectedIds)
                    selected=emptySet()
                },enabled=selectedIds.isNotEmpty()&&!snapshot.busy){Text("删除",color=c.error)}
            }
        }
    }
    if (files.isEmpty())
        Text(
            if (inputs) "还没有导入文件" else "还没有转换结果",
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 32.dp),
        )
    LazyColumn(Modifier.fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp*.55f).dp)) {
    itemsIndexed(rows,key={_,file->file.id}) { index,f ->
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable {
                    selected = if (f.id in selected) selected - f.id else selected + f.id
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    f.id in selected,
                    { selected = if (it) selected + f.id else selected - f.id },
                )
                PreviewCard(f,actions,52.dp,output=!inputs){if(inputs)actions.openInput(f.id)else actions.open(f.id)}
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(f.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                    Text(formatSize(f.size), fontSize = 12.sp, color = c.onSurfaceVariant)
                }
            }
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (inputs) {
                    SmallAction("eye","预览 ${f.name}",!snapshot.busy){actions.openInput(f.id)}
                    SmallAction("arrow-up", "上移文件", index > 0 && !snapshot.busy) {
                        order =
                            order.toMutableList().also {
                                java.util.Collections.swap(it, index, index - 1)
                            }
                    }
                    SmallAction("arrow-down", "下移文件", index < rows.lastIndex && !snapshot.busy) {
                        order =
                            order.toMutableList().also {
                                java.util.Collections.swap(it, index, index + 1)
                            }
                    }
                } else {
                    SmallAction("download", "保存 ${f.name}", !snapshot.busy) {
                        actions.save(listOf(f.id))
                    }
                    SmallAction("eye", "预览 ${f.name}", !snapshot.busy) { actions.open(f.id) }
                    SmallAction("share-2", "分享 ${f.name}", !snapshot.busy) { actions.share(f.id) }
                }
            }
        }
        HorizontalDivider(color = c.outline.copy(alpha = .45f))
    }
    }
    if (!inputs && files.isNotEmpty())
        Text(
            "图片、视频默认保存到相册；其他文件选择保存位置。",
            fontSize = 12.sp,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )

}

fun fileGlyph(name: String, kind: String = ""): String {
    if (kind in listOf("pdf", "document", "table", "text")) return "file-text"
    val ext = name.substringAfterLast('.', "").lowercase()
    return when {
        kind == "image" || ext in listOf("png", "jpg", "jpeg", "webp", "avif", "heic", "gif") ->
            "file-image"
        kind == "archive" || ext in listOf("zip", "gz", "rar", "7z", "tar") -> "file-archive"
        kind == "media" || ext in listOf("m4a", "mp3", "wav", "aac", "mp4") -> "file-audio"
        else -> "file-text"
    }
}
