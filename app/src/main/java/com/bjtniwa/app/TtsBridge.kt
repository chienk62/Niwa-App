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
        tts.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.SUCCESS
    }

    @JavascriptInterface
    fun stop() { if (ready) tts.stop() }

    fun shutdown() { tts.stop(); tts.shutdown() }
}
