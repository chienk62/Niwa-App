package com.bjtniwa.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var tts: TtsBridge
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    /* Mở trình chọn file cho ô "Chọn file Excel" trong trang */
    private val pickFiles = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val cb = fileCallback
        fileCallback = null
        cb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data))
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        /* Trang được phục vụ từ thư mục assets qua một địa chỉ https nội bộ,
           nhờ vậy dữ liệu (localStorage) của trang được giữ lại giữa các lần mở app. */
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            /* Cỡ chữ hệ thống quá lớn (một số máy Realme/Oppo) làm vỡ bố cục: giới hạn tối đa 110% */
            textZoom = textZoom.coerceAtMost(110)
        }

        tts = TtsBridge(this, web)
        web.addJavascriptInterface(tts, "AndroidTTS")

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.host == "appassets.androidplatform.net") return false
                /* Liên kết ra ngoài mở bằng trình duyệt */
                return try { startActivity(Intent(Intent.ACTION_VIEW, url)); true } catch (e: Exception) { true }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val accept = params.acceptTypes.flatMap { it.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }
                val mimes = accept.filter { it.contains("/") }
                val restrict = mimes.isNotEmpty() && accept.none { it.startsWith(".") }
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = if (restrict && mimes.size == 1) mimes[0] else "*/*"
                    if (restrict && mimes.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, mimes.toTypedArray())
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                }
                return try {
                    pickFiles.launch(Intent.createChooser(intent, null)); true
                } catch (e: Exception) {
                    fileCallback = null; false
                }
            }
        }

        /* Nút Back: đóng cửa sổ đang mở trong trang trước, nếu không có thì thoát app */
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript(
                    "(function(){var m=document.getElementById('modal-root');" +
                        "if(m&&m.innerHTML&&typeof closeModal==='function'){closeModal();return 1}return 0})()"
                ) { r ->
                    if (r != "1") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

        if (savedInstanceState == null) {
            web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        } else {
            web.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        tts.shutdown()
        web.destroy()
        super.onDestroy()
    }
}
