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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl

import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.FAULT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.test.test
import kotlin.test.assertTrue

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
object MessageGeneratorImplTest : Spek({
    describe("message factory") {
        val maxPayloadSizeBytes = 1024
        val generator = MessageGeneratorImpl(PayloadGenerator(), maxPayloadSizeBytes)
        given("single message parameters") {

            on("messages amount not specified in parameters") {
                it("should create infinite flux") {
                    val limit = 1000L
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(PERF3GPP),
                                    MessageType.VALID
                            )))
                            .take(limit)
                            .test()
                            .expectNextCount(limit)
                            .verifyComplete()
                }
            }

            on("messages amount = 0 specified in parameters") {
                it("should create empty message flux") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(PERF3GPP),
                                    MessageType.VALID,
                                    0
                            )))
                            .test()
                            .verifyComplete()
                }
            }

            on("messages amount specified in parameters") {
                it("should create message flux of specified size") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(PERF3GPP),
                                    MessageType.VALID,
                                    5
                            )))
                            .test()
                            .expectNextCount(5)
                            .verifyComplete()
                }
            }

            on("message type requesting valid message") {
                it("should create flux of valid messages with given domain") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(FAULT),
                                    MessageType.VALID,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertTrue(it.validate().isRight())
                                assertThat(it.payloadSize).isLessThan(maxPayloadSizeBytes)
                                assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(FAULT.domainName)
                            }
                            .verifyComplete()
                }
            }

            on("message type requesting too big payload") {
                it("should create flux of messages with given domain and payload exceeding threshold") {

                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(PERF3GPP),
                                    MessageType.TOO_BIG_PAYLOAD,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertTrue(it.validate().isRight())
                                assertThat(it.payloadSize).isGreaterThan(maxPayloadSizeBytes)
                                assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(PERF3GPP.domainName)
                            }
                            .verifyComplete()
                }
            }

            on("message type requesting invalid GPB data ") {
                it("should create flux of messages with invalid payload") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(PERF3GPP),
                                    MessageType.INVALID_GPB_DATA,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertTrue(it.validate().isRight())
                                assertThat(it.payloadSize).isLessThan(maxPayloadSizeBytes)
                                assertThatExceptionOfType(InvalidProtocolBufferException::class.java)
                                        .isThrownBy { extractCommonEventHeader(it.payload) }
                            }
                            .verifyComplete()
                }
            }

            on("message type requesting invalid wire frame ") {
                it("should create flux of messages with invalid version") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(PERF3GPP),
                                    MessageType.INVALID_WIRE_FRAME,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertTrue(it.validate().isLeft())
                                assertThat(it.payloadSize).isLessThan(maxPayloadSizeBytes)
                                assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(PERF3GPP.domainName)
                                assertThat(it.versionMajor).isNotEqualTo(WireFrameMessage.SUPPORTED_VERSION_MINOR)
                            }
                            .verifyComplete()
                }
            }

            on("message type requesting fixed payload") {
                it("should create flux of valid messages with fixed payload") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(FAULT),
                                    MessageType.FIXED_PAYLOAD,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertTrue(it.validate().isRight())
                                assertThat(it.payloadSize).isLessThan(maxPayloadSizeBytes)
                                assertThat(extractEventFields(it.payload).size()).isEqualTo(MessageGenerator.FIXED_PAYLOAD_SIZE)
                                assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(FAULT.domainName)
                            }
                            .verifyComplete()
                }
            }
        }
        given("list of message parameters") {
            it("should create concatenated flux of messages") {
                val singleFluxSize = 5L
                val messageParameters = listOf(
                        MessageParameters(commonHeader(PERF3GPP), MessageType.VALID, singleFluxSize),
                        MessageParameters(commonHeader(FAULT), MessageType.TOO_BIG_PAYLOAD, singleFluxSize),
                        MessageParameters(commonHeader(HEARTBEAT), MessageType.VALID, singleFluxSize)
                )
                generator.createMessageFlux(messageParameters)
                        .test()
                        .assertNext {
                            assertThat(it.payloadSize).isLessThan(maxPayloadSizeBytes)
                            assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(PERF3GPP.domainName)
                        }
                        .expectNextCount(singleFluxSize - 1)
                        .assertNext {
                            assertThat(it.payloadSize).isGreaterThan(maxPayloadSizeBytes)
                            assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(FAULT.domainName)
                        }
                        .expectNextCount(singleFluxSize - 1)
                        .assertNext {
                            assertThat(it.payloadSize).isLessThan(maxPayloadSizeBytes)
                            assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(HEARTBEAT.domainName)
                        }
                        .expectNextCount(singleFluxSize - 1)
                        .verifyComplete()
            }
        }
    }
})

fun extractCommonEventHeader(bytes: ByteData): CommonEventHeader =
        VesEvent.parseFrom(bytes.unsafeAsArray()).commonEventHeader


fun extractEventFields(bytes: ByteData): ByteString =
        VesEvent.parseFrom(bytes.unsafeAsArray()).eventFields
