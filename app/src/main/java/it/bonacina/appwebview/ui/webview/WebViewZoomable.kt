package it.bonacina.appwebview.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.*
import android.view.ViewGroup.LayoutParams
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.RelativeLayout
import it.bonacina.appwebview.R
import it.bonacina.appwebview.utils.Utility
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

import android.view.MotionEvent


class WebViewZoomable: WebView, ZoomStatusListener, View.OnLayoutChangeListener {

    private var lastScale = 0f

    private var mFooter: WebViewInjectedView? = null
    private var mHeader: WebViewInjectedView? = null
    private var currentTouchSlop by Delegates.notNull<Int>()

    private var loadHtml: Runnable? = null
    private var awaitHeaderPost: Runnable? = null

    private var followZoomScale: Boolean = false

    private val internalViewsTouchListener: MutableMap<Int, WebViewTouchListener> = mutableMapOf()

    internal abstract class ViewHeightListener : OnLayoutChangeListener {
        abstract fun onNewHeight(newHeight: Int)

        override fun onLayoutChange(
            v: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            val newHeight = bottom - top
            onNewHeight(newHeight)
        }

    }

    internal class HeaderHeightListener(private val webView: WebViewZoomable): ViewHeightListener() {
        override fun onNewHeight(newHeight: Int) {
            webView.injectTopPadding(newHeight)
        }
    }

    internal class FooterHeightListener(private val webView: WebViewZoomable): ViewHeightListener() {
        override fun onNewHeight(newHeight: Int) {
            webView.injectBottomPadding(newHeight)
        }
    }

    private val headerHeightListener = HeaderHeightListener(this)
    private val footerHeightListener = FooterHeightListener(this)

    constructor(context: Context) : super(context) {
        initialSetup(null)
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialSetup(attrs)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialSetup(attrs)
    }

    fun getHeaderView(): WebViewInjectedView? {
        return mHeader
    }

    fun getHeaderInternalView(): View? {
        return mHeader?.getChildAt(0)
    }

    fun setHeader(view: View) {
        val myHeaderView = WebViewInjectedView(context)
        myHeaderView.addView(view, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        this.addView(myHeaderView, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
        ))
        setEmbeddedHeader(myHeaderView)
    }

    fun getFooterView(): WebViewInjectedView? {
        return mFooter
    }

    fun getFooterInternalView(): View? {
        return mFooter?.getChildAt(0)
    }

    fun setFooter(view: View) {
        val myFooterView = WebViewInjectedView(context)
        myFooterView.addView(view, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        this.addView(myFooterView, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
        ))
        myFooterView.calculateCorrectHeaderHeight()
        setEmbeddedFooter(myFooterView)
    }

    fun setTouchSlop(slop: Int) {
        currentTouchSlop = slop
    }

