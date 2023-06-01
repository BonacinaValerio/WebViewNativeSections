/*
 * Copyright 2023 Valerio Bonacina. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.bonacina.webviewnativesections

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.*
import android.view.ViewGroup.LayoutParams
import android.view.animation.LinearInterpolator
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.RelativeLayout
import androidx.annotation.LayoutRes
import androidx.core.view.children
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import it.bonacina.webviewnativesections.*
import it.bonacina.webviewnativesections.domain.*
import it.bonacina.webviewnativesections.utils.Utility
import it.bonacina.webviewnativesections.view.WebViewInjectedView
import it.bonacina.webviewnativesections.view.webview.*
import it.bonacina.webviewzoomable.R
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.lang.Runnable
import java.lang.reflect.Type
import java.util.*
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

/**
 * A custom WebView that provides additional features for managing and displaying HTML content
 * with support for native header and footer views and efficient zooming.
 */
@Suppress( "DEPRECATION")
class WebViewNativeSections: WebView, PageListener, View.OnLayoutChangeListener,
    IWebViewNativeSections {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var lastScale = 0f
    private var currentTouchSlop by Delegates.notNull<Int>()
    private var currentAttr: AttributeSet? = null

    private var listener: WebViewListener? = null

    private val mHeaders: LinkedHashMap<String, WebViewInjectedView> = linkedMapOf()
    private val mFooters: LinkedHashMap<String, WebViewInjectedView> = linkedMapOf()
    private var currentTopWrappedSections: Set<String> = setOf()
    private var currentBottomWrappedSections: Set<String> = setOf()
    private var mGlobalHeader: WebViewInjectedView? = null
    private var mGlobalFooter: WebViewInjectedView? = null
    private val sectionHeaderHeightListeners: MutableMap<String?, NativeSectionHeightListener> = mutableMapOf()
    private val sectionFooterHeightListeners: MutableMap<String?, NativeSectionHeightListener> = mutableMapOf()
    private var sectionsDistanceHtml: Map<String, SectionDistanceHtml> = mapOf()

    private var loadHtml: Runnable? = null
    private var currentHtmlText: String? = null
    private var currentSections: MutableList<WebViewSection> = mutableListOf()
    private var sectionsReadyState: MutableMap<String, Boolean> = mutableMapOf()

    private var currentDocumentId: String? = null
    private var currentDocumentLoaded: Boolean = false

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

    internal class NativeSectionHeightListener(
        private val webView: WebViewNativeSections,
        private val sectionId: String?,
        private val sectionType: NativeSectionType
    ): ViewHeightListener() {
        override fun onNewHeight(newHeight: Int) {
            when (sectionType) {
                NativeSectionType.HEADER ->
                    webView.injectPaddingHeader(newHeight, sectionId)
                NativeSectionType.FOOTER -> {
                    webView.injectPaddingFooter(newHeight, sectionId)
                }
            }
        }
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialSetup(attrs)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initialSetup(attrs: AttributeSet?) {
        alpha = 0f
        currentTouchSlop = ViewConfiguration.get(context).scaledTouchSlop/2
        currentAttr = attrs

        this.scrollBarStyle = SCROLLBARS_OUTSIDE_OVERLAY
        this.isScrollbarFadingEnabled = true
        this.isLongClickable = true

        val webSettings = this.settings

        enableZoom(false)

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

        overScrollMode = OVER_SCROLL_NEVER

        addJavascriptInterface(this, "Android")

        lastScale = resources.displayMetrics.density

        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.WebViewNativeSections,
                0, 0
            )
            try {
                val headerViewId = a.getResourceId(R.styleable.WebViewNativeSections_global_header_view, 0)
                if (headerViewId != 0) {
                    setGlobalHeader(headerViewId)
                }

                val footerViewId = a.getResourceId(R.styleable.WebViewNativeSections_global_footer_view, 0)
                if (footerViewId != 0) {
                    setGlobalFooter(footerViewId)
                }
            } finally {
                a.recycle()
            }
        }

        addOnLayoutChangeListener(this)
    }

    private fun enableZoom(enable: Boolean) {
        settings.setSupportZoom(enable)
        settings.builtInZoomControls = enable
    }

    override fun getHeaderView(sectionId: String): WebViewInjectedView? {
        return mHeaders[sectionId]
    }

    override fun getGlobalHeaderView(): WebViewInjectedView? {
        return mGlobalHeader
    }

    @Synchronized
    private fun setHeader(@LayoutRes viewId: Int, sectionId: String) {
        if (
            mHeaders.keys.contains(sectionId)
        )
            return

        val view = View.inflate(context, viewId, null)
        val myHeaderView = WebViewInjectedView(context, NativeSectionType.HEADER)
        myHeaderView.sectionId = sectionId
        myHeaderView.addView(view, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        this.addView(myHeaderView, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
        ))
        myHeaderView.calculateCorrectHeaderHeight()
        setHeader(myHeaderView)
    }

    @Synchronized
    override fun setGlobalHeader(@LayoutRes viewId: Int) {
        val view = View.inflate(context, viewId, null)
        val myHeaderView = WebViewInjectedView(context, NativeSectionType.HEADER)
        myHeaderView.addView(view, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        this.addView(myHeaderView, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
        ))
        myHeaderView.calculateCorrectHeaderHeight()
        setGlobalHeader(myHeaderView)
    }

    private fun setHeader(v: WebViewInjectedView) {
        val heightListener = NativeSectionHeightListener(
            this,
            v.sectionId,
            NativeSectionType.HEADER
        )
        v.removeOnLayoutChangeListener(heightListener)
        v.addOnLayoutChangeListener(heightListener)
        sectionHeaderHeightListeners[v.sectionId] = heightListener
        mHeaders[v.sectionId] = v
    }

    private fun setGlobalHeader(v: WebViewInjectedView) {
        if (mGlobalHeader === v) return
        val heightListener = NativeSectionHeightListener(
            this,
            null,
            NativeSectionType.HEADER
        )
        v.removeOnLayoutChangeListener(heightListener)
        v.addOnLayoutChangeListener(heightListener)
        sectionHeaderHeightListeners[null] = heightListener
        mGlobalHeader = v
    }

    @Synchronized
    private fun setFooter(@LayoutRes viewId: Int, sectionId: String) {
        if (
            mFooters.keys.contains(sectionId)
        )
            return

        val view = View.inflate(context, viewId, null)
        val myFooterView = WebViewInjectedView(context, NativeSectionType.FOOTER)
        myFooterView.sectionId = sectionId
        myFooterView.addView(view, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        this.addView(myFooterView, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
        ))
        myFooterView.calculateCorrectHeaderHeight()
        setFooter(myFooterView)
    }

    @Synchronized
    override fun setGlobalFooter(@LayoutRes viewId: Int) {
        val view = View.inflate(context, viewId, null)
        val myFooterView = WebViewInjectedView(context, NativeSectionType.FOOTER)
        myFooterView.addView(view, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        this.addView(myFooterView, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
        ))
        myFooterView.calculateCorrectHeaderHeight()
        setGlobalFooter(myFooterView)
    }

    private fun setGlobalFooter(v: WebViewInjectedView) {
        if (mGlobalFooter === v) return
        val heightListener = NativeSectionHeightListener(
            this,
            null,
            NativeSectionType.FOOTER
        )
        v.removeOnLayoutChangeListener(heightListener)
        v.addOnLayoutChangeListener(heightListener)
        sectionFooterHeightListeners[null] = heightListener
        mGlobalFooter = v
    }

    private fun setFooter(v: WebViewInjectedView) {
        val heightListener = NativeSectionHeightListener(
            this,
            v.sectionId,
            NativeSectionType.FOOTER
        )
        v.removeOnLayoutChangeListener(heightListener)
        v.addOnLayoutChangeListener(heightListener)
        sectionFooterHeightListeners[v.sectionId] = heightListener
        mFooters[v.sectionId] = v
    }

    override fun getFooterView(sectionId: String): WebViewInjectedView? {
        return mFooters[sectionId]
    }

    override fun getGlobalFooterView(): WebViewInjectedView? {
        return mGlobalFooter
    }

    override fun setTouchSlop(slop: Int) {
        currentTouchSlop = slop
    }

    override fun toggleHideContent(sectionId: String, onChangeContentVisibility: (Boolean) -> Unit) {
        val script = """
            (function() {
                var isHide = null
                const sectionQuery = document.body.querySelectorAll(":scope > div[id='$sectionId']");
                if (sectionQuery.length > 0) {
                    const section = sectionQuery[0];
                    const iFrameQuery = section.querySelectorAll(":scope > iframe")
                    if (iFrameQuery.length > 0) {
                        const iFrame = iFrameQuery[0];
                        const currentDisplay = iFrame.style.getPropertyValue('display');
                        if (!currentDisplay || currentDisplay != "none") {
                            iFrame.style.setProperty('display', 'none', 'important');
                            isHide = true;
                        } else {
                            iFrame.style.removeProperty('display');
                            isHide = false;
                        }
                    }
                }
                return isHide;
            })();
        """.trimIndent()
        evaluateJavascript(
            script
        ) { returnValue ->
            refreshSectionsHtml()
            if (returnValue != "null") {
                currentSections.find { it.id == sectionId }?.visibility = if (!returnValue.toBoolean())
                    SectionVisibility.VISIBLE
                else
                    SectionVisibility.GONE
                onChangeContentVisibility(!returnValue.toBoolean())
            }
        }
    }

    override fun setContentVisibility(sectionId: String, show: Boolean, onChangeContentVisibility: (Boolean) -> Unit) {
        val script = """
            (function() {
                var isHide = null
                const sectionQuery = document.body.querySelectorAll(":scope > div[id='$sectionId']");
                if (sectionQuery.length > 0) {
                    const section = sectionQuery[0];
                    const iFrameQuery = section.querySelectorAll(":scope > iframe");
                    if (iFrameQuery.length > 0) {
                        const iFrame = iFrameQuery[0];
                        if ($show) {
                            iFrame.style.removeProperty('display');
                            isHide = false;
                        } else {
                            iFrame.style.setProperty('display', 'none', 'important');
                            isHide = true;
                        }
                    }
                }
                return isHide
            })();
        """.trimIndent()
        evaluateJavascript(
            script
        ) { returnValue ->
            refreshSectionsHtml()
            if (returnValue != "null") {
                currentSections.find { it.id == sectionId }?.visibility = if (!returnValue.toBoolean())
                    SectionVisibility.VISIBLE
                else
                    SectionVisibility.GONE
                onChangeContentVisibility(!returnValue.toBoolean())
            }
        }
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
        refreshSectionsHtml()
    }

    private fun refreshSectionsHtml(checkShowPage: Boolean = false, onDone: () -> Unit = {}) {
        val sections = currentSections.joinToString(prefix = "[", postfix = "]") { "'${it.id}'" }
        evaluateJavascript("""
            (function() {
                const allSections = $sections
                var sectionsHeight = {}
                allSections.forEach((sectionId) => { 
                    const sectionQuery = document.body.querySelectorAll(":scope > div[id='" + sectionId + "']");
                    if (sectionQuery.length > 0) {
                        sectionsHeight[sectionId] = {
                            headerPx: 0,
                            footerPx: 0,
                            isHide: false
                        }
                        const section = sectionQuery[0];
                        
                        const iFrameQuery = section.querySelectorAll(":scope > iframe");
                        if (iFrameQuery.length > 0) {
                            const iFrame = iFrameQuery[0];
                            const currentDisplay = iFrame.style.getPropertyValue('display');
                            if (!currentDisplay || currentDisplay != "none") {
                                sectionsHeight[sectionId].isHide = false;
                            } else {
                                sectionsHeight[sectionId].isHide = true;
                            }
                        }
                    
                        const headerQuery = section.querySelectorAll(":scope > .header")
                        if (headerQuery.length > 0) {
                            const header = headerQuery[0];
                            sectionsHeight[sectionId].headerPx = window.pageYOffset + header.getBoundingClientRect().top
                        }
                        
                        const footerQuery = section.querySelectorAll(":scope > .footer")
                        if (footerQuery.length > 0) {
                            const footer = footerQuery[0];
                            sectionsHeight[sectionId].footerPx = window.pageYOffset + footer.getBoundingClientRect().bottom
                        }
                    }
                });
                return { sectionHeight: sectionsHeight, docId: document.body.id };
            })();
        """.trimIndent()
        ) { returnValue ->
            val type: Type = Types.newParameterizedType(
                SectionDistanceHtmlAndDocId::class.java,
                MutableMap::class.java,
                String::class.java,
                SectionDistanceHtml::class.java
            )

            val adapter = moshi.adapter<SectionDistanceHtmlAndDocId>(type)

            val obj: SectionDistanceHtmlAndDocId? = adapter.fromJson(returnValue)
            if (obj != null && obj.docId == currentDocumentId) {
                sectionsDistanceHtml = obj.sectionHeight
                refreshNativeViews(checkShowPage)
            }

            onDone()
        }
    }

    override fun displayHtmlContent(
        webViewClient: WebViewNativeSectionsClient,
        sections: List<WebViewSection>,
        listener: WebViewListener?
    ) {
        setWebViewClient(webViewClient)
        webViewClient.setOnPageListener(this)
        this.listener = listener
        setHtmlContent(sections)
    }

    private fun setHtmlContent(
        sections: List<WebViewSection>,
        loadHtmlContent: Boolean = true
    ) {
        synchronized(this) {
            alpha = 0f

            sections.forEach { section ->
                if (section.headerLayoutId != null)
                    setHeader(section.headerLayoutId, section.id)
                if (section.footerLayoutId != null)
                    setFooter(section.footerLayoutId, section.id)
            }
            injectSectionToHtml(
                sections
            )
            if (loadHtmlContent) {
                loadHtml = Runnable {
                    enableZoom(false)
                    loadDataWithBaseURL(
                        null,
                        currentHtmlText ?: "",
                        "text/html",
                        "utf-8",
                        null
                    )
                    resumeTimers()
                }
                post(loadHtml)
            }
        }
    }

    override fun addAdditionalSection(
        section: WebViewSection
    ) {
        synchronized(this) {
            if (currentSections.find { it.id == section.id } != null)
                return

            currentSections.add(section)

            if (section.headerLayoutId != null)
                setHeader(section.headerLayoutId, section.id)
            if (section.footerLayoutId != null)
                setFooter(section.footerLayoutId, section.id)

            injectSectionToHtml(
                currentSections
            )

            loadHtml = Runnable {
                enableZoom(false)
                loadDataWithBaseURL(
                    url,
                    currentHtmlText ?: "",
                    "text/html",
                    "utf-8",
                    null
                )
                resumeTimers()
            }
            post(loadHtml)
        }
    }

    private fun injectSectionToHtml(
        sections: List<WebViewSection>
    ): String {
        val document = Jsoup.parse(baseHtmlContent)

        sections.forEach { section ->
            val sectionId = section.id
            val sectionHtml = section.htmlText
            val iframe = createIframeSection(sectionHtml, section.url, sectionId)
            addPaddingToElement(
                iframe.getElementsByClass("header")[0],
                getHeaderHeight(sectionId),
                NativeSectionType.HEADER
            )

            addPaddingToElement(
                iframe.getElementsByClass("footer")[0],
                getFooterHeight(sectionId),
                NativeSectionType.FOOTER
            )
            document.body().appendChild(iframe)
        }

        addPaddingToElement(
            document.body(),
            getGlobalHeaderHeight(),
            NativeSectionType.HEADER
        )
        addPaddingToElement(
            document.body(),
            getGlobalFooterHeight(),
            NativeSectionType.FOOTER
        )

        val docId = UUID.randomUUID().toString()

        currentDocumentId = docId
        document.body().id(docId)

        sectionsReadyState = sections.associateTo(mutableMapOf()) {
            it.id to false
        }
        currentDocumentLoaded = false
        currentHtmlText = document.toString()
        currentSections = sections.toMutableList()
        return document.toString()
    }

    private fun addPaddingToElement(
        element: Element,
        paddingPx: Int,
        sectionType: NativeSectionType
    ) {
        val padding: Float = Utility.convertPixelsToPx(
            paddingPx,
            lastScale
        )
        val currentStyle = element.attr("style")
        val paddingCss = when (sectionType) {
            NativeSectionType.HEADER -> "padding-top: ${padding}px !important;"
            NativeSectionType.FOOTER -> "padding-bottom: ${padding}px !important;"
        }
        element.attr("style", "$currentStyle $paddingCss")
    }

    internal fun injectPaddingHeader(
        paddingPx: Int,
        sectionId: String?
    ) {
        val px: Float = Utility.convertPixelsToPx(paddingPx, lastScale)
        evaluateJavascript(
            """
            (function() {
                if (document.body.id == '$currentDocumentId') {
                    if ('$sectionId' == 'null') {
                        document.body.style.setProperty('padding-top', '${px}px', 'important');
                    } else {
                        const sectionQuery = document.body.querySelectorAll(":scope > div[id='$sectionId']");
                        if (sectionQuery.length > 0) {
                            const section = sectionQuery[0];
                            const headerQuery = section.querySelectorAll(":scope > .header")
                            if (headerQuery.length > 0) {
                                const header = headerQuery[0];
                                header.style.setProperty('padding-top','${px}px', 'important');
                            }
                        }
                    }
                }
            })();
            """.trimIndent(),
            null
        )
    }

    internal fun injectPaddingFooter(
        paddingPx: Int,
        sectionId: String?
    ) {
        val px: Float = Utility.convertPixelsToPx(paddingPx, lastScale)
        evaluateJavascript(
            """
            (function() {
                if (document.body.id == '$currentDocumentId') {
                    if ('$sectionId' == 'null') {
                        document.body.style.setProperty('padding-bottom', '${px}px', 'important');
                    } else {
                        const sectionQuery = document.body.querySelectorAll(":scope > div[id='$sectionId']");
                            var list = Array.prototype.map.call(sectionQuery, e => {
                                return e.id;
                            })
                        if (sectionQuery.length > 0) {
                            const section = sectionQuery[0];
                            const footerQuery = section.querySelectorAll(":scope > .footer")
                            if (footerQuery.length > 0) {
                                const footer = footerQuery[0];
                                footer.style.setProperty('padding-bottom','${px}px', 'important');
                            }
                        }
                    }
                }
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onPageEvent(pageEvent: PageEvent, data: String?) {
        when(pageEvent) {
            PageEvent.ON_PAGE_LOADED -> {
                if (!canGoBack())
                    refreshSectionsHtml()
                else
                    alpha = 1f
            }
            PageEvent.ON_REDIRECT -> {
                if (data != null) {
                    alpha = 0f
                    refreshHtmlFromJavascript(false) {
                        children.forEach {
                            it.visibility = View.GONE
                        }
                        loadUrl(data)
                    }
                }
            }
        }
    }

    override fun goBack() {
        if (!canGoBackOrForward(-2))
            restoreViews()
        super.goBack()
    }

    private fun restoreViews() {
        alpha = 0f
        children.forEach {
            it.visibility = View.VISIBLE
        }
    }

    private fun refreshNativeViews(checkShowPage: Boolean = false) {
        val topWrappedSections = mutableSetOf<String>()
        val bottomWrappedSections = mutableSetOf<String>()
        synchronized(this) {
            for (section in currentSections) {
                val sectionDistanceHtml = sectionsDistanceHtml[section.id]
                if (sectionDistanceHtml != null) {
                    if (sectionDistanceHtml.isHide) {
                        topWrappedSections.add(section.id)
                    } else {
                        topWrappedSections.add(section.id)
                        break
                    }
                }
            }
            currentTopWrappedSections = topWrappedSections
            for (section in currentSections.reversed()) {
                if (section.id in topWrappedSections)
                    break

                val sectionDistanceHtml = sectionsDistanceHtml[section.id]
                if (sectionDistanceHtml != null) {
                    if (sectionDistanceHtml.isHide) {
                        bottomWrappedSections.add(section.id)
                    } else {
                        break
                    }
                }
            }
            currentBottomWrappedSections = bottomWrappedSections.reversed().toSet()
        }

        injectPaddingHeader(
            getGlobalHeaderHeight(),
            null
        )
        injectPaddingFooter(
            getGlobalFooterHeight(),
            null
        )
        mHeaders.forEach { (sectionId, nativeView) ->
            injectPaddingHeader(
                getHeaderHeight(sectionId),
                sectionId
            )
            updateNativePosition(nativeView, NativeSectionType.HEADER)
        }
        mFooters.forEach { (sectionId, nativeView) ->
            injectPaddingFooter(
                getFooterHeight(sectionId),
                sectionId
            )
            updateNativePosition(nativeView, NativeSectionType.FOOTER)
        }

        if (checkShowPage)
            checkShowPage()
    }

    private fun getHeaderHeight(sectionId: String): Int {
        return mHeaders[sectionId]?.height ?: 0
    }

    private fun getFooterHeight(sectionId: String): Int {
        return mFooters[sectionId]?.height ?: 0
    }

    private fun getGlobalFooterHeight(): Int {
        return mGlobalFooter?.height ?: 0
    }

    private fun getGlobalHeaderHeight(): Int {
        return mGlobalHeader?.height ?: 0
    }

    override fun addInternalViewTouchListener(sectionId: String?, type: NativeSectionType, viewsAndListeners: List<ViewAndTouchListener>) {
        viewsAndListeners.forEach {
            val nativeView = when(type) {
                NativeSectionType.HEADER -> {
                    if (sectionId == null)
                        mGlobalHeader
                    else
                        mHeaders[sectionId]
                }
                NativeSectionType.FOOTER -> {
                    if (sectionId == null)
                        mGlobalFooter
                    else
                        mFooters[sectionId]
                }
            }
            nativeView?.findViewById<View>(it.viewId)?.setOnTouchListener(it.touchListener)
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

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        onTouchEvent(ev)
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        handleMotionEvent(event)
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
        if (child === mGlobalHeader) {
            mGlobalHeader?.apply {
                offsetLeftAndRight(this@WebViewNativeSections.scrollX - this.left)
            }
        }
        if (child === mGlobalFooter) {
            mGlobalFooter?.apply {
                offsetLeftAndRight(this@WebViewNativeSections.scrollX - this.left)
                this.translationY = (computeVerticalScrollRange() - getGlobalFooterHeight()).toFloat()
            }
        }
        if (child is WebViewInjectedView) {
            mHeaders[child.sectionId]?.apply {
                offsetLeftAndRight(this@WebViewNativeSections.scrollX - this.left)
                updateNativePosition(this, NativeSectionType.HEADER)
            }

            mFooters[child.sectionId]?.apply {
                offsetLeftAndRight(this@WebViewNativeSections.scrollX - this.left)
                updateNativePosition(this, NativeSectionType.FOOTER)
            }
        }

        val result = super.drawChild(canvas, child, drawingTime)
        canvas.restore()
        return result
    }

    @Synchronized
    private fun updateNativePosition(nativeView: WebViewInjectedView, type: NativeSectionType) {
        val currentSectionId = nativeView.sectionId

        var newTranslation: Float? = null
        val sectionDistanceHtml = sectionsDistanceHtml[currentSectionId]
        if (sectionDistanceHtml != null) {
            if (currentSectionId in currentTopWrappedSections) {
                if (
                    type == NativeSectionType.FOOTER &&
                    !sectionDistanceHtml.isHide
                ) {
                    val nextSections = currentSections
                        .takeLastWhile { it.id != currentSectionId }

                    val nextSection = nextSections.firstOrNull()
                    newTranslation = if (
                        nextSection == null ||
                        nextSection.id in currentBottomWrappedSections
                    ) {
                        // questo è in bottomWrapper
                        getBottomViewWrappedTranslation(
                            currentSectionId,
                            type
                        )
                    } else {
                        // questo è in other
                        getBelowViewWrappedTranslation(
                            nextSections,
                            type,
                            currentSectionId
                        )
                    }
                } else {
                    newTranslation = getTopViewWrappedTranslation(
                        currentSectionId,
                        type
                    )
                }
            } else if (currentSectionId in currentBottomWrappedSections) {
                newTranslation = getBottomViewWrappedTranslation(
                    currentSectionId,
                    type
                )
            } else {
                // check bottom wrap

                val nextSections = currentSections
                    .takeLastWhile { it.id != currentSectionId }

                if (
                    (nextSections.isEmpty() ||
                        currentBottomWrappedSections.containsAll(nextSections.map { it.id })) &&
                    type == NativeSectionType.FOOTER
                ) {
                    // questo è in bottomWrapper
                    newTranslation = getBottomViewWrappedTranslation(
                        currentSectionId,
                        type
                    )
                } else {
                    if (
                        sectionDistanceHtml.isHide ||
                        type == NativeSectionType.FOOTER
                    ) {
                        newTranslation = getBelowViewWrappedTranslation(
                            nextSections,
                            type,
                            currentSectionId
                        )
                    } else {
                        newTranslation = getHtmlViewTranslation(
                            currentSectionId,
                            type,
                            sectionDistanceHtml
                        )
                    }
                }
            }
        }

        if (newTranslation != null) {
            if (nativeView.translationY != newTranslation)
                nativeView.translationY = newTranslation
        }
    }

    private fun getBelowViewWrappedTranslation(
        nextSections: List<WebViewSection>,
        type: NativeSectionType,
        currentSectionId: String
    ): Float? {
        var newTranslation: Float? = null
        for (nextSection in nextSections) {
            val nextSectionDistanceHtml = sectionsDistanceHtml[nextSection.id]
            if (nextSectionDistanceHtml != null) {
                if (newTranslation == null)
                    newTranslation = 0f

                if (!nextSectionDistanceHtml.isHide) {
                    newTranslation += getHtmlViewTranslation(
                        nextSection.id,
                        NativeSectionType.HEADER,
                        nextSectionDistanceHtml
                    )
                    break
                } else {
                    newTranslation -= getHeaderHeight(nextSection.id)
                    newTranslation -= getFooterHeight(nextSection.id)
                }
            }
        }

        if (newTranslation != null) {
            newTranslation -= when (type) {
                NativeSectionType.FOOTER ->
                    getFooterHeight(currentSectionId)
                NativeSectionType.HEADER ->
                    getHeaderHeight(currentSectionId) +
                        getFooterHeight(currentSectionId)
            }
        }
        return newTranslation
    }

    private fun getTopViewWrappedTranslation(
        currentSectionId: String,
        type: NativeSectionType
    ): Float {
        var newTranslation = getGlobalHeaderHeight().toFloat()
        currentTopWrappedSections.toList()
            .takeWhile { it != currentSectionId }
            .forEach { prevSectionId ->
                newTranslation += getHeaderHeight(prevSectionId)
                newTranslation += getFooterHeight(prevSectionId)
            }

        if (type == NativeSectionType.FOOTER) {
            newTranslation += getHeaderHeight(currentSectionId)
        }
        return newTranslation
    }

    private fun getHtmlViewTranslation(
        currentSectionId: String,
        type: NativeSectionType,
        sectionDistanceHtml: SectionDistanceHtml
    ): Float {
        val px = when (type) {
            NativeSectionType.FOOTER -> sectionDistanceHtml.footerPx
            NativeSectionType.HEADER -> sectionDistanceHtml.headerPx
        }

        val scale =
            scale //(resources.displayMetrics.density * computeHorizontalScrollRange()) / initialWidth

        var newTranslation = Utility.convertPxToPixels(px, scale).toFloat()
        if (type == NativeSectionType.FOOTER) {
            newTranslation -= getFooterHeight(currentSectionId)
        }
        return newTranslation
    }

    private fun getBottomViewWrappedTranslation(
        currentSectionId: String,
        type: NativeSectionType
    ): Float {
        var bottomViewOffset = getGlobalFooterHeight().toFloat()
        currentBottomWrappedSections.toList()
            .takeLastWhile { it != currentSectionId }
            .forEach { prevSectionId ->
                bottomViewOffset += getHeaderHeight(prevSectionId)
                bottomViewOffset += getFooterHeight(prevSectionId)
            }

        bottomViewOffset += when (type) {
            NativeSectionType.FOOTER ->
                getFooterHeight(currentSectionId)
            NativeSectionType.HEADER ->
                getHeaderHeight(currentSectionId) + getFooterHeight(currentSectionId)
        }
        return computeVerticalScrollRange() - bottomViewOffset
    }

    private fun createIframeSection(
        htmlContent: String,
        urlContent: String?,
        sectionId: String
    ): Element {
        val iFrameContainer = Element("div")
        iFrameContainer.id(sectionId)

        val headerElement = Element("div")
        headerElement.addClass("header")
        iFrameContainer.appendChild(headerElement)

        val iframeElement = Element("iframe")
        iframeElement.attr("style", "width:100%;")
        iframeElement.attr("frameborder", "0")
        iframeElement.attr("scrolling", "no")
        iframeElement.attr(
            "onload",
            "javascript:resizeIframe(this, Android.getCurrentSectionVisibility(\'$sectionId\'));"
        )

        val contentDoc = Jsoup.parse(htmlContent)
        val baseElement = if (urlContent == null)
            contentDoc.head()
                .appendElement("base")
                .attr("href", "about:blank")
        else
            contentDoc.head()
                .appendElement("base")
                .attr("href", urlContent)

        baseElement.attr("target", "_parent")

        iframeElement.attr("srcdoc", contentDoc.toString())
        iFrameContainer.appendChild(iframeElement)

        val footerElement = Element("div")
        footerElement.addClass("footer")
        iFrameContainer.appendChild(footerElement)

        return iFrameContainer
    }

    @JavascriptInterface
    fun getCurrentSectionVisibility(sectionId: String): Boolean {
        return synchronized(this) {
            (currentSections.find { it.id == sectionId }?.visibility) == SectionVisibility.VISIBLE
        }
    }

    @JavascriptInterface
    fun onRefreshWindow(docId: String?) {
        post {
            synchronized(this) {
                var checkShowPage = false
                if (docId != null && docId == currentDocumentId) {
                    currentDocumentLoaded = true
                    checkShowPage = true
                }

                refreshHtmlFromJavascript(checkShowPage)
            }
        }
    }

    @JavascriptInterface
    fun onRefreshSection(docId: String, sectionIdLoaded: String) {
        post {
            synchronized(this) {
                var checkShowPage = false
                if (docId == currentDocumentId) {
                    sectionsReadyState[sectionIdLoaded] = true
                    checkShowPage = true
                }

                refreshHtmlFromJavascript(checkShowPage)
            }
        }
    }

    private fun checkShowPage() {
        synchronized(this) {
            if (currentDocumentLoaded && sectionsReadyState.values.all { it } && alpha == 0f) {
                enableZoom(true)
                listener?.onPageLoaded()
                animate()
                    .setInterpolator(LinearInterpolator())
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    private fun refreshHtmlFromJavascript(checkShowPage: Boolean, onDone: () -> Unit = {}) {
        settings.loadWithOverviewMode = false
        settings.loadWithOverviewMode = true
        setInitialScale(0)
        lastScale = scale
        refreshSectionsHtml(checkShowPage, onDone)
    }

    companion object {
        private val baseHtmlContent = """
            <!DOCTYPE html>
            <html>
              <head>
                <meta id="viewport" name="viewport" content="width=device-width"/>
                <script language="javascript" type="text/javascript">
                  function resizeIframe(obj, isVisible) {
                    obj.style.height = obj.contentWindow.document.documentElement.offsetHeight + 'px'; 
                    if (!isVisible) {
                      obj.style.setProperty('display', 'none', 'important');
                    }
                    Android.onRefreshSection(document.body.id, obj.parentNode.id);
                  }
                </script>
              </head>
              <body style="margin: 0px;">
              </body>
              <script language="javascript" type="text/javascript">
                addEventListener('DOMContentLoaded', (event) => {
                  Android.onRefreshWindow(document.body.id);
                });
                window.onresize = function() { 
                  Android.onRefreshWindow();
                }
              </script>
            </html>
        """.trimIndent()
    }
}