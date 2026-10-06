package com.example.car

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.data.CarStreamRepository
import com.google.android.apps.auto.sdk.CarActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real Android Auto Head-Unit Projection Activity (`CarActivity` from `aauto-sdk`).
 * Replaces the gray MediaBrowser template (Photo 1) with the full YouTube & Video interface
 * on the car screen (Photo 2), enhanced with modern toolbar controls, aspect ratio zoom,
 * quick channel bar, and in-car search/keyboard input.
 */
class MainCarActivity : CarActivity() {

    private var webView: WebView? = null
    private var topToolbar: LinearLayout? = null
    private var bookmarksStrip: HorizontalScrollView? = null
    private var searchBarContainer: LinearLayout? = null
    private var searchEditText: EditText? = null
    private var progressBar: ProgressBar? = null
    private var fullscreenContainer: FrameLayout? = null
    private var clockTextView: TextView? = null
    private var aspectButtonView: TextView? = null
    private var fullscreenToggleBtn: TextView? = null

    private var customFullscreenView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var isFullscreenCarMode = false
    private var currentZoomScale = 1.0f
    private var currentAspectIndex = 0 // 0 = Fit, 1 = 21:9 Cover, 2 = Fill
    private val clockHandler = Handler(Looper.getMainLooper())

    private val clockRunnable = object : Runnable {
        override fun run() {
            val fmt = SimpleDateFormat("HH:mm น.", Locale.getDefault())
            clockTextView?.text = fmt.format(Date())
            clockHandler.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        // Hide default Android Auto header to maximize YouTube screen area
        runCatching {
            val uiController = carUiController
            uiController?.statusBarController?.hideAppHeader()
            uiController?.statusBarController?.hideConnectivityLevel()
        }

        val ctx = getLayoutInflater().context
        setContentView(buildCarProjectionView(ctx))
        clockHandler.post(clockRunnable)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildCarProjectionView(ctx: Context): View {
        val rootFrame = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0A0D14"))
        }

        val mainColumn = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. Upgraded CarStream Top Toolbar (Matches & Upgrades Photo 2: Back, Home, Refresh, Zoom-, Zoom+, Aspect/Fullscreen, Channels, Clock)
        val toolbar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#161C28"))
            setPadding(dp(ctx, 8), dp(ctx, 4), dp(ctx, 10), dp(ctx, 4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(ctx, 46)
            )
        }
        topToolbar = toolbar

        // Back Button (←)
        toolbar.addView(
            createToolbarButton(ctx, "←") {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                }
            }
        )

        // Home Button (⌂)
        toolbar.addView(
            createToolbarButton(ctx, "⌂") {
                webView?.loadUrl("https://m.youtube.com")
            }
        )

        // Refresh Button (↻)
        toolbar.addView(
            createToolbarButton(ctx, "↻") {
                webView?.reload()
            }
        )

        // Zoom Out (🔍-)
        toolbar.addView(
            createToolbarButton(ctx, "🔍-") {
                currentZoomScale = (currentZoomScale - 0.08f).coerceAtLeast(0.80f)
                applyWebZoomAndAspect()
            }
        )

        // Zoom In (🔍+)
        toolbar.addView(
            createToolbarButton(ctx, "🔍+") {
                currentZoomScale = (currentZoomScale + 0.08f).coerceAtMost(1.40f)
                applyWebZoomAndAspect()
            }
        )

        // Aspect Ratio & Fullscreen Highlight Button (Matches blue highlighted icon in Photo 2)
        val aspectBtn = createToolbarButton(
            ctx = ctx,
            label = "⛶ FIT",
            highlighted = true
        ) {
            currentAspectIndex = (currentAspectIndex + 1) % 3
            val modeText = when (currentAspectIndex) {
                1 -> "⛶ 21:9"
                2 -> "⛶ FILL"
                else -> "⛶ FIT"
            }
            aspectButtonView?.text = modeText
            applyWebZoomAndAspect()
        }
        aspectButtonView = aspectBtn
        toolbar.addView(aspectBtn)

        // Quick Search / Keyboard Toggle (⌨)
        toolbar.addView(
            createToolbarButton(ctx, "⌨ ค้นหา") {
                val visible = searchBarContainer?.visibility == View.VISIBLE
                searchBarContainer?.visibility = if (visible) View.GONE else View.VISIBLE
                if (!visible) {
                    searchEditText?.requestFocus()
                }
            }
        )

        // Quick Channels Grid Button (⊞)
        toolbar.addView(
            createToolbarButton(ctx, "⊞ ช่อง") {
                val visible = bookmarksStrip?.visibility == View.VISIBLE
                bookmarksStrip?.visibility = if (visible) View.GONE else View.VISIBLE
            }
        )

        // True Fullscreen Hide-Bar Toggle
        val fsBtn = createToolbarButton(ctx, "เต็มจอ") {
            toggleCarFullscreen(true)
        }
        fullscreenToggleBtn = fsBtn
        toolbar.addView(fsBtn)

        // Right-aligned Clock (e.g., 21:06 น. like Photo 2)
        val clockTv = TextView(ctx).apply {
            text = "21:06 น."
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 8), 0, dp(ctx, 4), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        clockTextView = clockTv
        toolbar.addView(clockTv)

        mainColumn.addView(toolbar)

        // 2. Collapsible In-Car Search & "@" Quick-Input Bar (Fixes missing @ and cleared input field bug)
        val searchRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#111722"))
            setPadding(dp(ctx, 8), dp(ctx, 4), dp(ctx, 8), dp(ctx, 4))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        searchBarContainer = searchRow

        val inputField = EditText(ctx).apply {
            hint = "ค้นหา YouTube หรือวางลิงก์..."
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = dp(ctx, 8).toFloat()
            }
            setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6))
            layoutParams = LinearLayout.LayoutParams(0, dp(ctx, 38), 1f).apply {
                marginEnd = dp(ctx, 6)
            }
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    val query = v.text.toString()
                    webView?.loadUrl(CarStreamRepository.normalizeUrlOrSearch(query))
                    searchBarContainer?.visibility = View.GONE
                    hideKeyboard(ctx, v)
                    true
                } else {
                    false
                }
            }
        }
        searchEditText = inputField
        searchRow.addView(inputField)

        // Quick "@" and ".com" buttons for Car login
        listOf("@", ".com").forEach { token ->
            searchRow.addView(
                createSmallPillButton(ctx, token, "#00E5FF") {
                    inputField.append(token)
                }
            )
        }

        // Go button
        searchRow.addView(
            createSmallPillButton(ctx, "ค้นหา", "#FF2A54") {
                val query = inputField.text.toString()
                webView?.loadUrl(CarStreamRepository.normalizeUrlOrSearch(query))
                searchBarContainer?.visibility = View.GONE
                hideKeyboard(ctx, inputField)
            }
        )

        mainColumn.addView(searchRow)

        // 3. Collapsible Quick Channels Strip (YouTube, Music, Plex, Live)
        val channelsScroll = HorizontalScrollView(ctx).apply {
            setBackgroundColor(Color.parseColor("#111722"))
            isHorizontalScrollBarEnabled = false
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        bookmarksStrip = channelsScroll

        val channelsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(ctx, 8), dp(ctx, 4), dp(ctx, 8), dp(ctx, 4))
        }
        val quickChannels = listOf(
            "YouTube Home" to "https://m.youtube.com",
            "YouTube Music" to "https://music.youtube.com",
            "Plex TV" to "https://app.plex.tv/desktop",
            "Lofi Beats Live" to "https://www.youtube.com/embed/jfKfPfyJRdk?autoplay=1",
            "NASA Live" to "https://www.youtube.com/embed/21X5lGlDOfg?autoplay=1",
            "Google" to "https://www.google.com"
        )
        quickChannels.forEach { (title, url) ->
            channelsRow.addView(
                createSmallPillButton(ctx, title, "#1E293B") {
                    webView?.loadUrl(url)
                    bookmarksStrip?.visibility = View.GONE
                }
            )
        }
        channelsScroll.addView(channelsRow)
        mainColumn.addView(channelsScroll)

        // 4. Thin Loading Bar
        val prog = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(ctx, 3)
            )
        }
        progressBar = prog
        mainColumn.addView(prog)

        // 5. Fullscreen YouTube WebView Area
        val wv = WebView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(Color.BLACK)

            runCatching {
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false
                textZoom = 100
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressBar?.progress = newProgress
                    progressBar?.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null) return
                    customFullscreenView = view
                    customViewCallback = callback
                    fullscreenContainer?.removeAllViews()
                    fullscreenContainer?.addView(
                        view,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    fullscreenContainer?.visibility = View.VISIBLE
                    topToolbar?.visibility = View.GONE
                    isFullscreenCarMode = true
                }

                override fun onHideCustomView() {
                    fullscreenContainer?.removeAllViews()
                    fullscreenContainer?.visibility = View.GONE
                    customFullscreenView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                    topToolbar?.visibility = View.VISIBLE
                    isFullscreenCarMode = false
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    progressBar?.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar?.visibility = View.GONE
                    applyWebZoomAndAspect()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val reqUrl = request?.url?.toString() ?: return false
                    return !(reqUrl.startsWith("http://") || reqUrl.startsWith("https://"))
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    return true
                }
            }

            loadUrl("https://m.youtube.com")
        }
        webView = wv
        mainColumn.addView(wv)
        rootFrame.addView(mainColumn)

        // 6. Native HTML5 Fullscreen Container
        val fsContainer = FrameLayout(ctx).apply {
            visibility = View.GONE
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        fullscreenContainer = fsContainer
        rootFrame.addView(fsContainer)

        // 7. Floating Exit-Fullscreen Pill (Visible only when top toolbar is hidden in Fullscreen mode)
        val exitFsFloatingBtn = TextView(ctx).apply {
            text = "ออกจากเต็มจอ"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(ctx, 10), dp(ctx, 5), dp(ctx, 10), dp(ctx, 5))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC0A0D14"))
                setStroke(dp(ctx, 1), Color.parseColor("#00E5FF"))
                cornerRadius = dp(ctx, 14).toFloat()
            }
            visibility = View.GONE
            setOnClickListener {
                toggleCarFullscreen(false)
                it.visibility = View.GONE
            }
        }
        val floatingParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = dp(ctx, 8)
            rightMargin = dp(ctx, 8)
        }
        rootFrame.addView(exitFsFloatingBtn, floatingParams)

        fullscreenToggleBtn?.setOnClickListener {
            toggleCarFullscreen(true)
            exitFsFloatingBtn.visibility = View.VISIBLE
        }

        return rootFrame
    }

    private fun toggleCarFullscreen(fullscreen: Boolean) {
        isFullscreenCarMode = fullscreen
        if (fullscreen) {
            topToolbar?.visibility = View.GONE
            searchBarContainer?.visibility = View.GONE
            bookmarksStrip?.visibility = View.GONE
        } else {
            if (customFullscreenView != null) {
                fullscreenContainer?.removeAllViews()
                fullscreenContainer?.visibility = View.GONE
                customFullscreenView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }
            topToolbar?.visibility = View.VISIBLE
        }
    }

    private fun applyWebZoomAndAspect() {
        val objectFitRule = when (currentAspectIndex) {
            1 -> "object-fit: cover !important; transform: scale(${currentZoomScale * 1.12f}, ${currentZoomScale * 1.02f}) !important;"
            2 -> "object-fit: fill !important; transform: scale($currentZoomScale) !important;"
            else -> "object-fit: contain !important; transform: scale($currentZoomScale) !important;"
        }
        val js = """
            (function() {
                try {
                    document.body.style.zoom = '$currentZoomScale';
                    var s = document.getElementById('carstream-car-fit');
                    if (!s) {
                        s = document.createElement('style');
                        s.id = 'carstream-car-fit';
                        document.head.appendChild(s);
                    }
                    s.innerHTML = `
                        ytm-promoted-sparkles-web-renderer,
                        ytm-companion-ad-renderer,
                        .ad-showing .video-ads,
                        .ytp-ad-overlay-container,
                        ytm-mealbar-promo-renderer { display: none !important; }
                        video { width: 100% !important; height: 100% !important; $objectFitRule }
                        *:focus { outline: 3px solid #00E5FF !important; }
                    `;
                } catch(e) {}
            })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
    }

    private fun createToolbarButton(
        ctx: Context,
        label: String,
        highlighted: Boolean = false,
        onClick: () -> Unit
    ): TextView {
        return TextView(ctx).apply {
            text = label
            setTextColor(if (highlighted) Color.parseColor("#0A0D14") else Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), dp(ctx, 6))
            background = GradientDrawable().apply {
                setColor(
                    if (highlighted) Color.parseColor("#7CC4E8") else Color.parseColor("#232E42")
                )
                cornerRadius = dp(ctx, 8).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                0,
                dp(ctx, 36),
                1f
            ).apply {
                marginEnd = dp(ctx, 6)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createSmallPillButton(
        ctx: Context,
        label: String,
        bgHex: String,
        onClick: () -> Unit
    ): TextView {
        return TextView(ctx).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), dp(ctx, 6))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = dp(ctx, 8).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(ctx, 36)
            ).apply {
                marginEnd = dp(ctx, 6)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun hideKeyboard(ctx: Context, view: View) {
        runCatching {
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun dp(ctx: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            ctx.resources.displayMetrics
        ).toInt()
    }

    override fun onStop() {
        clockHandler.removeCallbacks(clockRunnable)
        super.onStop()
    }
}
