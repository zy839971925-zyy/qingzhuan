package cn.qingzhuan.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun MediaOptionsPanel(tool: String, durationMs: Long, options: Map<String,String>, onChange: (Map<String,String>)->Unit,reduced:Boolean=false) {
    val c=MaterialTheme.colorScheme
    fun set(key:String,value:String)=onChange(options+(key to value))
    val gif=tool=="gif"
    val duration=durationMs/1000f
    UiText(if(gif) "GIF不含声音；尺寸、帧率和片段长度共同决定体积。" else "降低分辨率、码率或帧率可以减小文件；转码可能有画质损失。",fontSize=12.sp,color=c.onSurfaceVariant)
    Spacer(Modifier.height(14.dp))
    if(duration>0f && tool!="videoframe") {
        val start=(options["clipStart"]?.toFloatOrNull()?:0f).coerceIn(0f,duration)
        val end=(options["clipEnd"]?.toFloatOrNull()?.takeIf { it>0 }?:if(gif) minOf(duration,start+10f) else duration).coerceIn(start,duration)
        UiText(String.format(Locale.ROOT,"截取 %.2f – %.2f 秒",start,end),fontSize=13.sp)
        RangeSlider(start..end,{ range -> onChange(options+mapOf("clipStart" to String.format(Locale.ROOT,"%.3f",range.start),"clipEnd" to String.format(Locale.ROOT,"%.3f",range.endInclusive))) },valueRange=0f..duration)
    }
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(options["clipStart"]?:"0",{set("clipStart",it)},label={UiText(if(tool=="videoframe") "时间点（秒）" else "起点（秒）")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)
        if(tool!="videoframe") OutlinedTextField(options["clipEnd"]?:"",{set("clipEnd",it)},label={UiText("终点（秒，留空自动）")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)
    }
    Spacer(Modifier.height(16.dp))
    val edgeKey=if(gif) "gifEdge" else "videoEdge"
    UiText("输出最长边（保持比例）",fontSize=13.sp)
    MediaChoiceRow(if(gif) listOf("240","360","480","720","960") else listOf("480","720","1080","1440","2160"),options[edgeKey]?:if(gif) "480" else "1080") {set(edgeKey,it)}
    if(tool!="videoframe") {
        Spacer(Modifier.height(12.dp));UiText("帧率（fps）",fontSize=13.sp)
        val fpsKey=if(gif) "gifFps" else "videoFps"
        MediaChoiceRow(if(gif) listOf("8","12","15","24","30") else listOf("24","30","60"),options[fpsKey]?:if(gif) "12" else "30") {set(fpsKey,it)}
    }
    if(gif) {
        Spacer(Modifier.height(12.dp));UiText("质量",fontSize=13.sp)
        GlassSegments(listOf(GlassChoice("30","轻量"),GlassChoice("60","均衡"),GlassChoice("85","精细")),options["gifQuality"]?:"85",Modifier.fillMaxWidth(),reduced=reduced){set("gifQuality",it)}
        Spacer(Modifier.height(12.dp))
        UiText("播放方式",fontSize=13.sp)
        GlassSegments(listOf(GlassChoice("true","循环"),GlassChoice("false","一次")),options["gifLoop"]?:"true",Modifier.fillMaxWidth(),reduced=reduced){set("gifLoop",it)}
        UiText("GIF最长60秒，有处理量限制。Live Photo需导入MOV视频，或同名照片和MOV配对；仅有静态照片无法恢复动态。",fontSize=12.sp,color=c.onSurfaceVariant)
    } else if(tool!="videoframe") {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(options["videoBitrate"]?:"4000",{set("videoBitrate",it)},label={UiText("目标视频码率（kbps，100–80000）")},modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
        Spacer(Modifier.height(12.dp))
        GlassSegments(listOf(GlassChoice("false","保留声音"),GlassChoice("true","静音")),options["mute"]?:"false",Modifier.fillMaxWidth(),reduced=reduced){set("mute",it)}
        UiText("编码器和分辨率支持取决于手机；不会把不支持的HEVC/AV1悄悄替换为H.264。",fontSize=12.sp,color=c.onSurfaceVariant)
    }
}

@Composable
private fun MediaChoiceRow(values:List<String>,current:String,onSelect:(String)->Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        items(values.size) { index -> val value=values[index];GlassButton(GlassChoice(value,value),current==value,modifier=Modifier.width(74.dp)){onSelect(value)} }
    }
}
