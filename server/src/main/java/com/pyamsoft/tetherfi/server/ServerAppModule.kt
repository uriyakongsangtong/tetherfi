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

package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.bus.internal.DefaultEventBus
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizerImpl
import com.pyamsoft.tetherfi.server.clients.BlockedClientTracker
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.ClientEraser
import com.pyamsoft.tetherfi.server.clients.ClientManagerImpl
import com.pyamsoft.tetherfi.server.clients.SeenClients
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.permission.PermissionGuardImpl
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.WifiSharedProxy
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.manager.factory.DefaultProxyManagerFactory
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxySession
import com.pyamsoft.tetherfi.server.urlfixer.PSNUrlFixer
import com.pyamsoft.tetherfi.server.urlfixer.UrlFixer
import com.pyamsoft.tetherfi.server.widi.WiDiConfig
import com.pyamsoft.tetherfi.server.widi.WiDiConfigImpl
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkImpl
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiverRegister
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import com.pyamsoft.tetherfi.server.widi.receiver.WifiDirectReceiver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@Module
abstract class ServerAppModule {

  @Binds
  @CheckResult
  internal abstract fun bindShutdownConsumer(
      impl: EventBus<ServerShutdownEvent>
  ): EventConsumer<ServerShutdownEvent>

  @Binds
  @CheckResult
  internal abstract fun bindPermissionChecker(impl: PermissionGuardImpl): PermissionGuard

  @Binds @CheckResult internal abstract fun bindWiDiNetwork(impl: WiDiNetworkImpl): WiDiNetwork

  @Binds
  @CheckResult
  internal abstract fun bindWiDiNetworkStatus(impl: WiDiNetworkImpl): WiDiNetworkStatus

  @Binds
  @CheckResult
  internal abstract fun bindBatteryOptimizer(impl: BatteryOptimizerImpl): BatteryOptimizer

  @Binds
  @CheckResult
  internal abstract fun bindBlockedClients(impl: ClientManagerImpl): BlockedClients

  @Binds @CheckResult internal abstract fun bindSeenClients(impl: ClientManagerImpl): SeenClients

  @Binds @CheckResult internal abstract fun bindClientEraser(impl: ClientManagerImpl): ClientEraser

  @Binds
  @CheckResult
  internal abstract fun bindBlockedClientTracker(impl: ClientManagerImpl): BlockedClientTracker

  // Internals

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindProxy(impl: WifiSharedProxy): SharedProxy

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindConfig(impl: WiDiConfigImpl): WiDiConfig

  @Binds
  @IntoSet
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindPSNUrlFixer(impl: PSNUrlFixer): UrlFixer

  @Binds @CheckResult internal abstract fun bindWidiReceiver(impl: WifiDirectReceiver): WiDiReceiver

  @Binds
  @CheckResult
  internal abstract fun bindWidiReceiverRegister(impl: WifiDirectReceiver): WiDiReceiverRegister

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindProxyManagerFactory(
      impl: DefaultProxyManagerFactory
  ): ProxyManager.Factory

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindTcpProxySession(impl: TcpProxySession): ProxySession<TcpProxyData>

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    @Named("server")
    internal fun provideServerPermissionRequester(guard: PermissionGuard): PermissionRequester {
      return PermissionRequester.create(guard.requiredPermissions.toTypedArray())
    }

    @Provides
    @JvmStatic
    @Singleton
    @ServerInternalApi
    internal fun provideWidiReceiverEventBus(): EventBus<WidiNetworkEvent> {
      return DefaultEventBus()
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideShutdownEventBus(): EventBus<ServerShutdownEvent> {
      return DefaultEventBus()
    }

    @Provides
    @JvmStatic
    @ServerInternalApi
    internal fun provideProxyDebug(): ProxyDebug {
      return ProxyDebug.NONE
    }

    @Provides
    @JvmStatic
    @Singleton
    @ServerInternalApi
    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun provideProxyDispatcher(): CoroutineDispatcher {
      // We max paralellism at CORES * 4 to avoid CPU trashing at the system level from
      // resource starving when a bunch of requests fly in at once
      //
      // We max out total parallelism at 32 at the absolute most
      val coreCount = Runtime.getRuntime().availableProcessors()
      val parallelism = coreCount.times(4).coerceAtMost(32)

      Timber.d("Using Proxy limited dispatcher=$parallelism")
      return Dispatchers.IO.limitedParallelism(parallelism)
    }
  }
}
