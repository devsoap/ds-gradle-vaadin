/*
 * Copyright 2018-2019 Devsoap Inc.
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0
 * International Public License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      https://creativecommons.org/licenses/by-nd/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.vaadinflow.util

import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.internal.io.LineBufferingOutputStream
import org.gradle.internal.io.TextStream

import javax.annotation.Nullable
import java.util.logging.Level

/**
 * Utilities for logging
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class LogUtils {

    static final OutputStream getLogOutputStream(Level level) {
        new LineBufferingOutputStream(new LogStream(level))
    }

    static final <T> T measureTime(String description, Closure<T> closure) {
        Date start = new Date()
        T result = closure.call()
        Date end = new Date()
        TimeDuration duration = TimeCategory.minus(end, start)
        LOGGER.info("$description. It took $duration")
        result
    }

    static final void printIfNotPrintedBefore(Project project, String message, boolean licensed) {
        SingletonPrinter.instance.printIfNotPrintedBefore(project, message, licensed)
    }

    @Log('LOGGER')
    private static class LogStream implements TextStream {

        private final Level level

        LogStream(Level level) {
            this.level = level
        }

        @Override
        void text(String text) {
            String trimmedText = text.replaceAll(/\s*$/, '')
            if (!trimmedText.blank) {
                LOGGER.log(level, trimmedText)
            }
        }

        @Override
        void endOfStream(@Nullable Throwable failure) {
            if (failure) {
                LOGGER.log(Level.SEVERE, '', failure)
            }
        }
    }

    @Singleton(lazy = false, strict = true)
    private static class SingletonPrinter {

        private final Map<String, Boolean> messageStatus = [:]

        void printIfNotPrintedBefore(Project project, String message, boolean licensed) {
            messageStatus.putIfAbsent(message, true)
            if ( messageStatus[message] ) {
                if (!licensed) {
                    project.logger.quiet(message)
                } else if (project.logger.lifecycleEnabled) {
                    project.logger.lifecycle(message)
                }
                messageStatus[message] = false
            }
        }
    }

}
