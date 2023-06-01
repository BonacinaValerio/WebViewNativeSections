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
