/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.status.sections.operating

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.tetherfi.core.FeatureFlags
import com.pyamsoft.tetherfi.status.StatusObjectGraph
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.Label
import javax.inject.Inject

private enum class OperatingSettingsContentTypes {
  LABEL,
  BATTERY_OPTIMIZATION,
  ADD_TILE,
}

internal fun LazyListScope.renderOperatingSettings(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,
    featureFlags: FeatureFlags,

    // Battery optimization
    onDisableBatteryOptimizations: () -> Unit,
) {
  item(
      contentType = OperatingSettingsContentTypes.LABEL,
  ) {
    Label(
        modifier =
            itemModifier
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Operating Settings",
    )
  }

  item(
      contentType = OperatingSettingsContentTypes.BATTERY_OPTIMIZATION,
  ) {
    BatteryOptimization(
        modifier = itemModifier,
        isEditable = isEditable,
        appName = appName,
        state = state,
        onDisableBatteryOptimizations = onDisableBatteryOptimizations,
    )
  }

  if (featureFlags.isTileUiEnabled) {
    item(contentType = OperatingSettingsContentTypes.ADD_TILE) {
      AddTheTile(
          modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
          isEditable = isEditable,
          appName = appName,
      )
    }
  }
}
