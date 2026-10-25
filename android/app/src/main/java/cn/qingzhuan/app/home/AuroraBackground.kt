package cn.qingzhuan.app

import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AuroraShader {
    const val SOURCE =
        """
uniform float2 resolution;
uniform float time;
uniform float night;
float cloud(float2 p,float2 c,float2 radii){float2 q=(p-c)/radii;return exp(-dot(q,q)*2.0);}
half4 main(float2 point){
 float2 p=point/resolution;
 float t=time*0.22;
 float a=cloud(p,float2(.24+.14*sin(t),.32+.15*cos(t*.73)),float2(.60,.32));
 float b=cloud(p,float2(.78+.12*cos(t*.81),.54+.16*sin(t*.67)),float2(.44,.30));
 float d=cloud(p,float2(.25+.20*cos(t*.58),.83+.10*sin(t*.91)),float2(.55,.30));
 float e=cloud(p,float2(.65+.12*sin(t*.61),.13+.10*cos(t*.84)),float2(.52,.26));
 float3 paper=mix(float3(.969,.973,.953),float3(.082,.102,.122),night);
 float3 blue=mix(float3(.70,.80,.96),float3(.12,.23,.40),night);
 float3 lilac=mix(float3(.85,.76,.94),float3(.27,.19,.37),night);
 float3 mint=mix(float3(.73,.87,.82),float3(.11,.28,.25),night);
 float3 peach=mix(float3(.96,.84,.75),float3(.32,.22,.19),night);
 // Smooth chromatic landmarks, not flat tint: real glass can sample these moving gradients.
 float3 col=(paper+blue*a*.70+lilac*b*.72+mint*d*.56+peach*e*.40)/(1.0+a*.70+b*.72+d*.56+e*.40);
 float ribbon=cloud(p,float2(.56+.08*sin(t*.5),.49+.10*cos(t*.63)),float2(.55,.055));
 col=mix(col,mix(float3(.98,.99,1.0),float3(.30,.39,.52),night),ribbon*.20);
 return half4(col,1.0);
}
"""
}

@Composable
fun AmbientBackground(
    style: BackgroundStyle,
    dark: Boolean,
    reduced: Boolean,
    actions: HomeActions,
) {
    val c = MaterialTheme.colorScheme
    val context=LocalContext.current
    val bitmap by
        produceState<android.graphics.Bitmap?>(null, style.mode, style.revision, style.preset, if (Build.VERSION.SDK_INT < 31) style.blur else 0f) {
            value =
                if (style.mode == "custom")
                    withContext(Dispatchers.IO) {
                        NativeMediaStore.decode(actions.backgroundFile(), 1600)?.let {
                            if (Build.VERSION.SDK_INT < 31) NativeMediaStore.legacyBlur(it, style.blur) else it
                        }
                    }
                else if(style.mode=="preset") withContext(Dispatchers.IO){android.graphics.BitmapFactory.decodeResource(context.resources,BackgroundCatalog.image(style.preset))}
                else null
        }
    if (style.mode in listOf("custom","preset") && bitmap != null) {
        Image(
            bitmap!!.asImageBitmap(),
            null,
            Modifier.fillMaxSize().blur(if(style.mode=="custom")style.blur.dp else 0.dp),
            contentScale = ContentScale.Crop,
        )
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                if (dark) Color(0xFF101720).copy(alpha = if(style.mode=="preset").78f else .72f)
                else Color(0xFFF7F8F3).copy(alpha = if(style.mode=="preset").10f else .68f)
            )
        }
    } else if(style.mode=="solid") {
        Canvas(Modifier.fillMaxSize()){drawRect(BackgroundCatalog.solid(style.colorHex,dark))}
    } else if (style.mode == "aurora" && Build.VERSION.SDK_INT >= 33) {
        val shader = remember { android.graphics.RuntimeShader(AuroraShader.SOURCE) }
        val transition = rememberInfiniteTransition(label = "ambient drift")
        val seconds by
            transition.animateFloat(
                0f,
                if (reduced) 0f else 240f,
                infiniteRepeatable(tween(240000, easing = LinearEasing), RepeatMode.Reverse),
                label = "ambient time",
            )
        Canvas(Modifier.fillMaxSize()) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", seconds)
            shader.setFloatUniform("night", if (dark) 1f else 0f)
            drawRect(ShaderBrush(shader))
        }
    } else Canvas(Modifier.fillMaxSize()) { drawRect(c.background) }
}
