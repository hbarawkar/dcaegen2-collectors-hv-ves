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
package org.onap.dcae.collectors.veshv.tests.fakes

import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import reactor.core.publisher.Flux
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class StoringSink : Sink {
    private val sent: Deque<RoutedMessage> = ConcurrentLinkedDeque()

    val sentMessages: List<RoutedMessage>
        get() = sent.toList()

    override fun send(messages: Flux<RoutedMessage>): Flux<RoutedMessage> {
        return messages.doOnNext(sent::addLast)
    }
}

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class CountingSink : Sink {
    private val atomicCount = AtomicLong(0)

    val count: Long
        get() = atomicCount.get()

    override fun send(messages: Flux<RoutedMessage>): Flux<RoutedMessage> {
        return messages.doOnNext {
            atomicCount.incrementAndGet()
        }
    }
}

class NoOpSink : Sink {
    override fun send(messages: Flux<RoutedMessage>): Flux<RoutedMessage> = messages
}