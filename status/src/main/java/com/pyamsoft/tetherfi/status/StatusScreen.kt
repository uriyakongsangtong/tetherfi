package com.pyamsoft.tetherfi.status

import androidx.annotation.CheckResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.widget.NewVersionWidget
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus

private const val SYSTEM_DEFINED = "SYSTEM DEFINED: CANNOT CHANGE"

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: StatusViewState,
    onToggle: () -> Unit,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onDismissPermissionExplanation: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleKeepWakeLock: () -> Unit,
    onToggleConnectionInstructions: () -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
) {
  val wiDiStatus = state.wiDiStatus
  val isLoaded = state.preferencesLoaded

  val isButtonEnabled =
      remember(wiDiStatus) {
        wiDiStatus is RunningStatus.Running ||
            wiDiStatus is RunningStatus.NotRunning ||
            wiDiStatus is RunningStatus.Error
      }

  val buttonText =
      remember(wiDiStatus) {
        when (wiDiStatus) {
          is RunningStatus.Error -> "$appName Error"
          is RunningStatus.NotRunning -> "Turn $appName ON"
          is RunningStatus.Running -> "Turn $appName OFF"
          else -> "$appName is thinking..."
        }
      }

  val scaffoldState = rememberScaffoldState()

  val loadedContent =
      rememberPreparedLoadedContent(
          appName = appName,
          state = state,
          onSsidChanged = onSsidChanged,
          onPasswordChanged = onPasswordChanged,
          onPortChanged = onPortChanged,
          onOpenBatterySettings = onOpenBatterySettings,
          onToggleConnectionInstructions = onToggleConnectionInstructions,
          onToggleKeepWakeLock = onToggleKeepWakeLock,
          onSelectBand = onSelectBand,
      )

  Scaffold(
      modifier = modifier,
      scaffoldState = scaffoldState,
  ) { pv ->
    PermissionExplanationDialog(
        modifier = Modifier.padding(pv),
        state = state,
        appName = appName,
        onDismissPermissionExplanation = onDismissPermissionExplanation,
        onOpenPermissionSettings = onOpenPermissionSettings,
        onRequestPermissions = onRequestPermissions,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
      item {
        NewVersionWidget(
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
        )
      }
      item {
        Button(
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
            enabled = isButtonEnabled,
            onClick = onToggle,
        ) {
          Text(
              text = buttonText,
          )
        }
      }

      item {
        DisplayStatus(
            modifier =
                Modifier.padding(MaterialTheme.keylines.content)
                    .padding(bottom = MaterialTheme.keylines.content),
            title = "Tethering Network Status:",
            status = wiDiStatus,
        )
      }

      if (isLoaded) {
        loadedContent()
      } else {
        item {
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(top = MaterialTheme.keylines.content)
                      .padding(horizontal = MaterialTheme.keylines.content),
              contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
                modifier = Modifier.size(120.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
@CheckResult
private fun rememberPreparedLoadedContent(
    appName: String,
    state: StatusViewState,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleConnectionInstructions: () -> Unit,
    onToggleKeepWakeLock: () -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
): LazyListScope.() -> Unit {
  val canUseCustomConfig = remember { ServerDefaults.canUseCustomConfig() }
  val isEditable =
      remember(state.wiDiStatus) {
        when (state.wiDiStatus) {
          is RunningStatus.Running,
          is RunningStatus.Starting,
          is RunningStatus.Stopping -> false
          else -> true
        }
      }

  val showErrorHintMessage = remember(state.wiDiStatus) { state.wiDiStatus is RunningStatus.Error }

  val group = state.group
  val ssid =
      remember(
          isEditable,
          canUseCustomConfig,
          group?.ssid,
          state.ssid,
      ) {
        if (isEditable) {
          if (canUseCustomConfig) {
            state.ssid
          } else {
            SYSTEM_DEFINED
          }
        } else {
          group?.ssid ?: "NO SSID"
        }
      }
  val password =
      remember(
          isEditable,
          canUseCustomConfig,
          group?.password,
          state.password,
      ) {
        if (isEditable) {
          if (canUseCustomConfig) {
            state.password
          } else {
            SYSTEM_DEFINED
          }
        } else {
          group?.password ?: "NO PASSWORD"
        }
      }

  val ip = remember(state.ip) { state.ip.ifBlank { "NO IP ADDRESS" } }
  val port = remember(state.port) { if (state.port <= 0) "NO PORT" else "${state.port}" }

  val keylines = MaterialTheme.keylines
  return remember(
      keylines,
      appName,
      showErrorHintMessage,
      ssid,
      password,
      port,
      ip,
      onSsidChanged,
      onPasswordChanged,
      onPortChanged,
      onToggleConnectionInstructions,
      onToggleKeepWakeLock,
      onSelectBand,
      isEditable,
      state,
  ) {
    {
      renderNetworkInformation(
          itemModifier = Modifier.fillMaxWidth().padding(horizontal = keylines.content),
          isEditable = isEditable,
          canUseCustomConfig = canUseCustomConfig,
          appName = appName,
          showPermissionMessage = state.requiresPermissions,
          showErrorHintMessage = showErrorHintMessage,
          ssid = ssid,
          password = password,
          port = port,
          ip = ip,
          band = state.band,
          onSsidChanged = onSsidChanged,
          onPasswordChanged = onPasswordChanged,
          onPortChanged = onPortChanged,
          onSelectBand = onSelectBand,
      )

      renderBatteryAndPerformance(
          itemModifier = Modifier.fillMaxWidth().padding(horizontal = keylines.content),
          isEditable = isEditable,
          appName = appName,
          keepWakeLock = state.keepWakeLock,
          isBatteryOptimizationDisabled = state.isBatteryOptimizationsIgnored,
          onToggleKeepWakeLock = onToggleKeepWakeLock,
          onDisableBatteryOptimizations = onOpenBatterySettings,
      )

      // Bottom space
      item {
        Spacer(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = keylines.content)
                    .height(MaterialTheme.keylines.content),
        )
      }

      item {
        ConnectionInstructions(
            modifier = Modifier.padding(MaterialTheme.keylines.content),
            showing = state.isConnectionInstructionExpanded,
            canConfigure = canUseCustomConfig,
            appName = appName,
            ssid = ssid,
            password = password,
            port = port,
            ip = ip,
            onToggleConnectionInstructions = onToggleConnectionInstructions,
        )
      }

      item {
        Spacer(
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content).navigationBarsPadding(),
        )
      }
    }
  }
}

private fun LazyListScope.renderNetworkInformation(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    showPermissionMessage: Boolean,
    showErrorHintMessage: Boolean,
    canUseCustomConfig: Boolean,
    ssid: String,
    password: String,
    port: String,
    ip: String,
    band: ServerNetworkBand?,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
) {
  item {
    AnimatedVisibility(
        modifier = itemModifier,
        visible = showErrorHintMessage,
    ) {
      Box(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.content),
      ) {
        Text(
            text = "Try toggling this device's Wi-Fi off and on, then try again.",
            style =
                MaterialTheme.typography.body1.copy(
                    color = MaterialTheme.colors.error,
                ),
        )
      }
    }
  }

  item {
    AnimatedVisibility(
        modifier = itemModifier,
        visible = showPermissionMessage,
    ) {
      Box(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.content),
      ) {
        Text(
            text = "$appName requires permissions: Click the button and grant permissions",
            style =
                MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.error,
                ),
        )
      }
    }
  }

  if (isEditable) {
    item {
      StatusEditor(
          modifier = itemModifier.padding(bottom = MaterialTheme.keylines.baseline),
          enabled = canUseCustomConfig,
          title = "HOTSPOT NAME/SSID",
          value = ssid,
          onChange = onSsidChanged,
      )
    }

    item {
      StatusEditor(
          modifier = itemModifier.padding(bottom = MaterialTheme.keylines.baseline),
          enabled = canUseCustomConfig,
          title = "HOTSPOT PASSWORD",
          value = password,
          onChange = onPasswordChanged,
          keyboardOptions =
              KeyboardOptions(
                  keyboardType = KeyboardType.Password,
              ),
      )
    }

    item {
      StatusEditor(
          modifier = itemModifier.padding(bottom = MaterialTheme.keylines.baseline),
          title = "PROXY PORT",
          value = port,
          onChange = onPortChanged,
          keyboardOptions =
              KeyboardOptions(
                  keyboardType = KeyboardType.Number,
              ),
      )
    }
  } else {
    item {
      StatusItem(
          modifier = itemModifier.padding(bottom = MaterialTheme.keylines.baseline),
          title = "HOTSPOT NAME/SSID",
          value = ssid,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
              ),
      )
    }

    item {
      StatusItem(
          modifier = itemModifier.padding(bottom = MaterialTheme.keylines.content * 2),
          title = "HOTSPOT PASSWORD",
          value = password,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
              ),
      )
    }

    item {
      StatusItem(
          modifier = itemModifier.padding(bottom = MaterialTheme.keylines.baseline),
          title = "PROXY URL/HOSTNAME",
          value = ip,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
              ),
      )
    }

    item {
      StatusItem(
          modifier = itemModifier.padding(bottom = MaterialTheme.keylines.baseline),
          title = "PROXY PORT",
          value = port,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
              ),
      )
    }
  }
  item {
    NetworkBands(
        modifier = itemModifier.padding(vertical = MaterialTheme.keylines.content),
        isEnabled = canUseCustomConfig,
        isEditable = isEditable,
        band = band,
        onSelectBand = onSelectBand,
    )
  }
}

private fun LazyListScope.renderBatteryAndPerformance(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    keepWakeLock: Boolean,
    isBatteryOptimizationDisabled: Boolean,
    onToggleKeepWakeLock: () -> Unit,
    onDisableBatteryOptimizations: () -> Unit,
) {
  item {
    Text(
        modifier =
            itemModifier
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Battery and Performance",
        style =
            MaterialTheme.typography.caption.copy(
                fontWeight = FontWeight.W700,
                color =
                    MaterialTheme.colors.onBackground.copy(
                        alpha = ContentAlpha.medium,
                    ),
            ),
    )
  }

  item {
    CpuWakelock(
        modifier = itemModifier.padding(bottom = MaterialTheme.keylines.content),
        isEditable = isEditable,
        appName = appName,
        keepWakeLock = keepWakeLock,
        onToggleKeepWakeLock = onToggleKeepWakeLock,
    )
  }

  item {
    BatteryOptimization(
        modifier = itemModifier,
        isEditable = isEditable,
        appName = appName,
        isBatteryOptimizationDisabled = isBatteryOptimizationDisabled,
        onDisableBatteryOptimizations = onDisableBatteryOptimizations,
    )
  }
}
