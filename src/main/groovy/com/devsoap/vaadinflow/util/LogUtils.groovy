/*
 * Copyright 2018-2019 Devsoap Inc.
 *
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
 */
package com.devsoap.vaadinflow.util

import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.MapConstructor
import groovy.transform.Memoized
import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.internal.io.LineBufferingOutputStream
import org.gradle.internal.io.TextStream

import javax.annotation.Nullable
import java.nio.charset.StandardCharsets
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

    static final void printIfNotPrintedBefore(Project project, String message) {
        SingletonPrinter.instance.printIfNotPrintedBefore(project, message)
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

        void printIfNotPrintedBefore(Project project, String message) {
            messageStatus.putIfAbsent(message, true)
            if ( messageStatus[message] ) {
                project.logger.quiet(message)
                messageStatus[message] = false
            }
        }
    }

}
