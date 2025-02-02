/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.info.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.info.InfoViewState
import com.pyamsoft.tetherfi.info.MutableInfoViewState
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.TestServerViewState
import com.pyamsoft.tetherfi.ui.icons.QrCode
import com.pyamsoft.tetherfi.ui.icons.Visibility
import com.pyamsoft.tetherfi.ui.icons.VisibilityOff
import com.pyamsoft.tetherfi.ui.rememberServerHostname
import com.pyamsoft.tetherfi.ui.rememberServerPassword
import com.pyamsoft.tetherfi.ui.rememberServerRawPassword
import com.pyamsoft.tetherfi.ui.rememberServerSSID

private enum class DeviceSetupContentTypes {
  SETTINGS,
  CONNECT,
  TOGGLE,
}

internal fun LazyListScope.renderDeviceSetup(
    itemModifier: Modifier = Modifier,
    appName: String,
    state: InfoViewState,
    serverViewState: ServerViewState,
    onShowQRCode: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
  item(
      contentType = DeviceSetupContentTypes.SETTINGS,
  ) {
    OtherInstruction(
        modifier = itemModifier,
    ) {
      Text(
          text = "Open the Wi-Fi settings page",
          style = MaterialTheme.typography.bodyLarge,
      )
    }
  }

  item(
      contentType = DeviceSetupContentTypes.CONNECT,
  ) {
    OtherInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Column {
        Text(
            text = "Connect to the $appName Hotspot",
            style =
                MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )

        Row {
          val group by serverViewState.group.collectAsStateWithLifecycle()
          val ssid = rememberServerSSID(group)

          val password = rememberServerRawPassword(group)
          val isNetworkReadyForQRCode =
              remember(
                  ssid,
                  password,
              ) {
                ssid.isNotBlank() && password.isNotBlank()
              }

          Text(
              text = "Name",
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
          )

          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = ssid,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontWeight = FontWeight.W700,
                      fontFamily = FontFamily.Monospace,
                  ),
          )

          if (isNetworkReadyForQRCode) {
            // Don't use IconButton because we don't care about minimum touch target size
            Box(
                modifier =
                    Modifier.padding(start = MaterialTheme.keylines.baseline)
                        .clickable { onShowQRCode() }
                        .padding(MaterialTheme.keylines.typography),
                contentAlignment = Alignment.Center,
            ) {
              Icon(
                  modifier = Modifier.size(16.dp),
                  imageVector = Icons.Filled.QrCode,
                  contentDescription = "QR Code",
                  tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        Row {
          val group by serverViewState.group.collectAsStateWithLifecycle()
          val isPasswordVisible by state.isPasswordVisible.collectAsStateWithLifecycle()
          val password = rememberServerPassword(group, isPasswordVisible)
          val rawPassword = rememberServerRawPassword(group)

          val hapticManager = LocalHapticManager.current

          Text(
              text = "Password",
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
          )
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = password,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontWeight = FontWeight.W700,
                      fontFamily = FontFamily.Monospace,
                  ),
          )

          if (rawPassword.isNotBlank()) {
            // Don't use IconButton because we don't care about minimum touch target size
            Box(
                modifier =
                    Modifier.padding(start = MaterialTheme.keylines.baseline)
                        .clickable {
                          if (isPasswordVisible) {
                            hapticManager?.toggleOff()
                          } else {
                            hapticManager?.toggleOn()
                          }
                          onTogglePasswordVisibility()
                        }
                        .padding(MaterialTheme.keylines.typography),
                contentAlignment = Alignment.Center,
            ) {
              Icon(
                  modifier = Modifier.size(16.dp),
                  imageVector =
                      if (isPasswordVisible) Icons.Filled.VisibilityOff
                      else Icons.Filled.Visibility,
                  contentDescription =
                      if (isPasswordVisible) "Password Visible" else "Password Hidden",
                  tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        Text(
            modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
            text = "Configure the proxy settings",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
            text = "Use MANUAL mode and configure both HTTP and HTTPS proxy options.",
            style =
                MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )

        Row(
            modifier = Modifier.padding(top = MaterialTheme.keylines.typography),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          val connection by serverViewState.connection.collectAsStateWithLifecycle()
          val ipAddress = rememberServerHostname(connection)

          Text(
              text = "URL",
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
          )
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = ipAddress,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontWeight = FontWeight.W700,
                      fontFamily = FontFamily.Monospace,
                  ),
          )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              text = "Port",
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
          )

          val port by serverViewState.port.collectAsStateWithLifecycle()
          val portNumber = remember(port) { if (port <= 1024) "INVALID PORT" else "$port" }
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = portNumber,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontWeight = FontWeight.W700,
                      fontFamily = FontFamily.Monospace,
                  ),
          )
        }
      }
    }
  }

  item(
      contentType = DeviceSetupContentTypes.TOGGLE,
  ) {
    OtherInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Column {
        Text(
            text = "Turn the Wi-Fi off and back on again.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "It should automatically connect to the $appName Hotspot",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
      }
    }
  }
}

@Preview
@Composable
private fun PreviewDeviceSetup() {
  LazyColumn {
    renderDeviceSetup(
        appName = "TEST",
        serverViewState = TestServerViewState(),
        state = MutableInfoViewState(),
        onTogglePasswordVisibility = {},
        onShowQRCode = {},
    )
  }
}
