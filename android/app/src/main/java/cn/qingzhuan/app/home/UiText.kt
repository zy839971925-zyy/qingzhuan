package cn.qingzhuan.app

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*

val LocalUiEnglish=staticCompositionLocalOf{false}
@Composable
fun UiLanguageProvider(content:@Composable ()->Unit){
    val context=LocalContext.current
    // Read config so the public per-app locale API/system changes invalidate the provider.
    val configuration=LocalConfiguration.current
    AppText.load(context)
    val english=AppText.english(context)
    CompositionLocalProvider(LocalUiEnglish provides english,content=content)
}
@Composable fun ui(value:String)=AppText.translate(value,LocalUiEnglish.current)

/** Explicit localizable text boundary; use Material Text for actual user filenames/content. */
@Composable
fun UiText(
    text:String,modifier:Modifier=Modifier,color:Color=Color.Unspecified,fontSize:TextUnit=TextUnit.Unspecified,
    fontStyle:FontStyle?=null,fontWeight:FontWeight?=null,fontFamily:FontFamily?=null,
    letterSpacing:TextUnit=TextUnit.Unspecified,textDecoration:TextDecoration?=null,textAlign:TextAlign?=null,
    lineHeight:TextUnit=TextUnit.Unspecified,overflow:TextOverflow=TextOverflow.Clip,softWrap:Boolean=true,
    maxLines:Int=Int.MAX_VALUE,minLines:Int=1,style:TextStyle=LocalTextStyle.current,
){Text(text=ui(text),modifier=modifier,color=color,fontSize=fontSize,fontStyle=fontStyle,fontWeight=fontWeight,fontFamily=fontFamily,letterSpacing=letterSpacing,textDecoration=textDecoration,textAlign=textAlign,lineHeight=lineHeight,overflow=overflow,softWrap=softWrap,maxLines=maxLines,minLines=minLines,style=style)}
