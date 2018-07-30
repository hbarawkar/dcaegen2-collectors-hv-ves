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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.config

import arrow.core.identity
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.utils.commandline.WrongArgumentError


internal class ArgDcaeAppSimulatorConfigurationTest : Spek({

    lateinit var cut: ArgDcaeAppSimConfiguration
    val listenPort = "1234"
    val kafkaBootstrapServers = "localhosting:123,localhostinger:12345"
    val kafkaTopics = "top1,top2"

    beforeEachTest {
        cut = ArgDcaeAppSimConfiguration()
    }

    fun parseExpectingSuccess(vararg cmdLine: String): DcaeAppSimConfiguration =
            cut.parse(cmdLine).fold(
                    { throw AssertionError("Parsing result should be present") },
                    ::identity
            )

    fun parseExpectingFailure(vararg cmdLine: String) =
            cut.parse(cmdLine).fold(
                    ::identity,
                    { throw AssertionError("parsing should have failed") }
            )

    describe("parsing arguments") {
        lateinit var result: DcaeAppSimConfiguration

        given("all parameters are present in the long form") {

            beforeEachTest {
                result = parseExpectingSuccess(
                        "--listen-port", listenPort,
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "--kafka-topics", kafkaTopics
                )
            }

            it("should set proper port") {
                assertThat(result.apiPort).isEqualTo(listenPort.toInt())
            }


            it("should set proper kafka bootstrap servers") {
                assertThat(result.kafkaBootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            it("should set proper kafka topics") {
                assertThat(result.kafkaTopics).isEqualTo(
                        setOf("top1", "top2")
                )
            }
        }

        given("some parameters are present in the short form") {

            beforeEachTest {
                result = parseExpectingSuccess(
                        "-p", listenPort,
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "-f", kafkaTopics)
            }

            it("should set proper port") {
                assertThat(result.apiPort).isEqualTo(listenPort.toInt())
            }

            it("should set proper kafka bootstrap servers") {
                assertThat(result.kafkaBootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            it("should set proper kafka topics") {
                assertThat(result.kafkaTopics).isEqualTo(
                        setOf("top1", "top2")
                )
            }
        }

        describe("required parameter is absent") {
            given("kafka topics are missing") {
                it("should throw exception") {
                    assertThat(parseExpectingFailure(
                            "-p", listenPort,
                            "-s", kafkaBootstrapServers
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }

            given("kafka bootstrap servers is missing") {
                it("should throw exception") {
                    assertThat(parseExpectingFailure(
                            "-p", listenPort,
                            "-f", kafkaTopics
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }

            given("listen port is missing") {
                it("should throw exception") {
                    assertThat(parseExpectingFailure(
                            "-p", kafkaTopics,
                            "-s", kafkaBootstrapServers
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }
        }
    }
})