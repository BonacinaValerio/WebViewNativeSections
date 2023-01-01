package it.bonacina.appwebview.ui.webview

import androidx.annotation.LayoutRes
import java.util.*

data class WebViewSection(
    val htmlText: String,
    @LayoutRes val headerLayoutId: Int? = null,
    @LayoutRes val footerLayoutId: Int? = null,
    val initialVisibility: SectionVisibility = SectionVisibility.VISIBLE
) {
    val id = UUID.randomUUID().toString()
}

enum class SectionVisibility {
    VISIBLE,
    GONE
}