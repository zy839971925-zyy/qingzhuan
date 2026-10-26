package cn.qingzhuan.app

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ImageOptionsPanel(options:Map<String,String>,onChange:(Map<String,String>)->Unit,target:String?,reduced:Boolean) {
    val c=MaterialTheme.colorScheme
    val advanced=options["imageAdvanced"]=="true"
    fun set(key:String,value:String)=onChange(options+(key to value))
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                UiText("高级设置",style=MaterialTheme.typography.titleMedium)
                UiText(if(advanced)"尺寸与质量"else"原始尺寸 · 最高质量",color=c.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
            }
            Switch(advanced,{set("imageAdvanced",it.toString())})
        }
        AnimatedVisibility(advanced,enter=if(reduced)EnterTransition.None else fadeIn()+expandVertically(),exit=if(reduced)ExitTransition.None else fadeOut()+shrinkVertically()) {
            Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                UiText("分辨率",style=MaterialTheme.typography.titleSmall)
                val choices=listOf("original" to "原图","4k" to "4K","1080p" to "1080P","720p" to "720P","custom" to "自定义")
                choices.chunked(3).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    row.forEach{(value,title)->GlassButton(GlassChoice(value,title),(options["imageSize"]?:"original")==value,modifier=Modifier.weight(1f)){set("imageSize",value)}}
                    repeat(3-row.size){Spacer(Modifier.weight(1f))}
                }}
                if(options["imageSize"]=="custom") {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(options["imageWidth"]?:"1920",{set("imageWidth",it.filter(Char::isDigit).take(5))},label={UiText("宽（px）")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f))
                        OutlinedTextField(options["imageHeight"]?:"1080",{set("imageHeight",it.filter(Char::isDigit).take(5))},label={UiText("高（px）")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f))
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        UiText("保持比例",Modifier.weight(1f))
                        Switch(options["imageAspect"]!="false",{set("imageAspect",it.toString())})
                    }
                    UiText(if(options["imageAspect"]=="false")"按指定宽高调整，可能拉伸图片。"else"保持比例，适配指定范围，不放大图片。",style=MaterialTheme.typography.bodySmall,color=c.onSurfaceVariant)
                } else if(options["imageSize"]!="original") UiText("保持比例，不放大图片。",style=MaterialTheme.typography.bodySmall,color=c.onSurfaceVariant)
                if(target in listOf("jpg","webp")) {
                    var quality by remember(options["imageQuality"]){mutableFloatStateOf(options["imageQuality"]?.toFloatOrNull()?:100f)}
                    UiText("画质 · ${quality.toInt()}",style=MaterialTheme.typography.titleSmall)
                    Slider(quality,{quality=it;set("imageQuality",it.toInt().toString())},valueRange=20f..100f,steps=79)
                } else if(target=="png") UiText("PNG 为无损编码。",style=MaterialTheme.typography.bodySmall,color=c.onSurfaceVariant)
            }
        }
    }
}
