package cn.qingzhuan.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BackgroundCatalog {
    val ids=listOf("mist","dune","iris")
    val titles=listOf("雾青","暖砂","鸢尾")
    val palette=listOf("DFE9E5","DCE5F0","E7DFF0","F2E5DA","E8E7DF","D9E6E9")
    fun image(id:String)=when(id){"dune"->R.drawable.wallpaper_dune;"iris"->R.drawable.wallpaper_iris;else->R.drawable.wallpaper_mist}
    fun thumb(id:String)=when(id){"dune"->R.drawable.wallpaper_dune_thumb;"iris"->R.drawable.wallpaper_iris_thumb;else->R.drawable.wallpaper_mist_thumb}
    fun validHex(value:String):String? = value.trim().removePrefix("#").takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }?.uppercase(java.util.Locale.ROOT)
    fun color(hex:String)=Color((0xFF000000L or (validHex(hex)?:"DFE9E5").toLong(16)).toInt())
    fun solid(hex:String,dark:Boolean)=lerp(color(hex),if(dark)Color(0xFF111819)else Color.White,if(dark).79f else .45f)
    fun normalize(style:BackgroundStyle)=style.copy(
        mode=style.mode.takeIf {it in listOf("aurora","default","custom","solid","preset")}?:"aurora",
        blur=if(style.blur.isFinite())style.blur.coerceIn(0f,40f)else 18f,
        colorHex=validHex(style.colorHex)?:"DFE9E5",
        preset=style.preset.takeIf {it in ids}?:"mist",
        lightOverlay=if(style.lightOverlay.isFinite())style.lightOverlay.coerceIn(0f,1f)else .68f,
        darkOverlay=if(style.darkOverlay.isFinite())style.darkOverlay.coerceIn(0f,1f)else .72f,
    )
}

@Composable
fun BackgroundSettings(style:BackgroundStyle,actions:HomeActions,reduced:Boolean,onImport:()->Unit) {
    val c=MaterialTheme.colorScheme
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        GlassSegments(listOf(GlassChoice("aurora","光晕"),GlassChoice("default","默认"),GlassChoice("preset","壁纸")),style.mode,Modifier.fillMaxWidth(),reduced=reduced) { actions.setBackgroundStyle(style.copy(mode=it)) }
        GlassSegments(listOf(GlassChoice("solid","纯色"),GlassChoice("custom","照片")),style.mode,Modifier.fillMaxWidth(),reduced=reduced) {
            if(it=="custom"&&!actions.backgroundFile().isFile)onImport()else actions.setBackgroundStyle(style.copy(mode=it))
        }
        if(style.mode=="preset") {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                BackgroundCatalog.ids.forEachIndexed { index,id ->
                    val spoken=ui(BackgroundCatalog.titles[index])
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .border(if(style.preset==id)1.5.dp else .6.dp,c.outline,RoundedCornerShape(18.dp))
                        .clickable {actions.setBackgroundStyle(style.copy(preset=id))}
                        .semantics {role=Role.RadioButton; selected=style.preset==id;contentDescription=spoken},horizontalAlignment=Alignment.CenterHorizontally) {
                        Image(painterResource(BackgroundCatalog.thumb(id)),null,Modifier.fillMaxWidth().height(112.dp),contentScale=ContentScale.Crop)
                        UiText(BackgroundCatalog.titles[index],fontSize=12.sp,modifier=Modifier.padding(10.dp))
                    }
                }
            }
        }
        if(style.mode=="solid") {
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                BackgroundCatalog.palette.forEach { hex ->
                    val spoken=ui("颜色 #$hex")
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(BackgroundCatalog.color(hex))
                        .border(if(style.colorHex==hex)2.dp else .5.dp,c.outline,RoundedCornerShape(14.dp))
                        .clickable {actions.setBackgroundStyle(style.copy(colorHex=hex))}
                        .semantics {role=Role.RadioButton;selected=style.colorHex==hex;contentDescription=spoken})
                }
            }
            var hex by remember(style.colorHex){mutableStateOf(style.colorHex)}
            val valid=BackgroundCatalog.validHex(hex)
            OutlinedTextField(hex,{hex=it.take(7)},label={UiText("自定义颜色 · HEX")},prefix={UiText("#")},singleLine=true,
                isError=valid==null,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(capitalization=KeyboardCapitalization.Characters),modifier=Modifier.fillMaxWidth())
            Button({valid?.let {actions.setBackgroundStyle(style.copy(colorHex=it))}},enabled=valid!=null&&valid!=style.colorHex,modifier=Modifier.align(Alignment.End)){UiText("应用颜色")}
            UiText("颜色随深浅外观柔和适配，保持文字清晰。",fontSize=12.sp,color=c.onSurfaceVariant)
        }
        if(style.mode=="custom") {
            TextButton(onImport){Glyph("file-image",modifier=Modifier.size(18.dp));Spacer(Modifier.width(8.dp));UiText("更换背景图片")}
            val dark=MaterialTheme.colorScheme.background.luminance()<.5f
            var strength by remember(style.lightOverlay,style.darkOverlay,dark){mutableFloatStateOf(if(dark)style.darkOverlay else style.lightOverlay)}
            UiText((if(dark)"深色遮罩"else"浅色遮罩")+" · ${(strength*100).toInt()}%",style=MaterialTheme.typography.titleSmall)
            Slider(strength,{strength=it;actions.setBackgroundStyle(if(dark)style.copy(darkOverlay=it)else style.copy(lightOverlay=it))},valueRange=0f..1f)
            var blur by remember(style.blur){mutableFloatStateOf(style.blur)}
            UiText("模糊 · ${blur.toInt()}",fontSize=13.sp,color=c.onSurfaceVariant)
            Slider(blur,{blur=it},onValueChangeFinished={actions.setBackgroundStyle(style.copy(blur=blur))},valueRange=0f..40f)
        }
    }
}
