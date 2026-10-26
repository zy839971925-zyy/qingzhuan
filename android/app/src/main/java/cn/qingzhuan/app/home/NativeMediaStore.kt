package cn.qingzhuan.app

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.util.AtomicFile
import android.util.LruCache
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/** Bounded app-private thumbnails/backgrounds. Never alters imported conversion originals. */
object NativeMediaStore {
    private val importer = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val importing = java.util.concurrent.atomic.AtomicBoolean(false)
    val wallpaperBusy: Boolean get() = importing.get()

    fun importWallpaperAsync(context: Context, uri: Uri) {
        if (!importing.compareAndSet(false, true)) throw IOException("正在导入背景，请稍候")
        val app = context.applicationContext
        importer.execute {
            try {
                val revision = importWallpaper(app, uri)
                app.getSharedPreferences("appearance", 0).edit()
                    .putString("background", "custom")
                    .putLong("background-revision", revision).commit()
            } catch (e: Exception) {
                app.getSharedPreferences("appearance", 0).edit()
                    .putString("background-error", AppSession.message(e)).commit()
            } finally {
                importing.set(false)
            }
        }
    }

    // Pre-Android12 has no RenderEffect blur. Three bounded box passes approximate Gaussian blur.
    fun legacyBlur(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount <= 0f) return bitmap
        val edge = 400
        val scale = minOf(1f, edge.toFloat() / max(bitmap.width, bitmap.height))
        val w = max(1, (bitmap.width * scale).roundToInt())
        val h = max(1, (bitmap.height * scale).roundToInt())
        val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
        var pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        val radius = max(1, (amount * .28f).roundToInt())
        repeat(3) {
            val horizontal = IntArray(pixels.size)
            val output = IntArray(pixels.size)
            fun pass(input: IntArray, target: IntArray, width: Int, height: Int, transpose: Boolean) {
                for (y in 0 until height) {
                    var a=0; var r=0; var g=0; var b=0
                    fun index(x: Int): Int = if (transpose) x.coerceIn(0, width-1)*w+y else y*w+x.coerceIn(0, width-1)
                    fun add(color: Int, sign: Int) {
                        a += (color ushr 24)*sign; r += (color ushr 16 and 255)*sign
                        g += (color ushr 8 and 255)*sign; b += (color and 255)*sign
                    }
                    for (x in -radius..radius) add(input[index(x)],1)
                    val count = radius*2+1
                    for (x in 0 until width) {
                        target[index(x)] = ((a/count) shl 24) or ((r/count) shl 16) or ((g/count) shl 8) or (b/count)
                        add(input[index(x-radius)],-1); add(input[index(x+radius+1)],1)
                    }
                }
            }
            pass(pixels,horizontal,w,h,false)
            pass(horizontal,output,h,w,true)
            pixels = output
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private val cache =
        object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
        }

    fun decode(file: File, edge: Int): Bitmap? {
        if (!file.isFile) return null
        val key = "${file.path}:${file.lastModified()}:${file.length()}:$edge"
        cache.get(key)?.let {
            return it
        }
        val bitmap =
            try {
                if (Build.VERSION.SDK_INT >= 28)
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                        val factor =
                            minOf(1f, edge.toFloat() / max(info.size.width, info.size.height))
                        decoder.setTargetSize(
                            max(1, (info.size.width * factor).roundToInt()),
                            max(1, (info.size.height * factor).roundToInt()),
                        )
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                else {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path, bounds)
                    var sample = 1
                    while (max(bounds.outWidth, bounds.outHeight) / sample > edge) sample *= 2
                    val raw =
                        BitmapFactory.decodeFile(
                            file.path,
                            BitmapFactory.Options().apply { inSampleSize = sample },
                        ) ?: return null
                    val orientation =
                        try {
                            android.media
                                .ExifInterface(file.path)
                                .getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
                        } catch (_: Exception) {
                            1
                        }
                    val matrix = Matrix()
                    when (orientation) {
                        2 -> matrix.setScale(-1f, 1f)
                        3 -> matrix.setRotate(180f)
                        4 -> matrix.setScale(1f, -1f)
                        5 -> {
                            matrix.setRotate(90f)
                            matrix.postScale(-1f, 1f)
                        }
                        6 -> matrix.setRotate(90f)
                        7 -> {
                            matrix.setRotate(270f)
                            matrix.postScale(-1f, 1f)
                        }
                        8 -> matrix.setRotate(270f)
                    }
                    val oriented =
                        if (orientation in 2..8)
                            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                        else raw
                    val scale = minOf(1f, edge.toFloat() / max(oriented.width, oriented.height))
                    if (scale < 1)
                        Bitmap.createScaledBitmap(
                            oriented,
                            max(1, (oriented.width * scale).roundToInt()),
                            max(1, (oriented.height * scale).roundToInt()),
                            true,
                        )
                    else oriented
                }
            } catch (_: Exception) {
                null
            } catch (_: OutOfMemoryError) {
                null
            }
        if (bitmap != null) cache.put(key, bitmap)
        return bitmap
    }

    fun wallpaper(context: Context) = File(context.filesDir, "appearance/wallpaper.png")

    fun importWallpaper(context: Context, uri: Uri): Long {
        if (uri.scheme != "content") throw IOException("请选择系统授权的图片")
        val folder = File(context.filesDir, "appearance")
        folder.mkdirs()
        val incoming = File.createTempFile("incoming-", ".image", folder)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IOException("无法读取图片")
                incoming.outputStream().use { out ->
                    Core.copy(input, out, 32L * 1024 * 1024, Core.NONE)
                }
            }
            val decoded = decode(incoming, 1600) ?: throw IOException("手机无法解码这张背景图片")
            val atomic = AtomicFile(wallpaper(context))
            val output = atomic.startWrite()
            try {
                if (!decoded.compress(Bitmap.CompressFormat.PNG, 100, output))
                    throw IOException("无法保存背景图片")
                atomic.finishWrite(output)
            } catch (e: Throwable) {
                atomic.failWrite(output)
                throw e
            }
            cache.evictAll()
            return System.currentTimeMillis()
        } finally {
            incoming.delete()
        }
    }
}

data class BackgroundStyle(
    val mode: String = "aurora",
    val blur: Float = 18f,
    val revision: Long = 0,
    val colorHex: String = "DFE9E5",
    val preset: String = "mist",
    val lightOverlay: Float = .68f,
    val darkOverlay: Float = .72f,
)
