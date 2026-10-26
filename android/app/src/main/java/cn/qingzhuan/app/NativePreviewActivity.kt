package cn.qingzhuan.app

import android.content.*
import android.graphics.ImageDecoder
import android.graphics.drawable.*
import android.os.*
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import kotlinx.coroutines.*
import org.json.JSONArray

class NativePreviewActivity:ComponentActivity() {
    override fun onCreate(state:Bundle?){
        super.onCreate(state);WindowCompat.setDecorFitsSystemWindows(window,false)
        val id=intent.getStringExtra("id")?:return finish();val output=intent.getBooleanExtra("output",true);val session=AppSession.get(this)
        val file:File;val name:String
        try{if(output){name=session.selectedOutputs(JSONArray().put(id).toString()).getJSONObject(0).getString("name");file=session.output(id)}else{file=session.previewInput(id);name=session.snapshot().getJSONArray("inputs").files().first{it.id==id}.name}}
        catch(e:Exception){android.widget.Toast.makeText(this,AppText.text(this,AppSession.message(e)),1).show();finish();return}
        setContent { UiLanguageProvider {
            val system=isSystemInDarkTheme();val mode=getSharedPreferences("appearance",0).getString("theme","system");val dark=mode=="dark"||mode=="system"&&system
            SideEffect{WindowCompat.getInsetsController(window,window.decorView).apply{isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark}}
            QingzhuanTheme(dark){val c=MaterialTheme.colorScheme
                Column(Modifier.fillMaxSize().background(c.background).safeDrawingPadding()){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        SmallAction("x","关闭预览"){finish()}
                        Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(name,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=15.sp);UiText("${normalizedSuffix(file.name).uppercase()} · ${formatSize(file.length())}",fontSize=11.sp,color=c.onSurfaceVariant)}
                        if(output)SmallAction("share-2","分享文件"){
                            try{val uri=android.net.Uri.parse("content://$packageName.files/"+android.net.Uri.encode(id,"/"));val share=Intent(Intent.ACTION_SEND).setType(OutputProvider.mime(file.name)).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.clipData=ClipData.newRawUri("轻转文件",uri);startActivity(Intent.createChooser(share,AppText.text(this@NativePreviewActivity,"分享文件")))}catch(e:Exception){android.widget.Toast.makeText(this@NativePreviewActivity,AppText.text(this@NativePreviewActivity,AppSession.message(e)),1).show()}
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()){PreviewContent(file)}
                }
            }
        }
        }
    }
}

@Composable private fun PreviewContent(file:File){
    val mime=OutputProvider.mime(file.name)
    when{
        file.extension.equals("pdf",true)->PdfPreview(file)
        mime.startsWith("image/")->ImagePreview(file)
        mime.startsWith("video/")||mime.startsWith("audio/")->MediaPreview(file)
        else->{
            val english=LocalUiEnglish.current
            var text by remember(file){mutableStateOf<String?>(null)};var error by remember(file){mutableStateOf<String?>(null)}
            LaunchedEffect(file,english){try{text=withContext(Dispatchers.IO){NativePreviewText.read(file,english)}}catch(e:CancellationException){throw e}catch(e:Exception){error=AppSession.message(e)}catch(_:OutOfMemoryError){error="内容过大，无法在应用内预览"}catch(_:StackOverflowError){error="文档结构过于复杂，无法预览"}}
            when{error!=null->Message(error!!);text==null->Loading();else->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp)){
                if(file.extension in listOf("docx","xlsx"))item{UiText("内容预览：不还原排版/图片，不重新计算公式；表格最多200行、16列",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                items(text!!.chunked(1800)){part->SelectionContainer{Text(part,fontSize=14.sp,lineHeight=23.sp)}}
            }}
        }
    }
}

