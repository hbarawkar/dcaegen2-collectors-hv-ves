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
package org.onap.dcae.collectors.veshv.utils.commandline

import arrow.core.Option
import arrow.core.getOrElse
import arrow.effects.IO
import arrow.syntax.function.curried
import org.apache.commons.cli.CommandLine
import org.onap.dcae.collectors.veshv.utils.arrow.ExitFailure
import org.onap.dcae.collectors.veshv.utils.arrow.fromNullablesChain

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */

fun handleWrongArgumentError(programName: String, err: WrongArgumentError): IO<Unit> = IO {
    err.printMessage()
    err.printHelp(programName)
}.flatMap { ExitFailure(2).io() }

val handleWrongArgumentErrorCurried = ::handleWrongArgumentError.curried()

fun CommandLine.longValue(cmdLineOpt: CommandLineOption, default: Long): Long =
        longValue(cmdLineOpt).getOrElse { default }

fun CommandLine.stringValue(cmdLineOpt: CommandLineOption, default: String): String =
        optionValue(cmdLineOpt).getOrElse { default }

fun CommandLine.intValue(cmdLineOpt: CommandLineOption, default: Int): Int =
        intValue(cmdLineOpt).getOrElse { default }

fun CommandLine.intValue(cmdLineOpt: CommandLineOption): Option<Int> =
        optionValue(cmdLineOpt).map(String::toInt)

fun CommandLine.longValue(cmdLineOpt: CommandLineOption): Option<Long> =
        optionValue(cmdLineOpt).map(String::toLong)

fun CommandLine.stringValue(cmdLineOpt: CommandLineOption): Option<String> =
        optionValue(cmdLineOpt)

fun CommandLine.hasOption(cmdLineOpt: CommandLineOption): Boolean =
        this.hasOption(cmdLineOpt.option.opt) ||
                System.getenv(cmdLineOpt.environmentVariableName()) != null

private fun CommandLine.optionValue(cmdLineOpt: CommandLineOption) = Option.fromNullablesChain(
        getOptionValue(cmdLineOpt.option.opt),
        { System.getenv(cmdLineOpt.environmentVariableName()) })