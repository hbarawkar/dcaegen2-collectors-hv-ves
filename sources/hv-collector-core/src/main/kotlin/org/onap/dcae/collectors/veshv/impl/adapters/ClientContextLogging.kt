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

import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.utils.logging.AtLevelLogger
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.handleReactiveStreamError
import reactor.core.publisher.Flux

@Suppress("TooManyFunctions")
internal object ClientContextLogging {
    fun Logger.withError(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withError(ctx::asMap, block)
    fun Logger.withWarn(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withWarn(ctx::asMap, block)
    fun Logger.withInfo(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withInfo(ctx::asMap, block)
    fun Logger.withDebug(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withDebug(ctx::asMap, block)
    fun Logger.withTrace(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withTrace(ctx::asMap, block)

    fun Logger.error(ctx: ClientContext, message: () -> String) = error(ctx::asMap, message)
    fun Logger.warn(ctx: ClientContext, message: () -> String) = warn(ctx::asMap, message)
    fun Logger.info(ctx: ClientContext, message: () -> String) = info(ctx::asMap, message)
    fun Logger.debug(ctx: ClientContext, message: () -> String) = debug(ctx::asMap, message)
    fun Logger.trace(ctx: ClientContext, message: () -> String) = trace(ctx::asMap, message)

    fun <T> Logger.handleReactiveStreamError(context: ClientContext, ex: Throwable,
                                             returnFlux: Flux<T> = Flux.empty()): Flux<T> {
        return this.handleReactiveStreamError({ context.asMap() }, ex, returnFlux)
    }
}

