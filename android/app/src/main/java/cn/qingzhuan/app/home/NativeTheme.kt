package cn.qingzhuan.app

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val Light =
    lightColorScheme(
        primary = Color(0xFF476854),
        onPrimary = Color(0xFFFDFEF9),
        background = Color(0xFFF7F8F3),
        onBackground = Color(0xFF29352F),
        surface = Color(0xFFFDFEF9),
        onSurface = Color(0xFF29352F),
        surfaceVariant = Color(0xFFEDF1E8),
        onSurfaceVariant = Color(0xFF55665C),
        outline = Color(0xFFCDD8CE),
        secondaryContainer = Color(0xFFE2EBDD),
        onSecondaryContainer = Color(0xFF476854),
    )
private val Dark =
    darkColorScheme(
        primary = Color(0xFFAFCCB9),
        onPrimary = Color(0xFF1D3023),
        background = Color(0xFF171B19),
        onBackground = Color(0xFFE8EDE7),
        surface = Color(0xFF222C26),
        onSurface = Color(0xFFE8EDE7),
        surfaceVariant = Color(0xFF2B3530),
        onSurfaceVariant = Color(0xFFA3AFA6),
        outline = Color(0xFF3C4940),
        secondaryContainer = Color(0xFF2B3B30),
        onSecondaryContainer = Color(0xFFAFCCB9),
    )

@Composable
fun QingzhuanTheme(
    dark: Boolean,
    photoBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val base = if (dark) Dark else Light
    MaterialTheme(
        colorScheme = if (photoBackground) base.copy(onSurfaceVariant = base.onBackground) else base
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
