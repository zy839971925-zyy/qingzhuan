package cn.qingzhuan.app

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import kotlin.math.*

/** Only consumes unhandled horizontal edge travel. Pager keeps taps, slop and normal scrolling. */
@Stable
class ElasticPreviewMotion(private val scope:CoroutineScope,private val limit:Float) : NestedScrollConnection {
    var offset by mutableFloatStateOf(0f)
        private set
    private var raw=0f
    private var rebound:Job?=null
    private fun update(value:Float) {raw=value.coerceIn(-limit*5,limit*5);offset=limit*tanh(raw/limit)}
    override fun onPreScroll(available:Offset,source:NestedScrollSource):Offset {
        if(source!=NestedScrollSource.UserInput||available.x==0f)return Offset.Zero
        rebound?.cancel()
        if(raw!=0f&&sign(available.x)!=sign(raw)) {
            val consumed=sign(available.x)*min(abs(available.x),abs(raw))
            update(raw+consumed)
            return Offset(consumed,0f)
        }
        return Offset.Zero
    }
    override fun onPostScroll(consumed:Offset,available:Offset,source:NestedScrollSource):Offset {
        if(source!=NestedScrollSource.UserInput||available.x==0f)return Offset.Zero
        rebound?.cancel();update(raw+available.x)
        return Offset(available.x,0f)
    }
    override suspend fun onPreFling(available:Velocity):Velocity {release(available.x);return Velocity.Zero}
    override suspend fun onPostFling(consumed:Velocity,available:Velocity):Velocity {if(rebound?.isActive!=true)release(available.x);return Velocity.Zero}
    private fun release(velocity:Float) {
        rebound?.cancel()
        if(offset==0f)return
        rebound=scope.launch {
            animate(offset,0f,initialVelocity=velocity.coerceIn(-1200f,1200f)*.12f,
                animationSpec=spring(dampingRatio=.62f,stiffness=300f)) {value,_->
                offset=value.coerceIn(-limit,limit)
                raw=limit*atanh((offset/limit).coerceIn(-.999f,.999f))
            }
            raw=0f;offset=0f
        }
    }
    fun reset(){rebound?.cancel();raw=0f;offset=0f}
}

@Composable
fun rememberElasticPreviewMotion(limit:Float,key:Any):ElasticPreviewMotion {
    val scope=rememberCoroutineScope()
    val motion=remember(limit,key){ElasticPreviewMotion(scope,limit.coerceAtLeast(1f))}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,motion) {
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)motion.reset()}
        lifecycle.addObserver(observer)
        onDispose{lifecycle.removeObserver(observer);motion.reset()}
    }
    return motion
}
