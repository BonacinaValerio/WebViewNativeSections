package it.bonacina.webviewzoomable.domain

import androidx.annotation.LayoutRes
import java.util.*

data class WebViewSection(
    val htmlText: String,
    val url: String? = null,
    @LayoutRes val headerLayoutId: Int? = null,
    @LayoutRes val footerLayoutId: Int? = null,
    var visibility: SectionVisibility = SectionVisibility.VISIBLE
) {
    val id = UUID.randomUUID().toString()
}

enum class SectionVisibility {
    VISIBLE,
    GONE
}