/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */
package org.onap.dcae.collectors.veshv.impl.adapters

import org.onap.dcae.collectors.veshv.boundary.ConfigurationProvider
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.impl.adapters.kafka.KafkaSinkProvider
import org.onap.dcae.collectors.veshv.model.ConfigurationProviderParams
import reactor.ipc.netty.http.client.HttpClient
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object AdapterFactory {
    fun kafkaSink(): SinkProvider = KafkaSinkProvider()
    fun loggingSink(): SinkProvider = LoggingSinkProvider()

    fun consulConfigurationProvider(configurationProviderParams: ConfigurationProviderParams): ConfigurationProvider =
            ConsulConfigurationProvider(
                    configurationProviderParams.configurationUrl,
                    httpAdapter(),
                    configurationProviderParams.firstRequestDelay,
                    configurationProviderParams.requestInterval
            )

    fun httpAdapter(): HttpAdapter = HttpAdapter(HttpClient.create())
}

