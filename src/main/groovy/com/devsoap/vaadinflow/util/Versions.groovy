/*
 * Copyright 2018 Devsoap Inc.
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

import groovy.transform.Memoized
import org.gradle.util.VersionNumber

/**
 * Class for resolving application and library versions
 *
 * @author John Ahlroos
 * @since 1.0
 */
class Versions {

    private Versions() { }

    /**
     * Get a version for a key
     *
     * @param key
     *      the key to get the version for
     * @return
     *      the version number. If not found VersionNumber.UNKNOWN is returned.
     */
    @Memoized
    static final VersionNumber version(String key) {
        Properties properties = new Properties()
        Versions.getResourceAsStream( '/versions.properties' ).with {
            properties.load(it)
        }
        if (properties.get(key)) {
            return VersionNumber.parse(properties.get(key).toString())
        }
        VersionNumber.UNKNOWN
    }
}
