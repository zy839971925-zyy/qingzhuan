package cn.qingzhuan.app

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

@Composable
fun Glyph(
    name: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
) {
    val id =
        when (name) {
            "eye" -> R.drawable.qz_eye
            "plus" -> R.drawable.qz_plus
            "file-image" -> R.drawable.qz_file_image
            "file-text" -> R.drawable.qz_file_text
            "file-audio" -> R.drawable.qz_file_audio
            "file-archive" -> R.drawable.qz_file_archive
            "file" -> R.drawable.qz_file
            "check" -> R.drawable.qz_check
            "ellipsis" -> R.drawable.qz_ellipsis
            "settings-2" -> R.drawable.qz_settings_2
            "clock-3" -> R.drawable.qz_clock_3
            "download" -> R.drawable.qz_download
            "share-2" -> R.drawable.qz_share_2
            "arrow-up" -> R.drawable.qz_arrow_up
            "x" -> R.drawable.qz_x
            "chevron-down" -> R.drawable.qz_chevron_down
            "trash" -> R.drawable.qz_trash
            "repeat" -> R.drawable.qz_repeat
            "arrow-down" -> R.drawable.qz_arrow_down
            "folder" -> R.drawable.qz_folder
            "square" -> R.drawable.qz_square
            "check-square" -> R.drawable.qz_check_square
            else -> R.drawable.qz_file
        }
    Icon(painterResource(id), description, modifier, tint = color)
}
