package cn.qingzhuan.app

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Android API35 JVM flow test, not a physical device or FPS certification. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeFlowTest {
    @get:Rule val compose = createAndroidComposeRule<NativeHomeActivity>()

    private fun deliver(activity: NativeHomeActivity, request: Int, result: Int, data: Intent?) {
        val method =
            NativeHomeActivity::class
                .java
                .getDeclaredMethod(
                    "onActivityResult",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Intent::class.java,
                )
        method.isAccessible = true
        method.invoke(activity, request, result, data)
    }

    private fun capture(name: String) {
        runCatching {
                val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
                val folder = File("build/ui-api35")
                folder.mkdirs()
                File(folder, "$name.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
            .onFailure { println("CAPTURE_BLOCKED $name: $it") }
    }

    @Test
    fun android15_native_launch_batch_conversion_and_fresh_reopen() {
        compose.onNodeWithContentDescription("选择需要转换的文件").assertIsDisplayed()
        compose.onAllNodesWithText("转换完成").assertCountEquals(0)

        val activity = compose.activity
        val session = AppSession.get(activity)
        val source = File(activity.filesDir, "results/fixture/source.png")
        source.parentFile!!.mkdirs()
        javaClass.classLoader!!.getResourceAsStream("photo.png")!!.use { input ->
            source.outputStream().use { input.copyTo(it) }
        }
        val uri = Uri.parse("content://${activity.packageName}.files/fixture/source.png")
        compose.onNodeWithContentDescription("选择需要转换的文件").performClick()
        val picked =
            Intent().setData(uri).apply {
                clipData = ClipData.newRawUri("image", uri).also { it.addItem(ClipData.Item(uri)) }
            }
        compose.runOnUiThread { deliver(activity, 10, Activity.RESULT_OK, picked) }
        compose.waitUntil(20000) {
            session.snapshot().optJSONArray("inputs")?.length() == 2 &&
                !session.snapshot().optBoolean("busy")
        }
        compose.onAllNodesWithText("PNG").assertCountEquals(1)
        
        compose.onNodeWithContentDescription("JPG").performClick()
        val actions =
            compose
                .onNodeWithContentDescription("液态玻璃转换按钮，向上拖至中央松手")
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
        compose.runOnUiThread { assertTrue(actions.first().action()) }
        assertEquals("submit must reach Java: ${session.snapshot()}","convert",session.snapshot().getJSONObject("task").getString("kind"))
        try{compose.waitUntil(20000) {
            val t = session.snapshot().optJSONObject("task")
            if(t?.optString("kind")=="convert"&&t.optString("status") in listOf("error","cancelled","interrupted")) throw AssertionError("Backend failure: $t")
            t?.optString("kind") == "convert" && t.optString("status") == "complete"
        }
        }catch(e:Exception){throw AssertionError("Backend state: ${session.snapshot()}",e)}
        compose.mainClock.advanceTimeBy(4000)
        compose.onNodeWithText("转换完成").assertIsDisplayed()
        
        val outputs = session.snapshot().getJSONArray("outputs")
        assertEquals(2, outputs.length())
        val jpeg = session.output(outputs.getJSONObject(0).getString("id")).readBytes()
        assertEquals(255, jpeg[0].toInt() and 255)
        assertEquals(216, jpeg[1].toInt() and 255)
        compose.onNodeWithContentDescription("保存转换结果").assertIsDisplayed()
        compose.onNodeWithContentDescription("继续转换下一个文件").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithContentDescription("选择需要转换的文件").assertIsDisplayed()
        compose.onAllNodesWithText("转换完成").assertCountEquals(0)
        assertEquals(2, session.snapshot().getJSONArray("outputs").length())
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("已完成文件").performClick()
        compose.onNodeWithContentDescription("全选").performClick()
        compose.onNodeWithContentDescription("保存所选结果").performClick()
        val current = compose.activity
        val requested = shadowOf(current).nextStartedActivityForResult
        assertEquals(12, requested.requestCode)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, requested.intent.action)
        compose.runOnUiThread { deliver(current, 12, Activity.RESULT_CANCELED, null) }
        compose.onNodeWithContentDescription("删除所选文件").performClick()
        compose.onNodeWithText("删除", useUnmergedTree = true).performClick()
        compose.waitUntil(10000) { session.snapshot().getJSONArray("outputs").length() == 0 }
        compose.onNodeWithText("还没有转换结果").assertExists()
    }
}
