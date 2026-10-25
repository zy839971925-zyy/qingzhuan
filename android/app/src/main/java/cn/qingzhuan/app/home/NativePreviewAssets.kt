package cn.qingzhuan.app

import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.*
import android.util.LruCache
import java.io.File
import kotlin.math.*

object NativePreviewAssets {
    private val cache=object:LruCache<String,Bitmap>(8*1024*1024){override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount}
    fun pageCount(file:File):Int=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use { fd->PdfRenderer(fd).use{it.pageCount} }
    fun page(file:File,index:Int,edge:Int):Bitmap {
        val key="${file.path}:${file.length()}:${file.lastModified()}:$index:$edge"
        cache.get(key)?.let{return it}
        val b=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use { fd->PdfRenderer(fd).use { renderer->renderer.openPage(index).use { page->
            val scale=edge.coerceIn(128,1600).toFloat()/max(page.width,page.height)
            val bitmap=Bitmap.createBitmap(max(1,(page.width*scale).roundToInt()),max(1,(page.height*scale).roundToInt()),Bitmap.Config.ARGB_8888)
            try{bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);bitmap}catch(e:Throwable){bitmap.recycle();throw e}
        } } }
        cache.put(key,b);return b
    }
    @Synchronized fun thumbnail(file:File):Bitmap?=try {
        val type=OutputProvider.mime(file.name)
        when {
            file.extension.equals("pdf",true)->page(file,0,384)
            type.startsWith("video/")->{
                val key="video:${file.path}:${file.length()}:${file.lastModified()}"
                cache.get(key)?:run{val retriever=MediaMetadataRetriever();try{retriever.setDataSource(file.path)
                    val b=if(Build.VERSION.SDK_INT>=27) retriever.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,384,384)
                    else retriever.getFrameAtTime(0)?.let{raw->val s=384f/max(raw.width,raw.height);val scaled=Bitmap.createScaledBitmap(raw,max(1,(raw.width*s).roundToInt()),max(1,(raw.height*s).roundToInt()),true);if(scaled!=raw)raw.recycle();scaled}
                    b?.also{cache.put(key,it)}
                }finally{retriever.release()}}
            }
            else->NativeMediaStore.decode(file,384)
        }
    }catch(_:Exception){null}catch(_:OutOfMemoryError){null}
}
