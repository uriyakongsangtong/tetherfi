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

package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import java.time.LocalDateTime

private val UNIT_JUMP = 1024UL

@Stable
@Immutable
sealed class TetherClient(
    open val nickName: String,
    open val mostRecentlySeen: LocalDateTime,
    protected open val totalBytes: ByteTransferReport,
) {

  val transferToInternet by lazy { parseBytesToDisplay(totalBytes.proxyToInternet) }
  val transferFromInternet by lazy { parseBytesToDisplay(totalBytes.internetToProxy) }

  @CheckResult
  private fun parseBytesToDisplay(total: ULong): String {
    var amount = total
    var suffix = " bytes"
    while (amount > UNIT_JUMP) {
      suffix = mapSuffixToNextLargest(amount, suffix)
      amount /= UNIT_JUMP
    }

    return "$amount$suffix"
  }

  @CheckResult
  private fun mapSuffixToNextLargest(amount: ULong, suffix: String): String =
      when (suffix) {
        " bytes" -> "KB"
        "KB" -> "MB"
        "MB" -> "GB"
        "GB" -> "TB"
        "TB" -> "PB"
        else -> throw IllegalStateException("Bytes payload too big: $amount$suffix")
      }

  @CheckResult
  fun matches(o: TetherClient): Boolean {
    when (this) {
      is IpAddress -> {
        if (o is IpAddress) {
          return ip == o.ip
        }

        return false
      }
      is HostName -> {
        if (o is HostName) {
          return hostname == o.hostname
        }

        return false
      }
    }
  }

  @CheckResult
  fun mergeReport(report: ByteTransferReport): ByteTransferReport {
    return report.copy(
        internetToProxy = report.internetToProxy + totalBytes.internetToProxy,
        proxyToInternet = report.proxyToInternet + totalBytes.proxyToInternet,
    )
  }

  data class IpAddress(
      val ip: String,
      override val nickName: String,
      override val mostRecentlySeen: LocalDateTime,
      override val totalBytes: ByteTransferReport,
  ) :
      TetherClient(
          nickName = nickName,
          mostRecentlySeen = mostRecentlySeen,
          totalBytes = totalBytes,
      )

  data class HostName(
      val hostname: String,
      override val nickName: String,
      override val mostRecentlySeen: LocalDateTime,
      override val totalBytes: ByteTransferReport,
  ) :
      TetherClient(
          nickName = nickName,
          mostRecentlySeen = mostRecentlySeen,
          totalBytes = totalBytes,
      )
}

@CheckResult
fun TetherClient.key(): String {
  return when (this) {
    is TetherClient.HostName -> this.hostname
    is TetherClient.IpAddress -> this.ip
  }
}
