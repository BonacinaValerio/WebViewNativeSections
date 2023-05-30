package it.bonacina.webviewnativesections.domain

import android.view.View
import androidx.annotation.IdRes

/**
 * Represents a view and its associated touch listener.
 *
 * @param viewId The resource ID of the view.
 * @param touchListener The [View.OnTouchListener] for the view.
 */
data class ViewAndTouchListener(
    @IdRes val viewId: Int,
    val touchListener: View.OnTouchListener
)