@Composable private fun ImagePreview(file:File){
    if(Build.VERSION.SDK_INT<28 && file.extension.equals("gif",true)){LegacyGifPreview(file);return}
    var drawable by remember(file){mutableStateOf<Drawable?>(null)};var loading by remember(file){mutableStateOf(true)}
    LaunchedEffect(file){drawable=withContext(Dispatchers.IO){try{if(Build.VERSION.SDK_INT>=28)ImageDecoder.decodeDrawable(ImageDecoder.createSource(file)){decoder,info,_->val s=minOf(1f,2400f/maxOf(info.size.width,info.size.height));decoder.setTargetSize(maxOf(1,(info.size.width*s).toInt()),maxOf(1,(info.size.height*s).toInt()))}else NativeMediaStore.decode(file,2400)?.let{BitmapDrawable(android.content.res.Resources.getSystem(),it)}}catch(_:Exception){null}catch(_:OutOfMemoryError){null}};loading=false}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(drawable,lifecycle){
        fun playing(start:Boolean){if(Build.VERSION.SDK_INT>=28)(drawable as? AnimatedImageDrawable)?.let{if(start)it.start()else it.stop()}}
        if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))playing(true)
        val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_RESUME->playing(true);Lifecycle.Event.ON_STOP->playing(false);else->Unit}}
        lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer);playing(false)}
    }
    if(loading){Loading();return};if(drawable==null){Message("手机无法解码此图片，文件仍然保留。");return}
    var zoom by remember(file){mutableFloatStateOf(1f)};var pan by remember(file){mutableStateOf(Offset.Zero)}
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()){
        val w=constraints.maxWidth.toFloat();val h=constraints.maxHeight.toFloat()
        AndroidView(factory={ImageView(it).apply{scaleType=ImageView.ScaleType.FIT_CENTER}},update={it.setImageDrawable(drawable)},modifier=Modifier.fillMaxSize().pointerInput(file){detectTransformGestures{_,delta,scale,_->zoom=(zoom*scale).coerceIn(1f,5f);val next=pan+delta;pan=Offset(next.x.coerceIn(-w*(zoom-1)/2,w*(zoom-1)/2),next.y.coerceIn(-h*(zoom-1)/2,h*(zoom-1)/2))}}.graphicsLayer{scaleX=zoom;scaleY=zoom;translationX=pan.x;translationY=pan.y})
        TextButton({zoom=1f;pan=Offset.Zero},Modifier.align(Alignment.BottomCenter).padding(10.dp)){UiText("适合屏幕")}
    }
}

@Composable private fun PdfPreview(file:File){
    var pages by remember(file){mutableIntStateOf(-1)};var error by remember(file){mutableStateOf<String?>(null)}
    LaunchedEffect(file){try{pages=withContext(Dispatchers.IO){NativePreviewAssets.pageCount(file)}}catch(e:CancellationException){throw e}catch(e:Exception){error="无法读取PDF：${AppSession.message(e)}"}}
    when{error!=null->Message(error!!);pages<0->Loading();else->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        items(pages){index->var bitmap by remember(file,index){mutableStateOf<android.graphics.Bitmap?>(null)};var failed by remember(file,index){mutableStateOf(false)}
            LaunchedEffect(file,index){try{bitmap=withContext(Dispatchers.IO){NativePreviewAssets.page(file,index,1400)}}catch(e:CancellationException){throw e}catch(_:Exception){failed=true}catch(_:OutOfMemoryError){failed=true}}
            Column(horizontalAlignment=Alignment.CenterHorizontally){if(bitmap!=null)Image(bitmap!!.asImageBitmap(),ui("第${index+1}页"),Modifier.fillMaxWidth().aspectRatio(bitmap!!.width.toFloat()/bitmap!!.height),contentScale=ContentScale.FillWidth)else Box(Modifier.fillMaxWidth().height(200.dp),contentAlignment=Alignment.Center){if(failed)UiText("此页无法渲染")else CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp)};UiText("${index+1} / $pages",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }}
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable private fun MediaPreview(file:File){
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle
    var error by remember(file){mutableStateOf<String?>(null)}
    val player=remember(file){ExoPlayer.Builder(context).build().apply{setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT,true);addListener(object:androidx.media3.common.Player.Listener{override fun onPlayerError(e:androidx.media3.common.PlaybackException){error="手机无法播放此文件：${e.errorCodeName}"}});setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)));playWhenReady=false;prepare()}}
    Box(Modifier.fillMaxSize()){AndroidView(factory={PlayerView(AppText.localizedContext(it)).apply{this.player=player;controllerAutoShow=true}},modifier=Modifier.fillMaxSize());if(error!=null)Message(error!!)}
    DisposableEffect(player,lifecycle){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)player.pause()};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer);player.release()}}
}
@Composable private fun Loading(){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(Modifier.size(28.dp),strokeWidth=2.dp)}}
@Composable private fun Message(text:String){Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){UiText(text,color=MaterialTheme.colorScheme.onSurfaceVariant,lineHeight=24.sp)}}

