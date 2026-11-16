package com.bjtniwa.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import kotlin.math.abs

/** Ô từ vựng nổi trên các app khác (cần quyền “Hiển thị trên ứng dụng khác”). */
class FloatService : Service() {

    companion object {
        const val ACTION_STOP = "com.bjtniwa.app.FLOAT_STOP"
        const val ACTION_NEXT = "com.bjtniwa.app.FLOAT_NEXT"
        private const val CHANNEL = "niwa_float"
        private const val NOTIF_ID = 2102

        fun start(c: Context) {
            val i = Intent(c, FloatService::class.java)
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i)
        }
        fun stop(c: Context) { c.stopService(Intent(c, FloatService::class.java)) }
    }

    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private lateinit var lp: WindowManager.LayoutParams
    private lateinit var tvWord: TextView
    private lateinit var tvRead: TextView
    private lateinit var tvMean: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var expanded = false
    private val tick = object : Runnable {
        override fun run() { nextWord(); handler.postDelayed(this, intervalMs()) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
    private fun intervalMs(): Long = WordNotifier.prefs(this).getInt("floatMin", 5).coerceIn(1, 120) * 60_000L

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        buildView()
        nextWord()
        handler.postDelayed(tick, intervalMs())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                WordNotifier.prefs(this).edit().putBoolean("floatOn", false).apply()
                stopSelf(); return START_NOT_STICKY
            }
            ACTION_NEXT -> nextWord()
            else -> { handler.removeCallbacks(tick); handler.postDelayed(tick, intervalMs()) }
        }
        return START_STICKY
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(CHANNEL, "Ô từ vựng nổi", NotificationManager.IMPORTANCE_MIN)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val stop = PendingIntent.getService(this, 3, Intent(this, FloatService::class.java).setAction(ACTION_STOP), flags)
        val open = PendingIntent.getActivity(this, 4, Intent(this, MainActivity::class.java), flags)
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_niwa)
            .setContentTitle("Niwa đang hiện từ vựng trên màn hình")
            .setContentText("Chạm “Tắt” để ẩn ô từ vựng nổi")
            .setContentIntent(open)
            .addAction(0, "Tắt", stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIF_ID, n)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildView() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14f), dp(10f), dp(10f), dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = dp(18f).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1.5f), Color.parseColor("#9CC2F2"))
            }
            elevation = dp(8f).toFloat()
            minimumWidth = dp(150f)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = TextView(this).apply {
            text = "庭"; setTextColor(Color.WHITE); textSize = 12f; gravity = Gravity.CENTER
            background = GradientDrawable().apply { cornerRadius = dp(6f).toFloat(); setColor(Color.parseColor("#2F6FDB")) }
            layoutParams = LinearLayout.LayoutParams(dp(20f), dp(20f)).apply { rightMargin = dp(8f) }
        }
        tvWord = TextView(this).apply {
            setTextColor(Color.parseColor("#1E3A6E")); textSize = 18f; setTypeface(typeface, Typeface.BOLD)
            maxWidth = dp(220f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        fun btn(label: String, onClick: () -> Unit) = TextView(this).apply {
            text = label; textSize = 15f; setTextColor(Color.parseColor("#2F6FDB")); gravity = Gravity.CENTER
            setPadding(dp(8f), dp(2f), dp(8f), dp(2f))
            setOnClickListener { onClick() }
        }
        top.addView(logo); top.addView(tvWord)
        top.addView(btn("›") { nextWord(); handler.removeCallbacks(tick); handler.postDelayed(tick, intervalMs()) })
        top.addView(btn("✕") { WordNotifier.prefs(this).edit().putBoolean("floatOn", false).apply(); stopSelf() })
        tvRead = TextView(this).apply { setTextColor(Color.parseColor("#6B7A93")); textSize = 13f; maxWidth = dp(260f) }
        tvMean = TextView(this).apply {
            setTextColor(Color.parseColor("#2F6FDB")); textSize = 15f; setTypeface(typeface, Typeface.BOLD)
            maxWidth = dp(260f); visibility = View.GONE; setPadding(0, dp(4f), 0, 0)
        }
        card.addView(top); card.addView(tvRead); card.addView(tvMean)

        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val p = WordNotifier.prefs(this@FloatService)
            x = p.getInt("floatX", dp(16f)); y = p.getInt("floatY", dp(140f))
        }

        /* Kéo để di chuyển, chạm để hiện / ẩn nghĩa */
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        card.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (abs(dx) > dp(6f) || abs(dy) > dp(6f)) moved = true
                    if (moved) { lp.x = startX + dx.toInt(); lp.y = startY + dy.toInt(); try { wm.updateViewLayout(card, lp) } catch (_: Exception) {} }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) WordNotifier.prefs(this).edit().putInt("floatX", lp.x).putInt("floatY", lp.y).apply()
                    else { expanded = !expanded; tvMean.visibility = if (expanded) View.VISIBLE else View.GONE }
                    true
                }
                else -> false
            }
        }
        root = card
        try { wm.addView(card, lp) } catch (e: Exception) { stopSelf() }
    }

    private fun nextWord() {
        val p = WordNotifier.prefs(this)
        val arr = try { JSONArray(p.getString("words", "[]")) } catch (e: Exception) { JSONArray() }
        if (!::tvWord.isInitialized) return
        if (arr.length() == 0) { tvWord.text = "Chưa có từ vựng"; tvRead.text = "Mở Niwa để thêm thẻ"; tvMean.text = ""; return }
        var i = (0 until arr.length()).random()
        val last = p.getInt("floatLast", -1)
        if (arr.length() > 1 && i == last) i = (i + 1) % arr.length()
        p.edit().putInt("floatLast", i).apply()
        val w = arr.optJSONObject(i) ?: return
        tvWord.text = w.optString("f")
        val r = w.optString("r")
        tvRead.text = r; tvRead.visibility = if (r.isEmpty()) View.GONE else View.VISIBLE
        tvMean.text = w.optString("m")
        expanded = false; tvMean.visibility = View.GONE
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null
        super.onDestroy()
    }
}
