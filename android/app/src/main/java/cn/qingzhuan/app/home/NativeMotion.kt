package cn.qingzhuan.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner

/** UI-only motion. Business actions never wait for a reveal or a spring to finish. */
val LocalMotionReduced = staticCompositionLocalOf { false }

@Composable
fun rememberGlassInteraction(): MutableInteractionSource {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var epoch by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) epoch++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return remember(epoch) { MutableInteractionSource() }
}

/** Centered drawing deformation only: no offset, padding, height or parent transform. */
@Composable
fun Modifier.glassPressFeedback(source: InteractionSource, enabled: Boolean = true): Modifier {
    val reduced = LocalMotionReduced.current
    val pressed by source.collectIsPressedAsState()
    val pressure by animateFloatAsState(
        if (pressed && enabled && !reduced) 1f else 0f,
        if (reduced) snap() else spring(dampingRatio = .68f, stiffness = 740f),
        label = "glass control pressure",
    )
    return graphicsLayer {
        scaleX = 1f + pressure * .035f
        scaleY = 1f + pressure * .035f
        alpha = if (enabled) 1f else .42f
    }
}

/** Small finite entrance; same stable item does not replay on unrelated state changes. */
@Composable
fun MotionReveal(index: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reduced = LocalMotionReduced.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val reveal by animateFloatAsState(
        if (shown || reduced) 1f else 0f,
        if (reduced) snap() else tween(280, (index * 35).coerceIn(0, 180), CubicBezierEasing(.22f, 1f, .36f, 1f)),
        label = "ordered entrance",
    )
    Box(modifier.graphicsLayer {
        alpha = reveal
        translationY = (1f - reveal) * 10.dp.toPx()
        scaleX = .98f + reveal * .02f
        scaleY = scaleX
    }) { content() }
}

/** One editable section at a time; hidden content leaves composition after its exit. */
@Composable
fun MotionDisclosure(visible: Boolean, content: @Composable () -> Unit) {
    val reduced = LocalMotionReduced.current
    AnimatedVisibility(
        visible,
        enter = if (reduced) EnterTransition.None else fadeIn(tween(180)) + expandVertically(spring(dampingRatio = .9f, stiffness = 500f)),
        exit = if (reduced) ExitTransition.None else fadeOut(tween(100)) + shrinkVertically(tween(180)),
    ) {
        InteractionGate(visible,content)
    }
}

/** Block input before children, semantics and focus while retaining the exit drawing. */
@Composable
fun InteractionGate(active:Boolean,content:@Composable ()->Unit) {
    Box(Modifier.then(if(active)Modifier else Modifier
        .clearAndSetSemantics { }
        .focusProperties { canFocus=false;onEnter={cancelFocusChange()} }.focusGroup()
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while(true)awaitPointerEvent(PointerEventPass.Initial).changes.forEach{it.consume()}
            }
        })) { content() }
}
