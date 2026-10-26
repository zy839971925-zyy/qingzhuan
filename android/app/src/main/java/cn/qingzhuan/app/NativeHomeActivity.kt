package cn.qingzhuan.app

import android.app.Activity
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import org.json.JSONArray

/** Thin native host. AppSession remains the sole owner of conversion and files. */
class NativeHomeActivity : ComponentActivity() {
    private lateinit var session: AppSession
    private var snapshot by mutableStateOf(HomeSnapshot())
    private val outcomes = ConversionOutcomes()
    private var conversionOutcome by mutableStateOf<HomeTask?>(null)
    private var newIds by mutableStateOf<List<String>>(emptyList())
    private var importEpoch by mutableLongStateOf(0)
    private var resetEpoch by mutableLongStateOf(0)
    private var backgroundStyle by mutableStateOf(BackgroundStyle())
    private var uiEpoch by mutableIntStateOf(0)
    private var importBefore: Set<String>? = null
    private var stopped = false
    private var external = false
    private var lastNotice = ""
    private val backgroundPhotoPicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null)
                guarded {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    } catch (_: Exception) {}
                    DocumentInbox(File(filesDir, "document-result.json"))
                        .put(
                            13,
                            Activity.RESULT_OK,
                            JSONArray().put(uri.toString()),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    drainInbox()
                }
        }
    private val photoPicker =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
            guarded {
                if (uris.isEmpty()) {
                    importBefore = null
                } else {
                    for (uri in uris) try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    } catch (_: Exception) {}
                    DocumentInbox(File(filesDir, "document-result.json"))
                        .put(
                            14,
                            Activity.RESULT_OK,
                            JSONArray(uris.map { it.toString() }),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    drainInbox()
                }
            }
        }
    private val observer =
        AppSession.Listener { raw ->
            runOnUiThread {
                val next = raw.homeSnapshot()
                if (next.revision < snapshot.revision) return@runOnUiThread
                outcomes.observe(raw.optJSONObject("lastConversion"))
                outcomes.observe(raw.optJSONObject("task"))
                conversionOutcome = outcomes.latest()?.homeTask()
                snapshot = next
                if (!next.busy) drainGalleryContinuation()
                val terminal = next.task.id + ":" + next.task.seq
                if (!next.busy && terminal != lastNotice && next.task.id.isNotEmpty()) {
                    lastNotice = terminal
                    if (
                        next.task.kind != "convert" &&
                            next.task.status in listOf("error", "cancelled", "interrupted")
                    )
                        notice(next.task.message)
                    else if (next.task.kind == "gallery" && next.task.status == "complete")
                        notice("已保存到相册")
                    else if (next.task.kind == "export" && next.task.status == "complete")
                        notice("已保存到所选位置")
                }
                if (!next.busy && next.task.kind == "import" && next.task.status == "complete")
                    importBefore?.let { before ->
                        val fresh = next.inputs.filter { it.id !in before }.map { it.id }
                        if (fresh.isNotEmpty()) {
                            newIds = fresh
                            importEpoch++
                            importBefore = null
                        }
                    }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyTheme()
        backgroundStyle = readBackground()
        getSharedPreferences("appearance", 0).registerOnSharedPreferenceChangeListener(appearanceListener)
        StartupDiagnostics.phase("Jetpack Compose 原生首页")
        session = AppSession.get(this)
        val initial=session.snapshot()
        snapshot = initial.homeSnapshot()
        outcomes.observe(initial.optJSONObject("lastConversion"))
        outcomes.observe(initial.optJSONObject("task"))
        conversionOutcome=outcomes.latest()?.homeTask()
        lastNotice = snapshot.task.id + ":" + snapshot.task.seq
        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val prefs = getSharedPreferences("appearance", 0)
            val mode = remember(uiEpoch) { prefs.getString("theme", "system") ?: "system" }
            val dark = mode == "dark" || (mode == "system" && systemDark)
            val reduced =
                try {
                    Settings.Global.getFloat(
                        contentResolver,
                        Settings.Global.ANIMATOR_DURATION_SCALE,
                        1f,
                    ) == 0f
                } catch (_: Exception) {
                    false
                }
            val power = (getSystemService(POWER_SERVICE) as PowerManager).isPowerSaveMode
            UiLanguageProvider { NativeHome(
                snapshot,
                conversionOutcome,
                newIds,
                importEpoch,
                resetEpoch,
                dark,
                reduced || power,
                mode,
                backgroundStyle,
                actions(),
            ) }
            LaunchedEffect(Unit) {
                withFrameNanos {}
                StartupDiagnostics.ready()
            }
        }
        drainInbox()
        receive(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::session.isInitialized) {
            if (stopped && !external && !session.snapshot().optBoolean("busy") && !session.pendingConversion()) resetEpoch++
            stopped = false
            external = false
            session.attach(observer)
            drainInbox()
            drainGalleryContinuation()
            backgroundStyle = readBackground()
            applyTheme()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        window.decorView.post {
            if (!isDestroyed && ::session.isInitialized) drainGalleryContinuation()
        }
    }

    override fun onStop() {
        stopped = true
        super.onStop()
    }

    override fun onDestroy() {
        if (::session.isInitialized) session.detach(observer)
        getSharedPreferences("appearance", 0).unregisterOnSharedPreferenceChangeListener(appearanceListener)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!session.snapshot().optBoolean("busy") && !session.pendingConversion()) resetEpoch++
        receive(intent)
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        uiEpoch++
        applyTheme()
    }

    private fun notice(s: String) {
        runOnUiThread { Toast.makeText(this, AppText.text(this,s), Toast.LENGTH_LONG).show() }
    }

    private inline fun guarded(work: () -> Unit) {
        try {
            work()
        } catch (e: Exception) {
            notice(AppSession.message(e))
        }
    }

    private fun actions() =
        HomeActions(
            pick = {
                guarded {
                    if (!session.snapshot().optBoolean("busy")) {
                        importBefore = snapshot.inputs.map { it.id }.toSet()
                        external = true
                        startActivityForResult(
                            Intent(Intent.ACTION_OPEN_DOCUMENT)
                                .addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("*/*")
                                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true),
                            10,
                        )
                    }
                }
            },
            pickPhotos = {
                guarded {
                    if (!session.snapshot().optBoolean("busy")) {
                        importBefore = snapshot.inputs.map { it.id }.toSet()
                        external = true
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            )
                        )
                    }
                }
            },
            acknowledge = { session.acknowledgeConversion(it) },
            convert = { id, tool, opts, ids ->
                try {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED && !getSharedPreferences("appearance",0).getBoolean("askedNotifications",false)) {
                        getSharedPreferences("appearance",0).edit().putBoolean("askedNotifications",true).apply()
                        external = true
                        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 33)
                    }
                    session.convert(id, tool, opts.toString(), JSONArray(ids).toString())
                    true
                } catch (e: Exception) {
                    notice(AppSession.message(e))
                    false
                }
            },
            cancel = { session.cancel(it) },
            save = { ids -> guarded { saveOutputs(ids) } },
            share = { useOutput(it, true) },
            open = { openPreview(it,true) },
            openInput = { openPreview(it,false) },
            removeInputs = { ids -> guarded { session.removeSelected(JSONArray(ids).toString()) } },
            removeOutputs = { ids ->
                guarded { session.deleteSelected(JSONArray(ids).toString()) }
            },
            languageMode={ AppText.mode(this) },
            setLanguage={AppText.setLanguage(this,it);uiEpoch++},
            setTheme = { mode ->
                getSharedPreferences("appearance", 0).edit().putString("theme", mode).apply()
                uiEpoch++
                applyTheme()
            },
            readLicenses = { assets.open("licenses.txt").bufferedReader().use { it.readText() } },
            haptic = {
                window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                Unit
            },
            notice = { notice(it) },
            preview = { id, output ->
                try {
                    NativePreviewAssets.thumbnail(if(output)session.output(id)else session.previewInput(id))
                } catch (_: Exception) {
                    null
                }
            },
            backgroundFile = { NativeMediaStore.wallpaper(this) },
            pickBackground = { pickWallpaper() },
            pickBackgroundPhotos = {
                guarded {
                    if (NativeMediaStore.wallpaperBusy) {
                        notice("正在导入背景，请稍候")
                        return@guarded
                    }
                    external = true
                    backgroundPhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            },
            setBackground = { mode, blur -> updateBackground(backgroundStyle.copy(mode=mode,blur=blur)) },
            setBackgroundStyle = { updateBackground(it) },
        )

    private fun saveOutputs(ids: List<String>) {
        if (session.snapshot().optBoolean("busy")) throw IOException("请等待当前任务结束")
        val known = session.selectedOutputs(JSONArray(ids).toString()).files()
        val media =
            known
                .filter {
                    val mime = OutputProvider.mime(it.name)
                    mime.startsWith("image/") || mime.startsWith("video/")
                }
                .map { it.id }
        val documents = known.filter { it.id !in media }.map { it.id }
        if (
            media.isNotEmpty() &&
                Build.VERSION.SDK_INT < 29 &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            getSharedPreferences("gallery-permission", 0)
                .edit()
                .putString("ids", JSONArray(ids).toString())
                .apply()
            external = true
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 32)
            return
        }
        if (media.isNotEmpty())
            session.saveToGallery(JSONArray(media).toString(), JSONArray(documents).toString())
        else exportDocuments(documents)
    }

    private fun exportDocuments(ids: List<String>) {
        if (ids.isEmpty()) return
        if (session.snapshot().optBoolean("busy")) throw IOException("请从已完成列表继续保存其余文件")
        if (ids.size == 1) {
            export(session.output(ids[0]))
            return
        }
        session.selectedOutputs(JSONArray(ids).toString())
        val prefs = getSharedPreferences("pending-folder", 0)
        if (prefs.contains("ids")) throw IOException("请先完成当前文件夹选择")
        prefs.edit().putString("ids", JSONArray(ids).toString()).commit()
        try {
            external = true
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    ),
                12,
            )
        } catch (e: Exception) {
            prefs.edit().remove("ids").apply()
            throw e
        }
    }

    @Deprecated("Legacy Android8/9 gallery permission only")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 32) {
            val prefs = getSharedPreferences("gallery-permission", 0)
            val json = prefs.getString("ids", null)
            prefs.edit().remove("ids").apply()
            if (json != null)
                guarded {
                    val ids = JSONArray(json).strings()
                    if (
                        grantResults.firstOrNull() ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    )
                        saveOutputs(ids)
                    else {
                        notice("未获相册权限，可保存到文件")
                        exportDocuments(ids)
                    }
                }
        }
    }

    private fun export(file: File) {
        val p = getSharedPreferences("pending-export", 0)
        if (p.contains("path")) throw IOException("请先完成当前保存操作")
        p.edit().putString("path", file.path).commit()
        try {
            external = true
            startActivityForResult(
                Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(OutputProvider.mime(file.name))
                    .putExtra(Intent.EXTRA_TITLE, file.name),
                11,
            )
        } catch (e: Exception) {
            p.edit().remove("path").apply()
            throw e
        }
    }

    private fun openPreview(id:String,output:Boolean)=guarded {
        if(output)session.selectedOutputs(JSONArray().put(id).toString())else session.previewInput(id)
        external=true
        startActivity(Intent(this,NativePreviewActivity::class.java).putExtra("id",id).putExtra("output",output))
    }

    private fun useOutput(id: String, share: Boolean) = guarded {
        val f = session.output(id)
        val uri = Uri.parse("content://$packageName.files/" + Uri.encode(id, "/"))
        val i = Intent(if (share) Intent.ACTION_SEND else Intent.ACTION_VIEW)
        if (share) {
            i.type = OutputProvider.mime(f.name)
            i.putExtra(Intent.EXTRA_STREAM, uri)
        } else i.setDataAndType(uri, OutputProvider.mime(f.name))
        i.clipData = ClipData.newRawUri("轻转文件", uri)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        external = true
        startActivity(if (share) Intent.createChooser(i, AppText.text(this,"分享文件")) else i)
    }

    @Deprecated("Host-owned durable SAF inbox")
    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if (request !in listOf(10, 11, 12, 13, 14)) return
        guarded {
            val uris = JSONArray()
            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) uris.put(clip.getItemAt(i).uri.toString())
            }
            if (uris.length() == 0) data?.data?.let { uris.put(it.toString()) }
            val flags = data?.flags ?: 0
            if (flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0)
                for (i in 0 until uris.length()) try {
                    contentResolver.takePersistableUriPermission(
                        Uri.parse(uris.getString(i)),
                        flags and
                            (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
                    )
                } catch (_: Exception) {}
            DocumentInbox(File(filesDir, "document-result.json")).put(request, result, uris, flags)
            drainInbox()
        }
    }

    private fun drainInbox() = guarded {
        val entry = DocumentInbox(File(filesDir, "document-result.json")).take() ?: return@guarded
        val uris = entry.optJSONArray("uris") ?: JSONArray()
        val result = entry.optInt("result")
        val uri = if (uris.length() > 0) Uri.parse(uris.getString(0)) else null
        when (entry.optInt("request")) {
            13 ->
                if (result == Activity.RESULT_OK && uri != null) {
                    NativeMediaStore.importWallpaperAsync(applicationContext, uri)
                }
            10,
            14 ->
                if (result == Activity.RESULT_OK) {
                    if (importBefore == null) importBefore = snapshot.inputs.map { it.id }.toSet()
                    session.importUris(List(uris.length()) { Uri.parse(uris.getString(it)) })
                } else importBefore = null
            11 -> {
                val p = getSharedPreferences("pending-export", 0)
                val path = p.getString("path", null)
                p.edit().remove("path").apply()
                if (path != null) {
                    val file = File(path).canonicalFile
                    val prefix = session.resultRoot.canonicalPath + "/"
                    if (!file.path.startsWith(prefix)) throw IOException("保存来源无效")
                    if (result == Activity.RESULT_OK && uri != null) session.saveTo(file, uri)
                }
            }
            12 -> {
                val p = getSharedPreferences("pending-folder", 0)
                val ids = p.getString("ids", null)
                p.edit().remove("ids").apply()
                if (ids != null && result == Activity.RESULT_OK && uri != null)
                    session.saveSelectedToFolder(ids, uri)
            }
        }
    }

    // Application worker never holds an Activity. A resumed host owns the next SAF launch.
    private fun drainGalleryContinuation() {
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return
        if (session.snapshot().optBoolean("busy")) return
        val prefs = getSharedPreferences("gallery-followup", 0)
        val json = prefs.getString("ids", null) ?: return
        prefs.edit().remove("ids").commit()
        guarded { exportDocuments(JSONArray(json).strings()) }
    }

    private val appearanceListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        runOnUiThread {
            if (key == "language")uiEpoch++
            if (key != null && key.startsWith("background")) backgroundStyle = readBackground()
            if (key == "background-error") {
                prefs.getString(key, null)?.let { notice(it) }
                prefs.edit().remove(key).apply()
            }
        }
    }

    private fun readBackground(): BackgroundStyle {
        val p = getSharedPreferences("appearance", 0)
        return BackgroundCatalog.normalize(BackgroundStyle(
            p.getString("background", "aurora") ?: "aurora",
            p.getFloat("background-blur", 18f).coerceIn(0f, 40f),
            p.getLong("background-revision", 0),
            p.getString("background-color", "DFE9E5") ?: "DFE9E5",
            p.getString("background-preset", "mist") ?: "mist",
            p.getFloat("background-light-overlay",.68f),
            p.getFloat("background-dark-overlay",.72f),
        ))
    }

    private fun updateBackground(value: BackgroundStyle) {
        val style=BackgroundCatalog.normalize(value)
        if(style.mode=="custom"&&!NativeMediaStore.wallpaper(this).isFile) {pickWallpaper();return}
        getSharedPreferences("appearance",0).edit()
            .putString("background",style.mode)
            .putFloat("background-blur",style.blur)
            .putString("background-color",style.colorHex)
            .putString("background-preset",style.preset)
            .putFloat("background-light-overlay",style.lightOverlay)
            .putFloat("background-dark-overlay",style.darkOverlay).apply()
        backgroundStyle=readBackground()
    }

    private fun pickWallpaper() = guarded {
        if (NativeMediaStore.wallpaperBusy) {
            notice("正在导入背景，请稍候")
            return@guarded
        }
        external = true
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*"),
            13,
        )
    }

    @Suppress("DEPRECATION")
    private fun receive(i: Intent?) {
        if (i == null) return
        val uris =
            when (i.action) {
                Intent.ACTION_SEND -> listOfNotNull(i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
                Intent.ACTION_SEND_MULTIPLE ->
                    i.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
                else -> emptyList()
            }
        i.action = null
        if (uris.isNotEmpty())
            guarded {
                importBefore = snapshot.inputs.map { it.id }.toSet()
                session.importUris(uris)
            }
    }

    @Suppress("DEPRECATION")
    fun applyTheme() {
        val mode = getSharedPreferences("appearance", 0).getString("theme", "system")
        val dark =
            mode == "dark" ||
                (mode == "system" &&
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                        Configuration.UI_MODE_NIGHT_YES)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        window.decorView.setBackgroundColor(Color.parseColor(if (dark) "#171B19" else "#F7F8F3"))
    }
}
