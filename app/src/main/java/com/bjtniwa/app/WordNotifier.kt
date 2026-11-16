package com.bjtniwa.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray

/** Hiện một từ vựng trên thanh thông báo. Danh sách từ do trang web trong app gửi sang (AndroidNotify.setWords). */
object WordNotifier {
    const val CHANNEL = "niwa_words_v2"
    const val NOTIF_ID = 2101

    fun prefs(c: Context): SharedPreferences = c.getSharedPreferences("niwa_notify", Context.MODE_PRIVATE)

    fun canNotify(c: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(c).areNotificationsEnabled()
    }

    private fun ensureChannel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        nm.deleteNotificationChannel("niwa_words")   /* kênh cũ (mức thấp, bị ẩn ở màn hình khóa) */
        if (nm.getNotificationChannel(CHANNEL) == null) {
            /* Mức mặc định nhưng không chuông, không rung: hiện cả trên màn hình khóa */
            val ch = NotificationChannel(CHANNEL, "Từ vựng Niwa", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Hiện từ vựng định kỳ trên thanh thông báo và màn hình khóa"
            ch.setSound(null, null)
            ch.enableVibration(false)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
    }

    fun show(c: Context): Boolean {
        if (!canNotify(c)) return false
        val p = prefs(c)
        val arr = try { JSONArray(p.getString("words", "[]")) } catch (e: Exception) { JSONArray() }
        if (arr.length() == 0) return false
        var i = (0 until arr.length()).random()
        val last = p.getInt("last", -1)
        if (arr.length() > 1 && i == last) i = (i + 1) % arr.length()
        p.edit().putInt("last", i).apply()

        val w = arr.optJSONObject(i) ?: return false
        val front = w.optString("f")
        val reading = w.optString("r")
        val meaning = w.optString("m")
        val example = w.optString("e")
        if (front.isEmpty()) return false

        ensureChannel(c)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            c, 0,
            Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags
        )
        val next = PendingIntent.getBroadcast(c, 1, Intent(c, NextWordReceiver::class.java), flags)
        val title = if (reading.isNotEmpty()) "$front  ·  $reading" else front
        val big = if (example.isNotEmpty()) "$meaning\n\n$example" else meaning

        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_niwa)
            .setContentTitle(title)
            .setContentText(meaning)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setContentIntent(open)
            .addAction(0, "Từ khác", next)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()
        return try { NotificationManagerCompat.from(c).notify(NOTIF_ID, n); true } catch (e: SecurityException) { false }
    }

    fun cancel(c: Context) { NotificationManagerCompat.from(c).cancel(NOTIF_ID) }
}
