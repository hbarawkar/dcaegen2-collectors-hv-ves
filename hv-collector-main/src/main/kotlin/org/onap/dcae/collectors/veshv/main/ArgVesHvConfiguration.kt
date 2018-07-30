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
package org.onap.dcae.collectors.veshv.main

import arrow.core.ForOption
import arrow.core.Option
import arrow.core.fix
import arrow.instances.extensions
import arrow.typeclasses.binding
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.model.ConfigurationProviderParams
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.*
import java.time.Duration

internal class ArgVesHvConfiguration : ArgBasedConfiguration<ServerConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList = listOf(
            LISTEN_PORT,
            CONSUL_CONFIG_URL,
            CONSUL_FIRST_REQUEST_DELAY,
            CONSUL_REQUEST_INTERVAL,
            SSL_DISABLE,
            PRIVATE_KEY_FILE,
            CERT_FILE,
            TRUST_CERT_FILE,
            IDLE_TIMEOUT_SEC,
            DUMMY_MODE
    )

    override fun getConfiguration(cmdLine: CommandLine): Option<ServerConfiguration> =
            ForOption extensions {
                binding {
                    val listenPort = cmdLine.intValue(LISTEN_PORT).bind()
                    val idleTimeoutSec = cmdLine.longValue(IDLE_TIMEOUT_SEC, DefaultValues.IDLE_TIMEOUT_SEC)
                    val dummyMode = cmdLine.hasOption(DUMMY_MODE)
                    val security = createSecurityConfiguration(cmdLine)
                    val configurationProviderParams = createConfigurationProviderParams(cmdLine)

                    ServerConfiguration(
                            listenPort = listenPort,
                            configurationProviderParams = configurationProviderParams,
                            securityConfiguration = security,
                            idleTimeout = Duration.ofSeconds(idleTimeoutSec),
                            dummyMode = dummyMode)
                }.fix()
            }


    private fun createConfigurationProviderParams(cmdLine: CommandLine): ConfigurationProviderParams {
        val configUrl = cmdLine.stringValue(CONSUL_CONFIG_URL, DefaultValues.CONFIG_URL)
        val firstRequestDelay = cmdLine.longValue(CONSUL_FIRST_REQUEST_DELAY, DefaultValues.CONSUL_FIRST_REQUEST_DELAY)
        val requestInterval = cmdLine.longValue(CONSUL_REQUEST_INTERVAL, DefaultValues.CONSUL_REQUEST_INTERVAL)

        return ConfigurationProviderParams(
                configUrl,
                Duration.ofSeconds(firstRequestDelay),
                Duration.ofSeconds(requestInterval)
        )
    }

    private fun createSecurityConfiguration(cmdLine: CommandLine): SecurityConfiguration {
        val sslDisable = cmdLine.hasOption(SSL_DISABLE)
        val pkFile = cmdLine.stringValue(PRIVATE_KEY_FILE, DefaultValues.PRIVATE_KEY_FILE)
        val certFile = cmdLine.stringValue(CERT_FILE, DefaultValues.CERT_FILE)
        val trustCertFile = cmdLine.stringValue(TRUST_CERT_FILE, DefaultValues.TRUST_CERT_FILE)

        return SecurityConfiguration(
                sslDisable = sslDisable,
                privateKey = stringPathToPath(pkFile),
                cert = stringPathToPath(certFile),
                trustedCert = stringPathToPath(trustCertFile)
        )
    }

    internal object DefaultValues {
        const val CONSUL_FIRST_REQUEST_DELAY = 10L
        const val CONSUL_REQUEST_INTERVAL = 5L
        const val CONFIG_URL = ""
        const val PRIVATE_KEY_FILE = "/etc/ves-hv/server.key"
        const val CERT_FILE = "/etc/ves-hv/server.crt"
        const val TRUST_CERT_FILE = "/etc/ves-hv/trust.crt"
        const val IDLE_TIMEOUT_SEC = 60L
    }
}