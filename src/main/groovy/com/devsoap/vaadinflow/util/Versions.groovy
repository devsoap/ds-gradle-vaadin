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

import groovy.util.logging.Log
import org.gradle.util.VersionNumber

/**
 * Class for resolving application and library versions
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
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
    static final VersionNumber version(String key) {
        String versionString = rawVersion(key)
        versionString != null ? VersionNumber.parse(versionString) : VersionNumber.UNKNOWN
    }

    /**
     * Get a raw version string for a key
     *
     * @param key
     *      the key to get the version for
     * @return
     *      the raw version number as it has been defined in the properties file
     */
    static final String rawVersion(String key) {
        Properties properties = new Properties()
        Versions.getResourceAsStream( '/versions.properties' ).with {
            properties.load(it)
        }
        if (properties.get(key)) {
            return properties.get(key).toString()
        }
        LOGGER.warning("Failed to find key $key in versions.properties")
        null
    }
}
