package cn.qingzhuan.app

import androidx.compose.ui.focus.focusProperties

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.backdrop.backdrops.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.*
import com.kyant.backdrop.highlight.*
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.*
import kotlinx.coroutines.*
import org.json.JSONObject

private enum class HomePhase {
    EMPTY,
    READY,
    CONVERTING,
    RETURNING,
    SUCCESS,
}

@Composable
fun NativeHome(
    snapshot: HomeSnapshot,
    conversionOutcome: HomeTask?,
    newIds: List<String>,
    importEpoch: Long,
    resetEpoch: Long,
    dark: Boolean,
    reduced: Boolean,
    mode: String,
    backgroundStyle: BackgroundStyle,
    actions: HomeActions,
) {
    val controls = rememberLayerBackdrop()
    CompositionLocalProvider(LocalControlBackdrop provides controls) {
        NativeHomeBody(
            snapshot,
            conversionOutcome,
            newIds,
            importEpoch,
            resetEpoch,
            dark,
            reduced,
            mode,
            backgroundStyle,
            actions,
            controls,
        )
    }
}

@Composable
private fun NativeHomeBody(
    snapshot: HomeSnapshot,
    conversionOutcome: HomeTask?,
    newIds: List<String>,
    importEpoch: Long,
    resetEpoch: Long,
    dark: Boolean,
    reduced: Boolean,
    mode: String,
    backgroundStyle: BackgroundStyle,
    actions: HomeActions,
    controlsBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
) {
    QingzhuanTheme(dark, backgroundStyle.mode == "custom") {
        val c = MaterialTheme.colorScheme
        val scope = rememberCoroutineScope()
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        var selected by remember { mutableStateOf(emptyList<String>()) }
        var toolId by remember { mutableStateOf("") }
        var target by remember { mutableStateOf<String?>(null) }
        var formatsExpanded by remember { mutableStateOf(false) }
        var fileEntrance by remember { mutableFloatStateOf(0f) }
        var sheet by remember { mutableStateOf<String?>(null) }
        var sheetVisible by remember { mutableStateOf(false) }
        val backdrop = rememberLayerBackdrop()
        var phase by remember { mutableStateOf(HomePhase.EMPTY) }
        var p by remember { mutableFloatStateOf(0f) }
        var horizontal by remember { mutableFloatStateOf(0f) }
        var touch by remember { mutableStateOf(false) }
        var animation by remember { mutableStateOf<Job?>(null) }
        var visualTask by remember { mutableStateOf<String?>(null) }
        var presentedTask by remember { mutableStateOf("") }
        var completed by remember { mutableStateOf(emptyList<HomeFile>()) }
        var fill by remember { mutableFloatStateOf(0f) }
        var clockRotation by remember { mutableFloatStateOf(0f) }
        var check by remember { mutableFloatStateOf(0f) }
        var options by remember {
            mutableStateOf(
                mapOf(
                    "edge" to "2048",
                    "quality" to "88",
                    "encoding" to "auto",
                    "pages" to "",
                    "password" to "",
                    "zipEncoding" to "auto",
                )
            )
        }
        val latest by rememberUpdatedState(snapshot)
        val terminal by rememberUpdatedState(conversionOutcome)
        var handledImport by remember { mutableLongStateOf(0) }
        val files = selected.mapNotNull { id -> snapshot.inputs.find { it.id == id } }
        val common = ToolRouting.common(files.map { it.tools })
        val tools = HOME_TOOLS.filter { it.id in common && (it.id != "merge" || files.size>=2) }
        val tool = tools.find { it.id == toolId } ?: tools.firstOrNull()
        val formats = shortcutFormats(tool, files)
        val visualBusy = phase == HomePhase.CONVERTING || phase == HomePhase.RETURNING
        val busy = snapshot.busy || visualBusy
        fun useFiles(ids: List<String>) {
            visualTask = null
            animation?.cancel()
            selected = ids.distinct()
            val fresh = snapshot.inputs.filter { it.id in ids }
            toolId =
                HOME_TOOLS.firstOrNull { it.id in ToolRouting.common(fresh.map { it.tools }) }?.id
                    ?: ""
            target = null
            formatsExpanded = false
            completed = emptyList()
            phase = if (ids.isEmpty()) HomePhase.EMPTY else HomePhase.READY
            p = 0f
            horizontal = 0f
            sheet = null
        }
        LaunchedEffect(importEpoch, visualBusy) {
            if (importEpoch > handledImport && !visualBusy) {
                handledImport = importEpoch
                useFiles((if (phase == HomePhase.READY) selected else emptyList()) + newIds)
            }
        }
        LaunchedEffect(resetEpoch) {
            if (resetEpoch > 0 && !busy) {
                animation?.cancel()
                useFiles(emptyList())
            }
        }
        LaunchedEffect(selected) {
            if (selected.isNotEmpty()) {
                fileEntrance = if (reduced) 1f else 0f
                if (!reduced)
                    animate(
                        0f,
                        1f,
                        animationSpec = tween(650, easing = CubicBezierEasing(.22f, 1f, .36f, 1f)),
                    ) { v, _ ->
                        fileEntrance = v
                    }
            }
        }
        LaunchedEffect(snapshot.inputs) {
            selected = selected.filter { id -> snapshot.inputs.any { it.id == id } }
            if (selected.isEmpty() && phase == HomePhase.READY) {
                phase = HomePhase.EMPTY
                target = null
            }
        }
        LaunchedEffect(snapshot.warning) {
            if (snapshot.warning.isNotEmpty()) actions.notice(snapshot.warning)
        }
        fun returnHome(after: HomePhase) {
            animation?.cancel()
            animation =
                scope.launch {
                    if (reduced) {
                        p = 0f
                        horizontal = 0f
                    } else {
                        val startX = horizontal
                        val startP = p
                        animate(
                            0f,
                            1f,
                            animationSpec =
                                tween(680, easing = CubicBezierEasing(.22f, 1f, .36f, 1f)),
                        ) { v, _ ->
                            p = startP * (1 - v)
                            horizontal = startX * (1 - v)
                        }
                    }
                    phase = after
                }
        }
        fun submit() {
            if (busy || visualTask != null || files.isEmpty() || tool == null || target == null)
                return
            if (tool.id == "merge" && files.size < 2) {
                actions.notice("合并至少需要两份 PDF")
                returnHome(HomePhase.READY)
                return
            }
            animation?.cancel()
            val id = "task-" + java.util.UUID.randomUUID().toString()
            visualTask = id
            completed = emptyList()
            phase = HomePhase.CONVERTING
            fill = 0f
            check = 0f
            sheet = null
            animation =
                scope.launch {
                    if (reduced) {
                        p = 1f
                        horizontal = 0f
                    } else {
                        val startX = horizontal
                        val startP = p
                        animate(
                            0f,
                            1f,
                            animationSpec = spring(dampingRatio = .668f, stiffness = 114.286f),
                        ) { v, _ ->
                            p = startP + (1 - startP) * v
                            horizontal = startX * (1 - v)
                        }
                    }
                }
            val opts =
                JSONObject(options)
                    .put("target", target)
                    .put("edge", options["edge"]?.toIntOrNull() ?: 2048)
                    .put("quality", options["quality"]?.toIntOrNull() ?: 88)
            if (tool.id == "pack" && target != "zip") opts.put("password", "")
            if (
                !actions.convert(
                    id,
                    if (tool.id == "image" && target == "pdf") "imagepdf" else tool.id,
                    opts,
                    files.map { it.id },
                )
            ) {
                visualTask = null
                returnHome(HomePhase.READY)
            }
        }
        // A running task may be reattached after Activity recreation; old terminal tasks stay in
        // history.
        LaunchedEffect(snapshot.task.id, snapshot.task.status, snapshot.pendingConversion) {
            if ((snapshot.pendingConversion || snapshot.busy && snapshot.task.kind == "convert") && visualTask == null && phase != HomePhase.RETURNING && phase != HomePhase.SUCCESS) {
                val request = snapshot.request
                selected = request.optJSONArray("selection")?.strings() ?: emptyList()
                toolId = request.optString("tool", toolId)
                request.optJSONObject("options")?.let { saved ->
                    target = saved.optString("target").ifEmpty { null }
                    options = saved.keys().asSequence().associateWith { saved.optString(it) }
                }
                visualTask = request.optString("id").ifEmpty { snapshot.task.id }
                phase = HomePhase.CONVERTING
                p = 1f
            }
        }
        LaunchedEffect(phase, presentedTask) {
            if (phase == HomePhase.SUCCESS && presentedTask.isNotEmpty()) actions.acknowledge(presentedTask)
        }
        LaunchedEffect(visualTask) {
            val id = visualTask ?: return@LaunchedEffect
            val clock = FeedbackClock()
            var previous = 0L
            while (phase == HomePhase.CONVERTING && !clock.done) {
                val now = withFrameNanos { it }
                val dt = if (previous == 0L) 0.0 else (now - previous) / 1e6
                previous = now
                val t = terminal?.takeIf { it.id == id } ?: latest.task
                if (t.id == id && t.status in listOf("error", "cancelled", "interrupted")) {
                    actions.notice(t.message)
                    actions.acknowledge(id)
                    visualTask = null
                    returnHome(if (selected.isEmpty()) HomePhase.EMPTY else HomePhase.READY)
                    break
                }
                if (
                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && abs(p - 1f) < .05f
                ) {
                    val finished = t.id == id && t.status == "complete"
                    if (finished) completed = t.files
                    clock.step(dt, if (t.id == id) t.value else -1, finished)
                    fill = clock.fill.toFloat()
                    clockRotation = (clock.elapsed % 1000 / 1000 * 360).toFloat()
                    check = (clock.check / 220).toFloat().coerceIn(0f, 1f)
                }
            }
            if (clock.done && visualTask == id) {
                presentedTask = id
                phase = HomePhase.RETURNING
                actions.haptic()
                returnHome(HomePhase.SUCCESS)
                visualTask = null
            }
        }
        BackHandler(
            enabled =
                sheet != null || formatsExpanded ||
                    busy ||
                    target != null ||
                    selected.isNotEmpty() ||
                    phase == HomePhase.SUCCESS
        ) {
            when {
                sheet != null -> sheet = null
                formatsExpanded -> formatsExpanded = false
                busy -> actions.notice("请等待当前任务结束，或点击取消")
                target != null -> {
                    target = null
                    returnHome(HomePhase.READY)
                }
                else -> useFiles(emptyList())
            }
        }
        val currentSubmit by rememberUpdatedState({ submit() })
        val armed = target != null && files.isNotEmpty() && phase == HomePhase.READY && !formatsExpanded
        val visible by
            animateFloatAsState(
                if (armed || visualBusy) 1f else 0f,
                if (reduced) snap() else tween(420),
                label = "glass entry",
            )
        BoxWithConstraints(Modifier.fillMaxSize().background(c.background).clipToBounds().focusProperties{canFocus=!sheetVisible}.semantics { if(sheetVisible) hideFromAccessibility() }) {
            val density = LocalDensity.current
            val w = maxWidth.value
            val h = maxHeight.value
            val landscape = w > h * 1.2f
            val scale = if (landscape) h / 874 else w / 402
            val travel = h * .531f
            val radius = GestureGate.radius(w, h, p)
            val centerX = w / 2 + horizontal
            val centerY =
                h - travel * p + (1 - visible) * 245 * scale * 1.35f * (1 - p.coerceIn(0f, 1f))
            DisposableEffect(w, h, reduced) {
                onDispose {
                    touch = false
                    if (phase == HomePhase.READY) {
                        animation?.cancel()
                        p = 0f
                        horizontal = 0f
                    }
                }
            }
            val allowGesture by rememberUpdatedState(armed && !busy && sheet == null && !sheetVisible)
            val latestCenter by rememberUpdatedState(Offset(centerX, centerY))
            val latestRadius by rememberUpdatedState(radius)
            val rootGesture =
                Modifier.pointerInput(w, h, reduced) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pos = down.position / density.density
                        if (!allowGesture || (pos - latestCenter).getDistance() > latestRadius + 8)
                            return@awaitEachGesture
                        animation?.cancel()
                        touch = true
                        val p0 = p
                        val x0 = horizontal
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        val start = down.position
                        val released =
                            drag(down.id) { change ->
                                val delta = (change.position - start) / density.density
                                val raw = p0 - delta.y / travel
                                p =
                                    when {
                                        raw < 0 -> raw * .25f
                                        raw > 1 -> 1 + (raw - 1) * .25f
                                        else -> raw
                                    }
                                horizontal = 150 * scale * tanh((x0 + delta.x) / (150 * scale))
                                tracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                            }
                        touch = false
                        if (
                            released &&
                                GestureGate.commit(
                                    p,
                                    horizontal,
                                    travel * (1 - p),
                                    44 * scale * 1.75f,
                                )
                        ) {
                            currentSubmit()
                        } else {
                            val velocity = tracker.calculateVelocity()
                            val fromX = horizontal
                            val fromP = p
                            animation =
                                scope.launch {
                                    if (reduced) {
                                        p = 0f
                                        horizontal = 0f
                                    } else {
                                        animate(
                                            0f,
                                            1f,
                                            initialVelocity =
                                                (-velocity.y / density.density / travel).coerceIn(
                                                    -3f,
                                                    3f,
                                                ),
                                            animationSpec =
                                                spring(dampingRatio = .668f, stiffness = 114.286f),
                                        ) { v, _ ->
                                            p = fromP * (1 - v)
                                            horizontal = fromX * (1 - v)
                                        }
                                    }
                                }
                        }
                    }
                }
            Box(Modifier.fillMaxSize().then(rootGesture)) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    Box(Modifier.fillMaxSize().layerBackdrop(controlsBackdrop)) {
                        AmbientBackground(backgroundStyle, dark, reduced, actions)
                    }
                    val scene =
                        Modifier.graphicsLayer {
                            val t = p.coerceIn(0f, 1f)
                            alpha = (1 - (t - .08f) / .54f).coerceIn(0f, 1f)
                            translationY = -t * 110 * density.density
                            if (Build.VERSION.SDK_INT >= 31) {
                                val b =
                                    if (reduced) 0f
                                    else ((t - .05f) / .55f).coerceIn(0f, 1f) * 13 * density.density
                                renderEffect =
                                    if (b > .1f)
                                        android.graphics.RenderEffect.createBlurEffect(
                                                b,
                                                b,
                                                android.graphics.Shader.TileMode.DECAL,
                                            )
                                            .asComposeRenderEffect()
                                    else null
                            }
                        }
                    Box(Modifier.fillMaxSize().then(scene)) {
                        when (phase) {
                            HomePhase.EMPTY ->
                                UploadLanding(
                                    Modifier.align(Alignment.Center).offset(y = (-12).dp),
                                    min(w - 56, h * .43f).dp,
                                    reduced,
                                ) {
                                    sheet = "import"
                                }
                            HomePhase.READY,
                            HomePhase.CONVERTING -> {
                                if (files.isNotEmpty()) {
                                    val fileY =
                                        if (landscape) 64f
                                        else if (h < 720) 70f else max(88f, h * .17f)
                                    val contentWidth = if (landscape) w * .45f else w
                                    Column(
                                        Modifier.offset(
                                                y =
                                                    (fileY +
                                                            (h * .5f - 70 - fileY) *
                                                                (1 - fileEntrance))
                                                        .dp
                                            )
                                            .width(contentWidth.dp)
                                            .graphicsLayer {
                                                scaleX = 1 + (1 - fileEntrance) * .12f
                                                scaleY = scaleX
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        FilePreviewDeck(
                                            files,
                                            actions,
                                            (if (landscape) 72 else if (h < 720) 80 else 104).dp,
                                            reduced=reduced,
                                        ) {
                                            if (!busy) sheet = "inputs"
                                        }
                                    }
                                    Column(
                                        Modifier.offset(
                                                x = (if (landscape) w * .5f else 28f).dp,
                                                y =
                                                    (if (landscape) 64f
                                                        else if (h < 720) max(fileY + 156, h * .34f)
                                                        else max(fileY + 192, h * .40f))
                                                        .dp,
                                            )
                                            .width((if (landscape) w * .5f - 28 else w - 56).dp)
                                            .graphicsLayer {
                                                alpha =
                                                    ((fileEntrance - .12f) / .88f).coerceIn(0f, 1f)
                                                translationY = (1 - fileEntrance) * 20.dp.toPx()
                                            }
                                    ) {
                                        GlassMenuButton(formatsExpanded,!busy,reduced) {formatsExpanded=!formatsExpanded}
                                        Spacer(Modifier.height(12.dp))
                                        val presentation=FormatPresentation(formatsExpanded||formats.size>4,
                                            if(formatsExpanded)conversionChoices(tools,files) else formats.map{ConversionChoice(tool?.id?:toolId,it,"")})
                                        AnimatedContent(presentation,transitionSpec={(fadeIn(tween(200)) togetherWith fadeOut(tween(130))).using(SizeTransform(clip=false,sizeAnimationSpec={_,_->spring(dampingRatio=.9f,stiffness=420f)}))},label="format capsule/grid") { shown->
                                            if(shown.grid) {
                                                val columns=if(density.fontScale>1.3f)2 else 3
                                                Column(Modifier.fillMaxWidth().heightIn(max=(if(landscape)max(90f,h-230)else min(232f,h*.29f)).dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                                    shown.choices.chunked(columns).forEach { row->
                                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                                            row.forEach { choice->GlassButton(GlassChoice(choice.key,choice.label,choice.detail),choice.tool==toolId&&choice.target==target,!busy&&shown==presentation,Modifier.weight(1f)) {toolId=choice.tool;target=choice.target;formatsExpanded=false;actions.haptic()} }
                                                            repeat(columns-row.size){Spacer(Modifier.weight(1f))}
                                                        }
                                                    }
                                                }
                                            } else GlassSegments(shown.choices.map{GlassChoice(it.target,it.label)},target,Modifier.fillMaxWidth(),!busy&&shown==presentation,reduced) {target=it;actions.haptic()}
                                        }
                                        Spacer(Modifier.height(18.dp))
                                        Row(
                                            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            SmallAction("settings-2", "转换选项", !busy) {
                                                sheet = "options"
                                            }
                                            SmallAction("plus", "添加文件", !busy) { sheet = "import" }
                                        }
                                    }
                                }
                            }
                            HomePhase.RETURNING,
                            HomePhase.SUCCESS -> {
                                if (completed.isNotEmpty())
                                    Column(
                                        Modifier.offset(
                                                y = (if (landscape) 32f else max(120f, h * .28f)).dp
                                            )
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        PreviewCard(
                                            completed.first(),
                                            actions,
                                            (if (landscape) 72 else 96).dp,
                                            output = true,
                                        ) {actions.open(completed.first().id)}
                                        Spacer(Modifier.height(18.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Glyph(
                                                "check",
                                                modifier = Modifier.size(17.dp),
                                                color =
                                                    if (dark) Color(0xFF85D5A2)
                                                    else Color(0xFF27824E),
                                            )
                                            Text(
                                                "转换完成",
                                                color =
                                                    if (dark) Color(0xFF85D5A2)
                                                    else Color(0xFF27824E),
                                                fontSize = 13.sp,
                                            )
                                        }
                                        Spacer(Modifier.height(17.dp))
                                        Text(
                                            completed.first().name,
                                            Modifier.padding(horizontal = 28.dp),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 18.sp,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Spacer(Modifier.height(7.dp))
                                        Text(
                                            formatSize(completed.sumOf { it.size }) +
                                                (if (completed.size > 1) " · ${completed.size} 份文件"
                                                else ""),
                                            fontSize = 12.sp,
                                            color = c.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(30.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                            SmallAction("download", "保存转换结果", !busy) {
                                                actions.save(completed.map { it.id })
                                            }
                                            SmallAction("plus", "继续转换下一个文件", !busy) {
                                                useFiles(emptyList())
                                                sheet = "import"
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
                // Actual Kyant library backdrop: seven-band refraction, adaptive highlight and
                // shadow.
                if (visible > .001f || visualBusy) {
                    Box(
                        Modifier.offset {
                                IntOffset(
                                    ((centerX - radius) * density.density).roundToInt(),
                                    ((centerY - radius) * density.density).roundToInt(),
                                )
                            }
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .size((radius * 2).dp)
                            .semantics {
                                contentDescription = "液态玻璃转换按钮，向上拖至中央松手"
                                customActions =
                                    listOf(
                                        CustomAccessibilityAction("直接开始转换") {
                                            if (allowGesture) {
                                                currentSubmit()
                                                true
                                            } else false
                                        }
                                    )
                            }
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { CircleShape },
                                effects = {
                                    vibrancy()
                                    blur(1.5f.dp.toPx())
                                    lens((radius*.28f).dp.toPx(),(radius*.42f).dp.toPx(),depthEffect=true,chromaticAberration=true)
                                },
                                highlight = {
                                    Highlight(
                                        width = .75.dp,
                                        alpha = if (dark) .8f else .65f,
                                        style =
                                            HighlightStyle.Default(
                                                color = Color.White.copy(alpha = .8f),
                                                falloff = 2f,
                                            ),
                                    )
                                },
                                shadow = {
                                    Shadow(
                                        radius = 16.dp,
                                        color = Color.Black.copy(alpha = if (dark) .20f else .09f),
                                    )
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha=if(touch).06f else if(dark).035f else .018f))
                                },
                            )
                    ) {
                        if (phase == HomePhase.CONVERTING)
                            ClockRing(
                                fill,
                                clockRotation,
                                check,
                                c.onBackground,
                                if (dark) Color(0xFF85D5A2) else Color(0xFF27824E),
                                Modifier.fillMaxSize().padding((radius * .5f).dp),
                            )
                    }
                }
                if (armed && !visualBusy)
                    Text(
                        "按住玻璃罩，向上滑动以转换",
                        Modifier.align(Alignment.BottomCenter)
                            .offset(y = (-(if(h<720)140*scale else 245*scale+26)).dp)
                            .alpha((1 - p / .3f).coerceIn(0f, 1f)),
                        fontSize = 12.sp,
                        color = c.onSurfaceVariant,
                    )
                if (phase == HomePhase.CONVERTING)
                    Column(
                        Modifier.align(Alignment.TopCenter).offset(y = (h * .59f).dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (check > 0) "已完成"
                            else if (snapshot.task.status == "complete") "正在完成" else "正在转换",
                            fontSize = 14.sp,
                        )
                        if (
                            snapshot.task.id == visualTask &&
                                snapshot.task.value >= 0 &&
                                check == 0f
                        )
                            Text(
                                "${snapshot.task.value}%",
                                fontSize = 12.sp,
                                color = c.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        if (check == 0f)
                            TextButton(
                                { visualTask?.let(actions.cancel) },
                                enabled = snapshot.task.status != "cancelling",
                            ) {
                                Text(
                                    if (snapshot.task.status == "cancelling") "正在取消" else "取消",
                                    fontSize = 12.sp,
                                )
                            }
                    }
                if (snapshot.busy && !visualBusy)
                    LinearProgressIndicator(
                        Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(28.dp)
                    )
                if (snapshot.busy && !visualBusy)
                    TextButton(
                        { actions.cancel(snapshot.task.id) },
                        enabled = snapshot.task.status != "cancelling",
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 40.dp),
                    ) {
                        Text(
                            if (snapshot.task.status == "cancelling") "正在取消" else "取消",
                            fontSize = 12.sp,
                        )
                    }
                Row(
                    Modifier.align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 10.dp, end = 16.dp)
                ) {
                    IconButton(
                        { sheet = "menu" },
                        enabled = !busy,
                        modifier = Modifier.controlGlass(controlsBackdrop, CircleShape),
                    ) {
                        Glyph("ellipsis", "更多操作", Modifier.size(23.dp), c.onSurfaceVariant)
                    }
                }
            }
        }
        run {
            NativeSheet(
                sheet,
                snapshot,
                if (tool?.id == "image" && target == "pdf") HOME_TOOLS.first { it.id == "imagepdf" }
                else tool,
                options,
                { options = it },
                mode,
                backgroundStyle,
                actions,
                { sheet = it },
                { sheet = null },
                { useFiles(it) },
                {
                    toolId = it.id
                    selected = files.filter { f -> it.id in f.tools }.map { f -> f.id }
                    target = null
                },
                tools,
                selectedFiles=files,
                reduced=reduced,
                backdrop=backdrop,
                onVisible={sheetVisible=it},
            )
        }
    }
}

@Composable
fun FileEmblem(file: HomeFile, size: Dp) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.size(size)
            .shadow(
                8.dp,
                RoundedCornerShape(21.dp),
                ambientColor = c.primary.copy(alpha = .08f),
                spotColor = c.primary.copy(alpha = .08f),
            )
            .background(c.surface, RoundedCornerShape(21.dp))
            .border(.7.dp, c.outline.copy(alpha = .55f), RoundedCornerShape(21.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Glyph(
            fileGlyph(file.name, file.kind),
            modifier = Modifier.size(size * .48f),
            color = c.primary,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            file.format
                .ifEmpty { normalizedSuffix(file.name) }
                .ifEmpty { "FILE" }
                .uppercase()
                .take(7),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.primary,
        )
    }
}

@Composable
private fun UploadLanding(modifier: Modifier, size: Dp, reduced: Boolean, onPick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "upload float")
    val drift by
        transition.animateFloat(
            0f,
            if (reduced) 0f else 4f,
            infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "paper drift",
        )
    Box(
        modifier.size(size).clip(RoundedCornerShape(28.dp)).clickable(onClick = onPick).semantics {
            contentDescription = "选择需要转换的文件"
        }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 1.dp.toPx()
            drawRoundRect(
                c.outline,
                Offset(inset, inset),
                Size(this.size.width - 2 * inset, this.size.height - 2 * inset),
                CornerRadius(28.dp.toPx()),
                style =
                    Stroke(
                        1.1.dp.toPx(),
                        pathEffect =
                            PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 7.dp.toPx())),
                    ),
            )
        }
        Box(Modifier.align(Alignment.Center).offset(y = (-20).dp).size(160.dp, 120.dp)) {
            Box(
                Modifier.offset(12.dp, (18 - drift).dp)
                    .size(48.dp, 62.dp)
                    .rotate(-12f)
                    .background(c.secondaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Glyph(
                    "file-image",
                    modifier = Modifier.size(27.dp),
                    color = c.onSurfaceVariant.copy(alpha = .7f),
                )
            }
            Box(
                Modifier.offset(100.dp, (26 + drift).dp)
                    .size(48.dp, 62.dp)
                    .rotate(12f)
                    .background(c.secondaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Glyph(
                    "file-text",
                    modifier = Modifier.size(27.dp),
                    color = c.onSurfaceVariant.copy(alpha = .7f),
                )
            }
            Box(
                Modifier.align(Alignment.Center)
                    .size(62.dp)
                    .shadow(
                        7.dp,
                        CircleShape,
                        ambientColor = c.primary.copy(alpha = .05f),
                        spotColor = c.primary.copy(alpha = .05f),
                    )
                    .background(c.surface, CircleShape)
                    .border(.7.dp, c.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Glyph("plus", modifier = Modifier.size(26.dp), color = c.primary)
            }
        }
        Text(
            "选择需要转换的文件",
            Modifier.align(Alignment.Center).offset(y = 69.dp),
            fontSize = 14.sp,
            color = c.onSurfaceVariant,
        )
    }
}

@Composable
private fun ClockRing(
    fill: Float,
    rotation: Float,
    check: Float,
    color: Color,
    green: Color,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val r = size.minDimension * .43f
        val center = Offset(size.width / 2, size.height / 2)
        val stroke = 2.2.dp.toPx()
        drawCircle(color.copy(alpha = .12f * (1 - check)), r, center, style = Stroke(stroke))
        drawArc(
            color.copy(alpha = 1 - check),
            -90f,
            360 * fill,
            false,
            topLeft = center - Offset(r, r),
            size = Size(2 * r, 2 * r),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        if (check < 1) {
            val angle = (rotation - 90) / 180 * PI
            drawLine(
                color.copy(alpha = .55f * (1 - check)),
                center,
                center + Offset(cos(angle).toFloat() * r * .62f, sin(angle).toFloat() * r * .62f),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color.copy(alpha = .55f * (1 - check)), 1.6.dp.toPx(), center)
        }
        if (check > 0) {
            val a = center + Offset(-r * .49f, 0f)
            val b = center + Offset(-r * .12f, r * .35f)
            val d = center + Offset(r * .57f, -r * .40f)
            val path =
                Path().apply {
                    moveTo(a.x, a.y)
                    val first = (check / .35f).coerceIn(0f, 1f)
                    lineTo(a.x + (b.x - a.x) * first, a.y + (b.y - a.y) * first)
                    if (check > .35f) {
                        val second = ((check - .35f) / .65f).coerceIn(0f, 1f)
                        lineTo(b.x + (d.x - b.x) * second, b.y + (d.y - b.y) * second)
                    }
                }
            drawPath(
                path,
                green,
                style = Stroke(stroke * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
