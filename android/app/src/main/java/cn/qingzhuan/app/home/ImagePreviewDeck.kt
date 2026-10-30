package cn.qingzhuan.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PreviewCard(
    file: HomeFile,
    actions: HomeActions,
    size: Dp,
    output: Boolean = false,
    onClick: () -> Unit = {},
) {
    val loader by rememberUpdatedState(actions.preview)
    val bitmap by
        produceState<android.graphics.Bitmap?>(null, file.id, output) {
            value =
                withContext(Dispatchers.IO) {
                    loader(file.id,output)
                }
        }
    val c = MaterialTheme.colorScheme
    Box(
        Modifier.size(size)
            .shadow(
                9.dp,
                RoundedCornerShape(18.dp),
                ambientColor = c.primary.copy(alpha = .12f),
                spotColor = c.primary.copy(alpha = .14f),
            )
            .clip(RoundedCornerShape(18.dp))
            .border(.8.dp, c.outline.copy(alpha = .65f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val imageAlpha by animateFloatAsState(if(bitmap!=null)1f else 0f,if(LocalMotionReduced.current)snap()else tween(200),label="thumbnail ready")
        if(bitmap==null || imageAlpha<1f)FileEmblem(file,size)
        if (bitmap != null)
            Image(
                bitmap!!.asImageBitmap(),
                ui("图片预览：${file.name}"),
                Modifier.fillMaxSize().graphicsLayer{alpha=imageAlpha},
                contentScale = ContentScale.Crop,
            )
    }
}

/** Single item is static. Multiple items fan slightly and change front card with a real pager. */
@Composable
fun FilePreviewDeck(
    files: List<HomeFile>,
    actions: HomeActions,
    cardSize: Dp,
    reduced: Boolean = false,
    output: Boolean = false,
    onManage: () -> Unit,
) {
    if (files.isEmpty()) return
    val c = MaterialTheme.colorScheme
    val pager = rememberPagerState(pageCount = { files.size })
    val focused = files[pager.currentPage.coerceIn(files.indices)]
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth()) {
        if (files.size == 1)
            Box(Modifier.fillMaxWidth().height(cardSize + 20.dp), contentAlignment = Alignment.Center) {
                PreviewCard(files[0], actions, cardSize, output=output, onClick = { if(output)actions.open(files[0].id)else actions.openInput(files[0].id) })
            }
        else
            BoxWithConstraints(Modifier.fillMaxWidth().height(cardSize + 24.dp)) {
                val padding = ((maxWidth - cardSize) / 2).coerceAtLeast(0.dp)
                val density = LocalDensity.current
                val elastic=rememberElasticPreviewMotion(with(density){32.dp.toPx()},files.map{it.id})
                val pagePosition=pager.currentPage+pager.currentPageOffsetFraction
                val trailing by animateFloatAsState(pagePosition,if(reduced)snap()else spring(dampingRatio=.54f,stiffness=260f),label="stack inertia")
                val lag=(trailing-pagePosition).coerceIn(-.8f,.8f)
                val settleScale by animateFloatAsState(if(pager.isScrollInProgress&&!reduced).975f else 1f,if(reduced)snap()else spring(dampingRatio=.62f,stiffness=400f),label="preview settle")
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize().then(if(reduced)Modifier else Modifier.nestedScroll(elastic)).graphicsLayer{translationX=if(reduced)0f else elastic.offset;scaleX=settleScale;scaleY=settleScale},
                    flingBehavior=PagerDefaults.flingBehavior(pager,pagerSnapDistance=PagerSnapDistance.atMost(1),snapAnimationSpec=if(reduced)snap()else spring(dampingRatio=.68f,stiffness=320f)),
                    overscrollEffect=null,
                    contentPadding = PaddingValues(horizontal = padding),
                    pageSize = PageSize.Fixed(cardSize),
                    pageSpacing = 14.dp,
                    beyondViewportPageCount = 2,
                    verticalAlignment = Alignment.CenterVertically,
                    key = { files[it].id },
                ) { page ->
                    val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                    Box(
                        Modifier.zIndex(10f - abs(offset)).graphicsLayer {
                            val depth=abs(offset).coerceAtMost(2f)
                            val fan=if(reduced)0f else lag*(.5f+depth*.45f)
                            translationX = offset * with(density) { (cardSize * .66f).toPx() }+fan*with(density){(cardSize*.16f).toPx()}
                            rotationZ = -offset.coerceIn(-2f, 2f) * 5f+fan*9f
                            rotationY=if(reduced)0f else fan*6f
                            cameraDistance=18f*size.width
                            val s = 1f - .075f * abs(offset).coerceAtMost(2f)
                            scaleX = s
                            scaleY = s
                            alpha = (1f - .15f * abs(offset)).coerceIn(.25f, 1f)
                        }
                    ) {
                        PreviewCard(files[page], actions, cardSize, output=output, onClick = { if(output)actions.open(files[page].id)else actions.openInput(files[page].id) })
                    }
                }
            }
        if(files.size>1)Box(Modifier.align(Alignment.CenterEnd).padding(end=16.dp)) {
            SmallAction("folder",if(output)"查看全部结果"else"管理文件",onClick=onManage)
        }
        }
        Spacer(Modifier.height(12.dp))
        AnimatedContent(focused.id,transitionSpec={fadeIn(if(reduced)snap()else tween(160)) togetherWith fadeOut(if(reduced)snap()else tween(80))},label="preview filename") { id ->
        val shown=files.firstOrNull{it.id==id}?:focused
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Text(
            shown.name,
            Modifier.padding(horizontal = 24.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(7.dp))
        UiText(
            if (files.size == 1) formatSize(shown.size)
            else "${files.indexOfFirst{it.id==id}.coerceAtLeast(0)+1} / ${files.size} · ${formatSize(files.sumOf{it.size})}",
            fontSize = 12.sp,
            color = c.onSurfaceVariant,
        )
        }
        }
    }
}
