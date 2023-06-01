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

package it.bonacina.webviewnativesections.view

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import it.bonacina.webviewnativesections.domain.NativeSectionType
import it.bonacina.webviewnativesections.WebViewNativeSections
import timber.log.Timber
import java.util.*

/**
 * Represents a container class that represents a native view injected inside WebView
 * through the WebViewNativeSections library.
 */
class WebViewInjectedView : RelativeLayout {

    constructor(context: Context, sectionType: NativeSectionType) : super(context) {
        this.sectionType = sectionType
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        sectionId = UUID.randomUUID().toString()
        isSaveEnabled = true
    }

    var sectionId: String
    var sectionType: NativeSectionType? = null

    companion object {
        private class SavedState : BaseSavedState {
            var sectionId: String? = null

            constructor(superState: Parcelable?) : super(superState)
            private constructor(`in`: Parcel) : super(`in`) {
                sectionId = `in`.readString()
            }

            override fun writeToParcel(out: Parcel, flags: Int) {
                super.writeToParcel(out, flags)
                out.writeString(sectionId)
            }

            override fun describeContents(): Int {
                return 0
            }

            companion object CREATOR : Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()

        val myState = SavedState(superState)
        myState.sectionId = this.sectionId

        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState

        super.onRestoreInstanceState(savedState.superState)

        savedState.sectionId?.let {
            this.sectionId = it
        }
    }

    fun calculateCorrectHeaderHeight(onDone: () -> Unit = {}) {
        val setCorrectHeightRunnable = Runnable {
            try {
                val lp: ViewGroup.LayoutParams = layoutParams
                lp.height = LinearLayout.LayoutParams.WRAP_CONTENT
                layoutParams = lp
                setCorrectHeaderHeight(onDone)
            } catch (ignored: Exception) { }
        }
        try {
            post(setCorrectHeightRunnable)
        } catch (ignored: Exception) { }
    }

    private fun setCorrectHeaderHeight(onDone: () -> Unit = {}) {
        val forceCorrectHeightRunnable = Runnable {
            try {
                measure(
                    MeasureSpec.makeMeasureSpec(this.width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                )
                val targetHeight: Int = measuredHeight
                Timber.d("New targetHeight: %s", targetHeight)
                val lp: ViewGroup.LayoutParams = layoutParams
                if (lp.height != targetHeight) {
                    lp.height = targetHeight
                    layoutParams = lp
                    requestLayout()
                    val webViewParent = parent
                    if (webViewParent is WebViewNativeSections) {
                        when (sectionType) {
                            NativeSectionType.HEADER ->
                                webViewParent.injectPaddingHeader(targetHeight, sectionId)
                            NativeSectionType.FOOTER -> {
                                webViewParent.injectPaddingFooter(targetHeight, sectionId)
                            }
                            else -> { }
                        }
                        onDone()
                    }
                }
            } catch (ignored: Exception) { }
        }
        try {
            post(forceCorrectHeightRunnable)
        } catch (ignored: Exception) { }
    }

    fun getInternalView(): View {
        return getChildAt(0)
    }
}