    fun toggleHideContent(onChangeContentVisibility: (Boolean) -> Unit) {
        val script = """
            (function() {
                var isHide = false
                var htmlElements = document.getElementsByTagName("html")
                if (htmlElements.length > 0) {
                    var html = htmlElements[0]
                    var currentStyle = html.getAttribute('style');
                    if (currentStyle == null) 
                        currentStyle = [];
                    else 
                        currentStyle = currentStyle.split(';').filter(prop => prop != null && prop !== "");
                    if (currentStyle.length > 0) {
                        var lastProp = currentStyle[currentStyle.length - 1];
                        if (lastProp == "display: none !important") {
                            currentStyle.pop();
                            isHide = false
                        } else {
                            currentStyle.push("display: none !important");
                            isHide = true
                        }
                    } else {
                        currentStyle.push("display: none !important");
                        isHide = true
                    }
                    var finalStyle = currentStyle.join(';');
                    if (currentStyle.length > 0)
                        finalStyle += ";";
                    html.setAttribute('style', finalStyle);
                }
                return isHide
            })();
        """.trimIndent()
        evaluateJavascript(
            script
        ) { returnValue ->
            onChangeContentVisibility(returnValue.toBoolean())
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initialSetup(attrs: AttributeSet?) {
        currentTouchSlop = ViewConfiguration.get(context).scaledTouchSlop/2

        this.scrollBarStyle = SCROLLBARS_OUTSIDE_OVERLAY
        this.isScrollbarFadingEnabled = true
        this.isLongClickable = true

        val webSettings = this.settings

        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.useWideViewPort = true
        setInitialScale(1)

        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING

        webSettings.loadWithOverviewMode = true

        disableDisplayZoomControls()

        webSettings.javaScriptEnabled = true
        webSettings.loadsImagesAutomatically = true
        webSettings.blockNetworkLoads = false
        webSettings.domStorageEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS

        lastScale = resources.displayMetrics.density

        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.WebViewZoomable,
                0, 0
            )
            try {
                val headerView = a.getResourceId(R.styleable.WebViewZoomable_header_view, 0)
                if (headerView != 0) {
                    val customHeaderView = View.inflate(context, headerView, null)
                    setHeader(customHeaderView)
                }

                val footerView = a.getResourceId(R.styleable.WebViewZoomable_footer_view, 0)
                if (footerView != 0) {
                    val customFooterView = View.inflate(context, footerView, null)
                    setFooter(customFooterView)
                }

                followZoomScale = a.getBoolean(R.styleable.WebViewZoomable_follow_zoom_scale, false)
            } finally {
                a.recycle()
            }
        }

        addOnLayoutChangeListener(this)
    }

