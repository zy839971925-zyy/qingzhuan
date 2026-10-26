package cn.qingzhuan.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.backdrop.backdrops.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.*
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.*
import kotlin.math.*

data class GlassChoice(val value:String,val label:String,val detail:String="")

/** Background and text are captured separately. The moving lens never samples itself. */
@Composable
fun GlassSegments(choices:List<GlassChoice>,selectedValue:String?,modifier:Modifier=Modifier,enabled:Boolean=true,reduced:Boolean=false,onSelect:(String)->Unit) {
    if(choices.isEmpty())return
    val c=MaterialTheme.colorScheme
    val base=LocalControlBackdrop.current
    val labels=rememberLayerBackdrop()
    val source=if(base!=null) rememberCombinedBackdrop(base,labels) else labels
    val keys=choices.map { it.value }
    val selected=keys.indexOf(selectedValue)
    var pressEpoch by remember { mutableIntStateOf(0) }
    val pressSources=remember(keys,pressEpoch){choices.map{MutableInteractionSource()}}
    val pressFlags=pressSources.map{it.collectIsPressedAsState().value}
    val pressed=pressFlags.any{it}
    val scope=rememberCoroutineScope()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var position by remember(keys) { mutableFloatStateOf(selected.coerceAtLeast(0).toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var stretch by remember { mutableFloatStateOf(0f) }
    var lastDragTime by remember { mutableLongStateOf(0L) }
    var settling by remember { mutableStateOf<Job?>(null) }
    val latestSelected by rememberUpdatedState(selected)
    val callback by rememberUpdatedState(onSelect)
    val latestEnabled by rememberUpdatedState(enabled)
    val settle by rememberUpdatedState<(Int)->Unit>({ index ->
        settling?.cancel();val end=index.coerceIn(0,choices.lastIndex).toFloat()
        if(reduced)position=end else settling=scope.launch { animate(position,end,animationSpec=spring(dampingRatio=.68f,stiffness=460f)) { value,_->position=value.coerceIn(-.075f,choices.lastIndex+.075f) } }
    })
    LaunchedEffect(selected,keys,enabled,reduced) { if(!dragging&&selected>=0)settle(selected) }
    DisposableEffect(lifecycle,keys,enabled) {
        val observer=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_STOP){settling?.cancel();dragging=false;stretch=0f;pressEpoch++;position=latestSelected.coerceAtLeast(0).toFloat()} }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer);settling?.cancel();dragging=false;stretch=0f }
    }
    val pressure by animateFloatAsState(if((pressed||dragging)&&enabled)1f else 0f,if(reduced)snap()else spring(dampingRatio=.62f,stiffness=680f),label="glass pressure")
    val elastic by animateFloatAsState(if(dragging)stretch else 0f,if(reduced)snap()else spring(dampingRatio=.72f,stiffness=560f),label="glass stretch")
    val visibility by animateFloatAsState(if(selected>=0||dragging)1f else 0f,if(reduced)snap()else tween(140),label="glass thumb")
    BoxWithConstraints(modifier.height(62.dp).graphicsLayer{scaleX=1f+pressure*.012f;scaleY=1f+pressure*.025f}.selectableGroup().controlGlass(base,RoundedCornerShape(31.dp)).border(.6.dp,c.outline.copy(alpha=.38f),RoundedCornerShape(31.dp))) {
        val cell=(maxWidth-10.dp)/choices.size
        val density=LocalDensity.current
        val px=with(density){cell.toPx()};val inset=with(density){5.dp.toPx()}
        val rtl=LocalLayoutDirection.current==LayoutDirection.Rtl
        Box(Modifier.fillMaxSize().pointerInput(keys,enabled,px,rtl) {
            if(!enabled||px<=0)return@pointerInput
            detectHorizontalDragGestures(
                onDragStart={ point->settling?.cancel();dragging=true;stretch=0f;lastDragTime=0L;val p=((point.x-inset)/px-.5f).coerceIn(0f,choices.lastIndex.toFloat());position=if(rtl)choices.lastIndex-p else p },
                onHorizontalDrag={ change,delta->change.consume();val dt=if(lastDragTime==0L)16L else (change.uptimeMillis-lastDragTime).coerceAtLeast(1);lastDragTime=change.uptimeMillis;stretch=(abs(delta)/px/(dt/1000f)/18f).coerceIn(0f,.16f);position=(position+delta/px*(if(rtl)-1 else 1)).coerceIn(0f,choices.lastIndex.toFloat()) },
                onDragEnd={dragging=false;val index=position.roundToInt().coerceIn(0,choices.lastIndex);if(latestEnabled&&lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)){callback(keys[index]);settle(index)}else settle(latestSelected.coerceAtLeast(0))},
                onDragCancel={dragging=false;settle(latestSelected.coerceAtLeast(0))},
            )
        }) {
            Row(Modifier.fillMaxSize().padding(5.dp).layerBackdrop(labels)) {
                choices.forEachIndexed { index,choice->
                    val spoken=ui(choice.label)
                    val labelScale by animateFloatAsState(if(pressFlags[index]&&!reduced)1.065f else 1f,if(reduced)snap()else spring(dampingRatio=.7f,stiffness=600f),label="tab press")
                    Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer{scaleX=labelScale;scaleY=labelScale}.selectable(selected=index==selected,enabled=enabled,role=Role.Tab,interactionSource=pressSources[index],indication=null){callback(choice.value);settle(index)}.semantics{contentDescription=spoken},contentAlignment=Alignment.Center) {
                        GlassLetters(choice.label,index*45,reduced,if(abs(position-index)<.5f&&(selected>=0||dragging))c.onBackground else c.onSurfaceVariant)
                    }
                }
            }
            if(visibility>.001f) {
                val p=if(rtl)choices.lastIndex-position else position
                Box(Modifier.absoluteOffset{IntOffset((inset+p*px).roundToInt(),inset.roundToInt())}.size(cell,52.dp).graphicsLayer{alpha=visibility;scaleX=1f+pressure*.075f+elastic;scaleY=1f+pressure*.12f-elastic*.22f}.drawBackdrop(
                    backdrop=source,shape={RoundedCornerShape(26.dp)},
                    effects={vibrancy();lens(8.dp.toPx(),14.dp.toPx(),depthEffect=true,chromaticAberration=true)},
                    highlight={Highlight(width=.8.dp,alpha=.65f)},shadow={Shadow(radius=5.dp,color=Color.Black.copy(alpha=.09f))},
                    onDrawSurface={drawRect(Color.White.copy(alpha=.09f))},
                ).clearAndSetSemantics{})
            }
        }
    }
}