@Suppress("DEPRECATION")
private class LegacyGifView(context:Context):android.view.View(context) {
    var movie:android.graphics.Movie?=null
    var plays:Int=1
    private var playing=false
    private var elapsed=0L
    private var stamp=0L
    fun playback(active:Boolean){playing=active;stamp=android.os.SystemClock.uptimeMillis();invalidate()}
    override fun onDraw(canvas:android.graphics.Canvas){
        super.onDraw(canvas);val frame=movie?:return
        val now=android.os.SystemClock.uptimeMillis();if(playing)elapsed+=(now-stamp).coerceIn(0,100);stamp=now
        val duration=frame.duration().takeIf{it>0}?:1000
        if(plays>0 && elapsed>=duration.toLong()*plays){playing=false;frame.setTime(duration-1)}else frame.setTime((elapsed%duration).toInt())
        val scale=minOf(width.toFloat()/frame.width().coerceAtLeast(1),height.toFloat()/frame.height().coerceAtLeast(1))
        canvas.save();canvas.translate((width-frame.width()*scale)/2,(height-frame.height()*scale)/2);canvas.scale(scale,scale);frame.draw(canvas,0f,0f);canvas.restore()
        if(playing)postInvalidateOnAnimation()
    }
}
@Suppress("DEPRECATION")
@Composable private fun LegacyGifPreview(file:File){
    var movie by remember(file){mutableStateOf<android.graphics.Movie?>(null)}
    var loading by remember(file){mutableStateOf(true)}
    var plays by remember(file){mutableIntStateOf(1)}
    var view by remember(file){mutableStateOf<LegacyGifView?>(null)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(file){
        val result=withContext(Dispatchers.IO){
            try{
                val bytes=file.inputStream().use{input->val b=ByteArray(4096);val n=input.read(b);if(n>0)b.copyOf(n)else byteArrayOf()}
                val i=String(bytes,Charsets.ISO_8859_1).indexOf("NETSCAPE2.0")
                val count=if(i>=0&&i+14<bytes.size)(bytes[i+13].toInt()and 255)or((bytes[i+14].toInt()and 255)shl 8)else -1
                android.graphics.Movie.decodeFile(file.path) to (if(count==0)-1 else if(count<0)1 else count+1)
            }catch(_:Exception){null to 1}catch(_:OutOfMemoryError){null to 1}
        }
        movie=result.first;plays=result.second;loading=false
    }
    when{loading->Loading();movie==null->Message("手机无法解码这个GIF，文件仍然保留");else->AndroidView(factory={LegacyGifView(it).also{v->view=v}},update={it.movie=movie;it.plays=plays;it.playback(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))},modifier=Modifier.fillMaxSize())}
    DisposableEffect(lifecycle,movie){val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_RESUME->view?.playback(true);Lifecycle.Event.ON_STOP->view?.playback(false);else->Unit}};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer);view?.playback(false);view?.movie=null;view=null}}
}
