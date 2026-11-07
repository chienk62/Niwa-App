package com.bjtniwa.app

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.util.Locale

/**
 * WebView trên Android không có giọng đọc của trình duyệt (speechSynthesis),
 * nên app nối nút loa trong trang với bộ đọc văn bản (Text-to-Speech) của Android.
 */
class TtsBridge(context: Context, private val web: WebView) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) = done(id)
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) = done(id)
            override fun onError(id: String?, errorCode: Int) = done(id)
        })
    }

    private fun done(id: String?) {
        val safe = (id ?: "").replace(Regex("[^A-Za-z0-9_-]"), "")
        web.post { web.evaluateJavascript("window.__ttsDone && window.__ttsDone('$safe')", null) }
    }

    @JavascriptInterface
    fun speak(text: String, lang: String, rate: Float, id: String): Boolean {
        if (!ready) return false
        val r = tts.setLanguage(Locale.forLanguageTag(lang))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        tts.setSpeechRate(rate.coerceIn(0.3f, 2f))
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.SUCCESS
    }

    @JavascriptInterface
    fun stop() { if (ready) tts.stop() }

    /** Danh sách giọng tiếng Nhật và tiếng Anh có trên máy, dạng JSON */
    @JavascriptInterface
    fun voices(): String {
        val arr = org.json.JSONArray()
        if (!ready) return arr.toString()
        try {
            tts.voices?.filter { v ->
                val tag = v.locale.toLanguageTag()
                tag.startsWith("ja") || tag.startsWith("en")
            }?.sortedBy { it.locale.toLanguageTag() + it.name }?.forEach { v ->
                val o = org.json.JSONObject()
                o.put("name", v.name)
                o.put("lang", v.locale.toLanguageTag())
                o.put("network", v.isNetworkConnectionRequired)
                arr.put(o)
            }
        } catch (e: Exception) { }
        return arr.toString()
    }

    /** Đọc bằng một giọng cụ thể; không tìm thấy giọng thì dùng giọng mặc định của ngôn ngữ */
    @JavascriptInterface
    fun speakVoice(text: String, lang: String, rate: Float, id: String, voiceName: String): Boolean {
        if (!ready) return false
        val v = try { tts.voices?.firstOrNull { it.name == voiceName } } catch (e: Exception) { null }
        if (v != null) {
            tts.voice = v
        } else {
            val r = tts.setLanguage(Locale.forLanguageTag(lang))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        }
        tts.setSpeechRate(rate.coerceIn(0.3f, 2f))
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.SUCCESS
    }

    fun shutdown() { tts.stop(); tts.shutdown() }
}
