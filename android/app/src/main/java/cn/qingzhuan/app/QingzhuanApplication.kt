package cn.qingzhuan.app

import android.app.Application
import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/** Native-only startup: no JS runtime, native renderer loading or bundler. */
class QingzhuanApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        StartupDiagnostics.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (StartupDiagnostics.hasPreviousFailure()) return
        StartupDiagnostics.begin()
        try {
            StartupDiagnostics.phase("原生文件引擎初始化")
            PDFBoxResourceLoader.init(this)
        } catch (e: Throwable) {
            StartupDiagnostics.capture("文件引擎初始化", e)
        }
    }
}
