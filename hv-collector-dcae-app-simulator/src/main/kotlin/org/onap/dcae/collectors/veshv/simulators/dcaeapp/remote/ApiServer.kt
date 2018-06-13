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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.remote

import com.google.protobuf.util.JsonFormat
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka.ConsumerStateProvider
import org.onap.ves.VesEventV5.VesEvent
import ratpack.handling.Chain
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import reactor.core.publisher.Mono

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class ApiServer(private val consumerState: ConsumerStateProvider) {
    private val jsonPrinter = JsonFormat.printer()

    fun start(port: Int): Mono<RatpackServer> = Mono.fromCallable {
        RatpackServer.of { server ->
            server.serverConfig(ServerConfig.embedded().port(port))
                    .handlers(this::setupHandlers)
        }
    }.doOnNext(RatpackServer::start)

    private fun setupHandlers(chain: Chain) {
        chain
                .get("messages/count") { ctx ->
                    ctx.response.contentType(CONTENT_TEXT)
                    consumerState.currentState()
                            .map { it.msgCount.toString() }
                            .subscribe(ctx.response::send)
                }

                .get("messages/last/key") { ctx ->
                    ctx.response.contentType(CONTENT_JSON)
                    consumerState.currentState()
                            .map { it.lastKey }
                            .map { VesEvent.CommonEventHeader.parseFrom(it) }
                            .map { jsonPrinter.print(it) }
                            .subscribe(ctx.response::send)
                }

                .get("messages/last/value") { ctx ->
                    ctx.response.contentType(CONTENT_JSON)
                    consumerState.currentState()
                            .map { it.lastValue }
                            .map { VesEvent.parseFrom(it) }
                            .map { jsonPrinter.print(it) }
                            .subscribe(ctx.response::send)
                }
    }

    companion object {
        private const val CONTENT_TEXT = "text/plain"
        private const val CONTENT_JSON = "application/json"
    }
}