package ru.proverka29.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var offlinePanel: View
    private lateinit var offlineMessage: TextView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var cameraUri: Uri? = null
    private var cameraFile: File? = null
    private var pendingWebPermission: PermissionRequest? = null
    private var requestedWebResources: Array<String> = emptyArray()
    private var pageFailed = false

    companion object {
        private const val FILE_CHOOSER_REQUEST = 4102
        private const val WEB_PERMISSION_REQUEST = 4103
        private const val NOTIFICATION_REQUEST = 4104
        private const val HOME_URL = "https://proverka29.ru"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        swipe = findViewById(R.id.swipeRefresh)
        progress = findViewById(R.id.pageProgress)
        offlinePanel = findViewById(R.id.offlinePanel)
        offlineMessage = findViewById(R.id.offlineMessage)
        findViewById<Button>(R.id.menuButton).setOnClickListener { showMenu(it) }
        findViewById<Button>(R.id.retryButton).setOnClickListener { loadHome() }

        // Настройки WebView: JS, localStorage и постоянные cookies для сессии.
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // Разрешаем чтение только доверенного HTML из assets приложения.
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "http" || uri.scheme == "https") return false
                openExternal(uri)
                return true
            }

            @Deprecated("Совместимость со старыми версиями Android")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val uri = Uri.parse(url)
                if (uri.scheme == "http" || uri.scheme == "https") return false
                openExternal(uri)
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                pageFailed = false
            }

            override fun onPageFinished(view: WebView, url: String) {
                swipe.isRefreshing = false
                if (!pageFailed && offlinePanel.visibility == View.VISIBLE) showWeb()
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    pageFailed = true
                    showOffline("Ошибка загрузки: ${error.errorCode} — ${error.description}")
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                openFilePicker()
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val needed = mutableListOf<String>()
                    if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.CAMERA)
                    }
                    if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (needed.isEmpty()) request.grant(request.resources) else {
                        pendingWebPermission = request
                        requestedWebResources = request.resources
                        requestPermissions(needed.toTypedArray(), WEB_PERMISSION_REQUEST)
                    }
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (pendingWebPermission == request) pendingWebPermission = null
            }
        }

        swipe.setColorSchemeColors(Color.rgb(13, 76, 211))
        swipe.setOnRefreshListener {
            if (isOnline()) webView.reload() else {
                swipe.isRefreshing = false
                showOffline("Нет подключения к интернету")
            }
        }
        loadHome()
        requestNotificationPermissionOnce()
    }

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_home -> webView.loadUrl("$HOME_URL/index.php")
                R.id.menu_request -> webView.loadUrl("$HOME_URL/zayavka.php")
                R.id.menu_account -> webView.loadUrl("$HOME_URL/account.php")
                R.id.menu_staff -> webView.loadUrl("$HOME_URL/staff.php")
                R.id.menu_report -> webView.loadUrl("$HOME_URL/report.php")
                R.id.menu_widget -> webView.loadUrl("file:///android_asset/widget.html")
                R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun loadHome() {
        if (isOnline()) { pageFailed = false; showWeb(); webView.loadUrl(HOME_URL) }
        else showOffline("Нет подключения к интернету")
    }

    private fun isOnline(): Boolean {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showOffline(message: String) {
        offlineMessage.text = message
        offlinePanel.visibility = View.VISIBLE
        webView.visibility = View.GONE
        swipe.isRefreshing = false
    }

    private fun showWeb() {
        offlinePanel.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }

    private fun openExternal(uri: Uri) {
        try {
            val intent = if (uri.scheme == "intent") Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                else Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Не найдено приложение для этой ссылки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFilePicker() {
        val picker = Intent(Intent.ACTION_GET_CONTENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }
        val chooser = Intent.createChooser(picker, "Выберите файл")
        try {
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            dir.mkdirs()
            cameraFile = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            cameraUri = FileProvider.getUriForFile(this, "ru.proverka29.app.fileprovider", cameraFile!!)
            val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
        } catch (_: Exception) { /* Если камера недоступна, остаётся выбор файла. */ }
        try { startActivityForResult(chooser, FILE_CHOOSER_REQUEST) }
        catch (_: Exception) { fileCallback?.onReceiveValue(null); fileCallback = null }
    }

    @Deprecated("Результат системного выбора файла")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val result = when {
                resultCode != Activity.RESULT_OK -> null
                data?.data != null -> arrayOf(data.data!!)
                cameraUri != null && cameraFile?.exists() == true -> arrayOf(cameraUri!!)
                else -> null
            }
            fileCallback?.onReceiveValue(result)
            fileCallback = null
            cameraUri = null
            cameraFile = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WEB_PERMISSION_REQUEST) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (granted) pendingWebPermission?.grant(requestedWebResources) else pendingWebPermission?.deny()
            pendingWebPermission = null
        }
        if (requestCode == NOTIFICATION_REQUEST) {
            getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("notification_prompted", true).apply()
        }
    }

    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT >= 33) {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            if (!prefs.getBoolean("notification_prompted", false) &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST)
            } else prefs.edit().putBoolean("notification_prompted", true).apply()
        }
    }

    @Deprecated("Обработка системной кнопки назад")
    override fun onBackPressed() {
        if (offlinePanel.visibility == View.VISIBLE) {
            if (webView.canGoBack()) { showWeb(); webView.goBack() } else super.onBackPressed()
        } else if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        webView.destroy()
        super.onDestroy()
    }
}
