package com.pyamsoft.widefi.status

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.widefi.server.status.RunningStatus

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onToggle: () -> Unit,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
) {
  val proxyStatus = state.proxyStatus
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
          is RunningStatus.Error -> "WideFi Error"
          is RunningStatus.NotRunning -> "Turn WideFi ON"
          is RunningStatus.Running -> "Turn WideFi OFF"
          else -> "WideFi is thinking..."
        }
      }

  val scaffoldState = rememberScaffoldState()

  Scaffold(
      modifier = modifier,
      scaffoldState = scaffoldState,
  ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
      item {
        Column(
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
        ) {
          Button(
              enabled = isButtonEnabled,
              onClick = onToggle,
          ) {
            Text(
                text = buttonText,
            )
          }
        }
      }

      item {
        Column(
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
        ) {
          DisplayStatus(
              title = "WiFi Network Status:",
              status = wiDiStatus,
          )

          DisplayStatus(
              title = "Proxy Status:",
              status = proxyStatus,
          )
        }
      }

      if (isLoaded) {
        item {
          NetworkInformation(
              modifier =
                  Modifier.padding(top = MaterialTheme.keylines.content)
                      .padding(horizontal = MaterialTheme.keylines.content),
              state = state,
              onSsidChanged = onSsidChanged,
              onPasswordChanged = onPasswordChanged,
              onPortChanged = onPortChanged,
          )
        }
      } else {
        item {
          Column(
              modifier =
                  Modifier.padding(top = MaterialTheme.keylines.content)
                      .padding(horizontal = MaterialTheme.keylines.content),
          ) {
            Box(
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
          }
        }
      }
    }
  }
}

@Composable
private fun NetworkInformation(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
) {
  val isEditable = remember(state.wiDiStatus) { state.wiDiStatus == RunningStatus.NotRunning }

  val group = state.group
  val ssid =
      remember(isEditable, group, state.ssid) {
        if (isEditable) state.ssid else group?.ssid ?: "--"
      }
  val password =
      remember(isEditable, group, state.password) {
        if (isEditable) state.password else group?.password ?: "--"
      }
  val bandName = remember(state.band) { state.band?.name ?: "--" }

  val ip = remember(state.ip) { state.ip.ifBlank { "--" } }
  val port = remember(state.port) { if (state.port <= 0) "--" else "${state.port}" }

  Crossfade(
      modifier = modifier,
      targetState = isEditable,
  ) { editable ->
    Column {
      if (editable) {
        Editor(
            title = "SSID",
            value = ssid,
            onChange = onSsidChanged,
        )

        Editor(
            title = "PASSWORD",
            value = password,
            onChange = onPasswordChanged,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
        )

        Editor(
            title = "PORT",
            value = port,
            onChange = onPortChanged,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
        )
      } else {
        Item(
            title = "SSID",
            value = ssid,
        )

        Item(
            title = "PASSWORD",
            value = password,
        )

        Item(
            title = "BAND",
            value = bandName,
        )

        Item(
            title = "IP",
            value = ip,
        )

        Item(
            title = "PORT",
            value = port,
        )
      }
    }
  }
}

@Composable
private fun Editor(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onChange: (String) -> Unit,
) {
  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = title,
        style = MaterialTheme.typography.body2,
    )
    TextField(
        modifier = Modifier.padding(start = MaterialTheme.keylines.baseline),
        keyboardOptions = keyboardOptions,
        value = value,
        onValueChange = onChange,
    )
  }
}

@Composable
private fun DisplayStatus(
    modifier: Modifier = Modifier,
    title: String,
    status: RunningStatus,
) {
  val text =
      remember(status) {
        when (status) {
          is RunningStatus.Error -> "Error: ${status.message}"
          is RunningStatus.NotRunning -> "Not Running"
          is RunningStatus.Running -> "Running"
          is RunningStatus.Starting -> "Starting"
          is RunningStatus.Stopping -> "Stopping"
        }
      }

  val color =
      remember(status) {
        when (status) {
          is RunningStatus.Error -> Color.Red
          is RunningStatus.NotRunning -> Color.Unspecified
          is RunningStatus.Running -> Color.Green
          is RunningStatus.Starting -> Color.Cyan
          is RunningStatus.Stopping -> Color.Magenta
        }
      }

  Item(
      modifier = modifier,
      title = title,
      value = text,
      color = color,
  )
}

@Composable
private fun Item(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color = Color.Unspecified,
) {
  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = title,
        style = MaterialTheme.typography.body2,
    )
    Text(
        modifier = Modifier.padding(start = MaterialTheme.keylines.baseline),
        text = value,
        style = MaterialTheme.typography.body2,
        color = color,
    )
  }
}
