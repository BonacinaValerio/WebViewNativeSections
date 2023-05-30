package it.bonacina.webviewnativesections.domain

internal data class SectionDistanceHtmlAndDocId(
    val sectionHeight: Map<String, SectionDistanceHtml>,
    val docId: String
): java.io.Serializable

internal data class SectionDistanceHtml(
    val headerPx: Float,
    val footerPx: Float,
    val isHide: Boolean
): java.io.Serializable
