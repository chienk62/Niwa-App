package com.bjtniwa.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Cầu nối cho trang web trong app: bật/tắt thông báo từ vựng và gửi danh sách từ. */
class NotifyBridge(private val activity: MainActivity) {
    private val ctx = activity.applicationContext

    @JavascriptInterface
    fun setWords(json: String) { WordNotifier.prefs(ctx).edit().putString("words", json).apply() }

    @JavascriptInterface
    fun hasPermission(): Boolean = WordNotifier.canNotify(ctx)

    @JavascriptInterface
    fun requestPermission() { activity.runOnUiThread { activity.askNotificationPermission() } }

    @JavascriptInterface
    fun enable(minutes: Int): Boolean {
        if (!WordNotifier.canNotify(ctx)) return false
        val m = minutes.coerceIn(15, 24 * 60)
        WordNotifier.prefs(ctx).edit().putBoolean("enabled", true).putInt("minutes", m).apply()
        val req = PeriodicWorkRequestBuilder<WordWorker>(m.toLong(), TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WordWorker.NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        WordNotifier.show(ctx)
        return true
    }

    @JavascriptInterface
    fun disable() {
        WordNotifier.prefs(ctx).edit().putBoolean("enabled", false).apply()
        WorkManager.getInstance(ctx).cancelUniqueWork(WordWorker.NAME)
        WordNotifier.cancel(ctx)
    }

    @JavascriptInterface
    fun showNow(): Boolean = WordNotifier.show(ctx)

    /* ----- Ô từ vựng nổi trên các app khác ----- */
    @JavascriptInterface
    fun canOverlay(): Boolean = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx)

    @JavascriptInterface
    fun requestOverlay() {
        activity.runOnUiThread {
            try {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName)))
            } catch (e: Exception) {
                try { activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) } catch (_: Exception) {}
            }
        }
    }

    @JavascriptInterface
    fun startFloat(minutes: Int): Boolean {
        if (!canOverlay()) return false
        WordNotifier.prefs(ctx).edit().putBoolean("floatOn", true).putInt("floatMin", minutes.coerceIn(1, 120)).apply()
        activity.runOnUiThread { FloatService.start(ctx) }
        return true
    }

    @JavascriptInterface
    fun stopFloat() {
        WordNotifier.prefs(ctx).edit().putBoolean("floatOn", false).apply()
        activity.runOnUiThread { FloatService.stop(ctx) }
    }

    @JavascriptInterface
    fun floatRunning(): Boolean = WordNotifier.prefs(ctx).getBoolean("floatOn", false)
}
