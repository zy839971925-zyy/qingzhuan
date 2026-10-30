package cn.qingzhuan.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real Compose transforms, no emulator. Not a physical optical or FPS test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Motion155Test {
    @get:Rule val compose=createAndroidComposeRule<NativeHomeActivity>()
    @Test fun segmented_track_stays_fixed_when_pressed_and_cancelled() {
        var chosen="png"
        compose.activity.setContent {
            QingzhuanTheme(false) {
                var selected by remember { mutableStateOf("png") }
                Box(Modifier.fillMaxSize().padding(24.dp)) {
                    GlassSegments(listOf(GlassChoice("png","PNG"),GlassChoice("webp","WEBP"),GlassChoice("pdf","PDF")),selected,Modifier.fillMaxWidth()) { selected=it;chosen=it }
                }
            }
        }
        compose.waitForIdle()
        val track=compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        val before=track.fetchSemanticsNode().boundsInRoot
        compose.mainClock.autoAdvance=false
        compose.onNodeWithContentDescription("PNG").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(320)
        val held=track.fetchSemanticsNode().boundsInRoot
        assertEquals("press must not lift the underlying track",before.top,held.top,.1f)
        assertEquals("press must not change the underlying height",before.height,held.height,.1f)
        compose.onNodeWithContentDescription("PNG").performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(1000)
        assertEquals("cancel keeps selection","png",chosen)
        val released=track.fetchSemanticsNode().boundsInRoot
        assertEquals(before.top,released.top,.1f)
        compose.mainClock.autoAdvance=true
        compose.onNodeWithContentDescription("PDF").performClick()
        compose.runOnIdle { assertEquals("pdf",chosen) }
    }
    @Test fun sheet_subpage_has_back_path_to_menu_without_resetting_home() {
        compose.runOnUiThread { AppText.setLanguage(compose.activity,"en") }
        compose.onNodeWithContentDescription("More actions").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Language").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        compose.onNodeWithText("Import history").assertIsDisplayed()
        compose.onNodeWithText("Language").assertDoesNotExist()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithContentDescription("More actions").assertIsDisplayed()
    }

    @Test fun panel_history_retains_origin_and_closes_root_without_stale_forward_page() {
        val panels=SheetNavigation()
        panels.open("menu");panels.navigate("settings");panels.navigate("background");panels.navigate("background-import")
        panels.back();assertEquals("background",panels.current)
        panels.back();assertEquals("settings",panels.current)
        panels.close();assertNull(panels.current);assertFalse(panels.canGoBack)
        panels.open("options");panels.back();assertNull(panels.current)
    }

    private fun stubActions(onPick:()->Unit={},onDelete:()->Unit={}):HomeActions = HomeActions(
        pick=onPick,pickPhotos=onPick,convert={_,_,_,_->false},cancel={},save={},share={},open={},
        removeInputs={onDelete()},removeOutputs={onDelete()},setTheme={},readLicenses={""},haptic={},notice={},
        preview={_,_->null},pickBackground={},pickBackgroundPhotos={},setBackground={_,_->},
        backgroundFile={File(compose.activity.cacheDir,"missing-background")},
    )
    @Test fun single_preview_remains_centered_for_input_and_result() {
        val actions=stubActions()
        var output by mutableStateOf(false)
        compose.activity.setContent { QingzhuanTheme(false) {
            Box(Modifier.fillMaxSize()) {
                FilePreviewDeck(listOf(HomeFile("one","photo.png",128,format="png")),actions,104.dp,output=output){}
            }
        } }
        for(result in listOf(false,true)) {
            compose.runOnIdle{output=result}
            val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
            val card=compose.onNodeWithText("PNG").fetchSemanticsNode().boundsInRoot
            assertEquals("single preview center, output=$result",root.center.x,card.center.x,1f)
        }
    }
    @Test fun toolbar_back_cancels_confirmation_and_stale_delete_cannot_execute() {
        var parents=0;var deletes=0
        val actions=stubActions {deletes++}
        compose.activity.setContent { QingzhuanTheme(false) {
            NativeSheet("inputs",HomeSnapshot(inputs=listOf(HomeFile("one","photo.png",128))),null,emptyMap(),{},"system",BackgroundStyle(),actions,{}, {}, {}, {},emptyList(),backdrop=rememberLayerBackdrop(),canGoBack=true,onBack={parents++})
        } }
        compose.onNodeWithContentDescription("全选").performClick()
        compose.onNodeWithContentDescription("删除所选文件").performClick()
        compose.onNodeWithText("删除 1 项？").assertIsDisplayed()
        val oldDelete=compose.onNodeWithText("删除").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.onNodeWithContentDescription("返回").performClick()
        compose.runOnIdle { assertEquals("Back should cancel before leaving",0,parents) }
        compose.onNodeWithText("删除 1 项？").assertDoesNotExist()
        compose.runOnUiThread { oldDelete() }
        compose.runOnIdle { assertEquals("stale confirmation cannot delete",0,deletes) }
    }
    @Test fun outgoing_menu_action_cannot_navigate_after_page_exit() {
        compose.runOnUiThread { AppText.setLanguage(compose.activity,"en") }
        compose.onNodeWithContentDescription("More actions").performClick()
        val oldBackground=compose.onNodeWithText("Background & glass").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Language").assertIsDisplayed()
        compose.runOnUiThread { oldBackground() }
        compose.onNodeWithText("Language").assertIsDisplayed()
    }

    @Test fun same_frame_old_menu_action_cannot_override_new_page() {
        compose.runOnUiThread { AppText.setLanguage(compose.activity,"en") }
        compose.onNodeWithContentDescription("More actions").performClick()
        val settings=compose.onNodeWithText("Settings").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        val oldBackground=compose.onNodeWithText("Background & glass").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { settings();oldBackground() }
        compose.onNodeWithText("Language").assertIsDisplayed()
    }

    @Test fun import_request_runs_once_before_dismissal_and_stale_picker_is_blocked() {
        var route by mutableStateOf<String?>("import")
        var picks=0
        val actions=stubActions(onPick={picks++})
        compose.activity.setContent { QingzhuanTheme(false) {
            NativeSheet(route,HomeSnapshot(),null,emptyMap(),{},"system",BackgroundStyle(),actions,{}, {route=null}, {}, {},emptyList(),backdrop=rememberLayerBackdrop(),isCurrentPage={route==it})
        } }
        val picker=compose.onNodeWithText("从文件导入").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { picker();picker() }
        compose.runOnIdle { assertEquals("picker request once",1,picks);assertNull(route) }
    }

}
