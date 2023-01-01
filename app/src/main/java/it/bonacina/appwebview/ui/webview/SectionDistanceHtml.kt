package it.bonacina.appwebview.ui.webview

data class SectionDistanceHtmlAndDocId(
    val sectionHeight: Map<String, SectionDistanceHtml>,
    val docId: String
): java.io.Serializable

data class SectionDistanceHtml(
    val headerPx: Float,
    val footerPx: Float,
    val isHide: Boolean
): java.io.Serializable
