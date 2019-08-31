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
package com.devsoap.vaadinflow.extensions

import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * Devsoap properties
 *
 * @author John Ahlroos
 * @since 1.3
 */
class DevsoapExtension {

    static final String NAME = 'devsoap'

    private final Property<String> email
    private final Property<String> key

    /**
     * Creates the devsoap extension
     *
     * @param project
     *      the project to create the extension for
     */
    DevsoapExtension(Project project) {
        email = project.objects.property(String)
        key = project.objects.property(String)
    }

    /**
     * The license email
     *
     */
    String getEmail() {
        email.getOrElse(System.getProperty('devsoap.gradle-flow-plugin.license.key'))
    }

    /**
     * Set the license email
     *
     * @param email
     *      The email address you registered the product with
     */
    void setEmail(String email) {
        this.email.set(email)
    }

    /**
     * The license key
     */
    String getKey() {
        key.getOrElse(System.getProperty('devsoap.gradle-flow-plugin.license.email'))
    }

    /**
     * Set the license key
     *
     * @param key
     *     the license key you recieved via registration
     */
    void setKey(String key) {
        this.key.set(key)
    }
}
