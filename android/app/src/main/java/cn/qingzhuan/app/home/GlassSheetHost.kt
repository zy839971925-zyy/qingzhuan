package cn.qingzhuan.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.*
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/** Same Activity/window as the scene: no second wallpaper crop, opaque handle, or dialog bars. */
@Composable
fun GlassSheetHost(
    name: String?,
    backdrop: Backdrop,
    reduced: Boolean,
    onClose: () -> Unit,
    onVisible: (Boolean) -> Unit,
    content: @Composable (String, () -> Unit) -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = name != null
    var retained by remember { mutableStateOf(name ?: "menu") }
    if (name != null) retained = name
    val active = state.currentState || state.targetState || !state.isIdle
    SideEffect { onVisible(active) }
    BackHandler(active) { if (name != null) onClose() }
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(topStart=32.dp, topEnd=32.dp)
    val dark = c.background.luminance() < .5f
    AnimatedVisibility(
        visibleState=state,
        modifier=Modifier.fillMaxSize(),
        enter=fadeIn(if(reduced)snap() else tween(220)),
        exit=fadeOut(if(reduced)snap() else tween(220)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().imePadding().semantics { isTraversalGroup=true; paneTitle="轻转操作面板" }) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(dark).30f else .16f))
                .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClose)
                .semantics { contentDescription="关闭操作面板" })
            val sheetBackdrop=rememberLayerBackdrop()
            val focus=remember{FocusRequester()}
            val scope=rememberCoroutineScope()
            val density=LocalDensity.current
            var dragOffset by remember {mutableFloatStateOf(0f)}
            var release by remember {mutableStateOf<Job?>(null)}
            val threshold=with(density){64.dp.toPx()}
            fun restore(){release?.cancel();release=scope.launch{if(reduced)dragOffset=0f else animate(dragOffset,0f,animationSpec=spring(dampingRatio=.76f,stiffness=420f)){v,_->dragOffset=v.coerceAtLeast(0f)}}}
            LaunchedEffect(name){if(name!=null){release?.cancel();dragOffset=0f}}
            LaunchedEffect(Unit) { focus.requestFocus() }
            Box(Modifier.align(Alignment.BottomCenter).widthIn(max=640.dp).fillMaxWidth()
                .heightIn(max=(maxHeight-WindowInsets.statusBars.asPaddingValues().calculateTopPadding()-12.dp).coerceAtLeast(120.dp))
                .animateEnterExit(
                    enter=slideInVertically(if(reduced)snap() else spring(dampingRatio=.88f,stiffness=360f)){it},
                    exit=slideOutVertically(if(reduced)snap() else tween(220, easing=FastOutSlowInEasing)){it},
                )
                .graphicsLayer{translationY=dragOffset}
                .focusRequester(focus).focusProperties { onExit={cancelFocusChange()} }.focusGroup()
                .clip(shape)
                // Consume taps on empty panel space; only the scrim outside dismisses.
                .pointerInput(Unit){detectTapGestures{}},
            ) {
                Box(Modifier.matchParentSize().layerBackdrop(sheetBackdrop).drawBackdrop(
                    backdrop=backdrop,shape={shape},
                    effects={vibrancy();blur(24.dp.toPx());lens(5.dp.toPx(),10.dp.toPx(),chromaticAberration=true)},
                    highlight={Highlight(width=.7.dp,alpha=.45f)},
                    shadow={Shadow(radius=20.dp,color=Color.Black.copy(alpha=.12f))},
                    onDrawSurface={drawRect(c.surface.copy(alpha=if(dark).72f else .68f))},
                ))
                CompositionLocalProvider(LocalControlBackdrop provides sheetBackdrop) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                        Box(Modifier.fillMaxWidth().height(27.dp).pointerInput(name,reduced,threshold){
                            detectVerticalDragGestures(
                                onDragStart={release?.cancel()},
                                onVerticalDrag={change,dy->change.consume();dragOffset=(dragOffset+dy).coerceIn(0f,threshold*4)},
                                onDragCancel={restore()},
                                onDragEnd={if(dragOffset>threshold)onClose()else restore()},
                            )
                        }.semantics{contentDescription="向下拖动关闭面板"},contentAlignment=Alignment.Center) {
                            Box(Modifier.size(30.dp,4.dp).background(c.onSurfaceVariant.copy(alpha=.4f),RoundedCornerShape(2.dp)))
                        }
                        AnimatedContent(
                            targetState=retained,
                            transitionSpec={
                                if(reduced) (fadeIn(snap()) togetherWith fadeOut(snap())).using(SizeTransform(clip=false){_,_->snap()})
                                else ((fadeIn(tween(220,70))+slideInVertically(tween(280)){it.coerceAtMost(36)}) togetherWith
                                    (fadeOut(tween(110))+slideOutVertically(tween(160)){-it.coerceAtMost(20)}))
                                    .using(SizeTransform(clip=false){_,_->spring(dampingRatio=.92f,stiffness=420f)})
                            },label="sheet navigation",
                        ) { shown -> content(shown,onClose) }
                    }
                }
            }
        }
    }
}
