package cn.qingzhuan.app

import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.*
import com.kyant.backdrop.highlight.*
import com.kyant.backdrop.shadow.Shadow

/**
 * Controls read the background-only layer; the conversion orb reads background+content. Never
 * sample a backdrop layer while recording the same control into that layer.
 */
val LocalControlBackdrop = staticCompositionLocalOf<Backdrop?> { null }

fun Modifier.controlGlass(backdrop: Backdrop?, shape: Shape): Modifier =
    if (backdrop == null) this
    else
        drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(2f.dp.toPx())
                lens(6f.dp.toPx(), 10f.dp.toPx(), chromaticAberration = true)
            },
            highlight = { Highlight(width = .6.dp, alpha = .5f) },
            shadow = { Shadow(radius = 5.dp, color = Color.Black.copy(alpha = .055f)) },
            onDrawSurface = { drawRect(Color.White.copy(alpha = .07f)) },
        )

fun BackdropEffectScope.referenceLens(progress: Float, sloshX: Float, sloshY: Float) {
    if (Build.VERSION.SDK_INT < 33) return
    val radius = size.minDimension / 2f
    val shader = obtainRuntimeShader("AppllamaRadialLens", ReferenceOptics.LENS_SKSL)
    shader.setFloatUniform("c", size.width / 2 + padding, size.height / 2 + padding)
    shader.setFloatUniform("r", radius)
    shader.setFloatUniform("amount", .42f + .20f * progress.coerceIn(0f, 1f))
    shader.setFloatUniform("bezel", .25f + .17f * progress.coerceIn(0f, 1f))
    shader.setFloatUniform("disp", .05f + .07f * progress.coerceIn(0f, 1f))
    shader.setFloatUniform("slosh", sloshX * radius * .08f, sloshY * radius * .08f)
    effect(android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "image"))
}
