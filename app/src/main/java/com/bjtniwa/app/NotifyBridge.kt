package com.bjtniwa.app

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
}
