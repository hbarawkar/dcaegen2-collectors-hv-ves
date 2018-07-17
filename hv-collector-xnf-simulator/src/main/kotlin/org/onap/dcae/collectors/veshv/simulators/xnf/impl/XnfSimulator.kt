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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl

import arrow.effects.IO
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import org.onap.dcae.collectors.veshv.domain.EndOfTransmissionMessage
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.domain.WireFrameEncoder
import org.onap.dcae.collectors.veshv.simulators.xnf.config.SimulatorConfiguration
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ReplayProcessor
import reactor.ipc.netty.NettyOutbound
import reactor.ipc.netty.tcp.TcpClient


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class XnfSimulator(private val configuration: SimulatorConfiguration) {

    private val client: TcpClient = TcpClient.builder()
            .options { opts ->
                opts.host(configuration.vesHost)
                        .port(configuration.vesPort)
                        .sslContext(createSslContext(configuration.security))
            }
            .build()

    fun sendIo(messages: Flux<PayloadWireFrameMessage>) = IO<Unit> {
        sendRx(messages).block()
    }

    fun sendRx(messages: Flux<PayloadWireFrameMessage>): Mono<Void> {
        val complete = ReplayProcessor.create<Void>(1)
        client
                .newHandler { _, output -> handler(complete, messages, output) }
                .doOnError {
                    logger.info("Failed to connect to VesHvCollector on " +
                            "${configuration.vesHost}:${configuration.vesPort}")
                }
                .subscribe {
                    logger.info("Connected to VesHvCollector on " +
                            "${configuration.vesHost}:${configuration.vesPort}")
                }
        return complete.then()
    }

    private fun handler(complete: ReplayProcessor<Void>,
                        messages: Flux<PayloadWireFrameMessage>,
                        nettyOutbound: NettyOutbound):
            Publisher<Void> {
        val allocator = nettyOutbound.alloc()
        val encoder = WireFrameEncoder(allocator)
        val frames = messages
                .map(encoder::encode)
                .window(MAX_BATCH_SIZE)

        return nettyOutbound
                .logConnectionClosed()
                .options { it.flushOnBoundary() }
                .sendGroups(frames)
                .send(Mono.just(allocator.buffer().writeByte(eotMessageByte.toInt())))
                .then {
                    logger.info("Messages have been sent")
                    complete.onComplete()
                }
                .then()
    }

    private fun createSslContext(config: SecurityConfiguration): SslContext =
            SslContextBuilder.forClient()
                    .keyManager(config.cert.toFile(), config.privateKey.toFile())
                    .trustManager(config.trustedCert.toFile())
                    .sslProvider(SslProvider.OPENSSL)
                    .clientAuth(ClientAuth.REQUIRE)
                    .build()

    private fun NettyOutbound.logConnectionClosed(): NettyOutbound {
        context().onClose {
            logger.info { "Connection to ${context().address()} has been closed" }
        }
        return this
    }

    companion object {
        private const val MAX_BATCH_SIZE = 128
        private const val eotMessageByte = EndOfTransmissionMessage.MARKER_BYTE
        private val logger = Logger(XnfSimulator::class)
    }
}