    /**
     * Disable on-screen zoom controls on devices that support zooming via pinch-to-zoom.
     */
    private fun disableDisplayZoomControls() {
        val pm = context.packageManager
        val supportsMultiTouch =
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) ||
                pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT)
        settings.displayZoomControls = !supportsMultiTouch
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        mFooter?.apply {
            if (this.height != 0) {
                this.translationY = (computeVerticalScrollRange() - getFooterHeight()).toFloat()
            }
        }
    }

    fun displayHtmlContent(
        webViewClient: WebViewZoomableClient,
        url: String,
        htmlText: String
    ) {
        if (followZoomScale)
            webViewClient.setOnZoomStatusChangeListener(this)
        setWebViewClient(webViewClient)
        setHtmlContent(url, htmlText)
    }

    private fun setHtmlContent(url: String, htmlText: String) {
        loadHtml = Runnable {
            loadDataWithBaseURL(
                url,
                injectPaddingToHtml(htmlText)!!,
                "text/html",
                "utf-8",
                null
            )
            resumeTimers()
        }
        awaitHeaderPost = Runnable {
            mFooter?.post(loadHtml) ?: loadHtml?.run()
        }
        mHeader?.post(awaitHeaderPost) ?: kotlin.run { loadHtml?.run() }
    }

    private fun injectPaddingToHtml(htmlText: String?): String? {
        if (htmlText == null || htmlText.isEmpty()) return htmlText
        val titleHeight = getTitleHeight()
        val footerHeight = getFooterHeight()
        val paddingT: Float = Utility.convertPixelsToPx(
            titleHeight,
            lastScale
        )
        val paddingB: Float = Utility.convertPixelsToPx(
            footerHeight,
            lastScale
        )
        val paddingTop = "padding-top:" + paddingT + "px !important"
        val paddingBottom = "padding-bottom:" + paddingB + "px !important"
        val document: Document = Jsoup.parse(htmlText)
        document.body().attr("style", "$paddingTop;$paddingBottom; height: auto !important;")

        Timber.d("Top padding in dp: %s", paddingT)
        Timber.d("Bottom padding in dp: %s", paddingB )
        return document.toString()
    }

    private fun injectTopPadding(pixel: Int) {
        val dp: Float = Utility.convertPixelsToPx(pixel, lastScale)
        Timber.d("Top padding in dp: %s", dp)
        evaluateJavascript(
            "document.body.style.setProperty('padding-top','" + dp + "px', 'important');",
            null
        )
    }

    private fun injectBottomPadding(pixel: Int) {
        val dp: Float = Utility.convertPixelsToPx(pixel, lastScale)
        Timber.d("Bottom padding in dp: %s", dp)
        evaluateJavascript(
            "document.body.style.setProperty('padding-bottom','" + dp + "px', 'important');",
            null
        )
    }

    private fun setEmbeddedHeader(v: WebViewInjectedView) {
        if (mHeader === v) return
        mHeader = v
        mHeader?.removeOnLayoutChangeListener(headerHeightListener)
        mHeader?.addOnLayoutChangeListener(headerHeightListener)
    }

    private fun setEmbeddedFooter(v: WebViewInjectedView) {
        if (mFooter === v) return
        mFooter = v
        mFooter?.removeOnLayoutChangeListener(footerHeightListener)
        mFooter?.addOnLayoutChangeListener(footerHeightListener)
    }

    override fun onZoomStatusChanged(oldScale: Float, newScale: Float) {
        lastScale = newScale
        if (mHeader != null) {
            val dp: Float = Utility.convertPixelsToPx(
                getTitleHeight(),
                newScale
            )
            evaluateJavascript(
                "document.body.style.setProperty('padding-top','" + dp + "px', 'important');",
                null
            )
        }
        if (mFooter != null) {
            val dp: Float = Utility.convertPixelsToPx(
                getFooterHeight(),
                newScale
            )
            evaluateJavascript(
                "document.body.style.setProperty('padding-bottom','" + dp + "px', 'important');",
                null
            )
        }
    }

    private fun getTitleHeight(): Int {
        return mHeader?.height ?: 0
    }

    private fun getFooterHeight(): Int {
        return mFooter?.height ?: 0
    }

    fun addInternalViewTouchListener(viewIds: List<Int>, listener: WebViewTouchListener) {
        viewIds.forEach {
            internalViewsTouchListener[it] = listener
        }
    }

    private var startEvent: MotionEvent? = null
    private var lastRequestDisallow: Boolean? = null

    @SuppressLint("Recycle")
    private fun obtainScrollParameters(event: MotionEvent) {
        startEvent = MotionEvent.obtainNoHistory(event)
    }

    private fun recycleScrollParameters() {
        startEvent?.recycle()
        startEvent = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        handleMotionEvent(event)

        internalViewsTouchListener.forEach {
            findViewById<View>(it.key)?.let { view ->
                it.value.onTouchEvent(view, event)
            }
        }

        return super.onTouchEvent(event)
    }

    private fun handleMotionEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                obtainScrollParameters(event)
                if (canScrollHorizontally(1) || canScrollHorizontally(-1) || event.pointerCount > 1)
                    parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (canScrollHorizontally(1) || canScrollHorizontally(-1) || event.pointerCount > 1)
                    parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                lastRequestDisallow = null
                recycleScrollParameters()
            }
            MotionEvent.ACTION_MOVE -> {
                startEvent?.let { e1 ->
                    val dX = e1.x - event.x
                    val dY = e1.y - event.y

                    if (dX.absoluteValue > currentTouchSlop) {
                        if (dX.absoluteValue > dY.absoluteValue) {
                            val horizontalDirection = if (e1.x - event.x > 0) 1 else -1
                            //Timber.d("Horizontal direction: %s", horizontalDirection);
                            val canScrollHorizontally: Boolean =
                                canScrollHorizontally(horizontalDirection)

                            if (lastRequestDisallow == null) {
                                lastRequestDisallow = canScrollHorizontally
                            }

                            val sameScroll = lastRequestDisallow == true
                            parent.requestDisallowInterceptTouchEvent(sameScroll || canScrollHorizontally || event.pointerCount > 1)
                        } else {
                            if (lastRequestDisallow == null) {
                                lastRequestDisallow = true
                            }
                            parent.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }
            }
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        canvas.save()
        if (child === mHeader) {
            mHeader?.apply {
                offsetLeftAndRight(this@WebViewZoomable.scrollX - this.left)
            }
        }
        if (child === mFooter) {
            mFooter?.apply {
                offsetLeftAndRight(this@WebViewZoomable.scrollX - this.left)
                this.translationY = (computeVerticalScrollRange() - getFooterHeight()).toFloat()
            }
        }
        val result = super.drawChild(canvas, child, drawingTime)
        canvas.restore()
        return result
    }
}