@Composable
fun GlassMenuButton(expanded:Boolean,enabled:Boolean,reduced:Boolean,onClick:()->Unit) {
    val c=MaterialTheme.colorScheme
    val spoken=ui(if(expanded)"收起转换格式"else"展开转换格式")
    val angle by animateFloatAsState(if(expanded)180f else 0f,if(reduced)snap()else tween(240),label="format disclosure")
    Row(Modifier.height(44.dp).controlGlass(LocalControlBackdrop.current,RoundedCornerShape(22.dp)).border(.6.dp,c.outline.copy(alpha=.38f),RoundedCornerShape(22.dp)).clickable(enabled=enabled,onClick=onClick).semantics{role=Role.Button;contentDescription=spoken}.padding(horizontal=15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        UiText("转换为",fontSize=12.sp)
        Glyph("chevron-down",modifier=Modifier.size(16.dp).graphicsLayer{rotationZ=angle},color=c.onSurfaceVariant)
    }
}

@Composable
fun GlassButton(choice:GlassChoice,selected:Boolean=false,enabled:Boolean=true,modifier:Modifier=Modifier,onClick:()->Unit) {
    val c=MaterialTheme.colorScheme
    val spoken=ui(choice.label)
    Column(modifier.height(52.dp).controlGlass(LocalControlBackdrop.current,RoundedCornerShape(20.dp))
        .background(c.primary.copy(alpha=if(selected).065f else 0f),RoundedCornerShape(20.dp))
        .border(if(selected)1.dp else .6.dp,c.outline.copy(alpha=if(selected).95f else .38f),RoundedCornerShape(20.dp))
        .clickable(enabled=enabled,onClick=onClick).semantics{this.selected=selected;role=Role.Button;contentDescription=spoken}.padding(horizontal=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            UiText(choice.label,fontSize=13.sp,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
            if(selected)Glyph("check",modifier=Modifier.size(12.dp),color=c.primary)
        }

    }
}
@Composable
private fun GlassLetters(rawText:String,delay:Int,reduced:Boolean,color:Color) {
    val text=ui(rawText)
    var shown by remember(text){mutableStateOf(false)}
    LaunchedEffect(text){shown=true}
    val reveal by animateFloatAsState(if(shown)1f else 0f,if(reduced)snap()else tween(460,delay,CubicBezierEasing(.23f,1f,.32f,1f)),label="format reveal")
    Row {text.forEachIndexed { index,ch->val a=(reveal*1.34f-index.toFloat()/text.length.coerceAtLeast(1)*.34f).coerceIn(0f,1f)
        Text(ch.toString(),fontSize=13.sp,fontWeight=FontWeight.Medium,color=color,modifier=Modifier.clearAndSetSemantics{}.graphicsLayer{alpha=a;translationY=(1-a)*7.dp.toPx();rotationX=(1-a)*-32;if(android.os.Build.VERSION.SDK_INT>=31){val b=(1-a)*6.dp.toPx();renderEffect=if(b>.1f)android.graphics.RenderEffect.createBlurEffect(b,b,android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()else null}})
    }}
}

data class ConversionChoice(val tool:String,val target:String,val detail:String) {
    val key get()="$tool:$target"
    val label get()=when(target){"h264"->"H.264";"hevc"->"HEVC";"av1"->"AV1";""->"全部解压";else->target.uppercase()}
}
data class FormatPresentation(val grid:Boolean,val choices:List<ConversionChoice>)
fun conversionChoices(tools:List<HomeTool>,files:List<HomeFile>):List<ConversionChoice> = tools.flatMap { tool->
    if(tool.id=="imagepdf"&&tools.any{it.id=="image"})emptyList() else shortcutFormats(tool,files).map { ConversionChoice(tool.id,it,if(tool.id=="image"&&it=="pdf")"图片成册" else tool.title) }
}.distinctBy{it.key}
