/*
 * Copyright 2018-2020 Devsoap Inc.
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
package com.devsoap.vaadinflow.models

import groovy.transform.Memoized
import org.gradle.api.Project

/**
 * The type of application to generate
 *
 * @author John Ahlroos
 * @since 1.0
 */
enum ApplicationType {

    /**
     * Standard deployable web application (WAR)
     */
    WEB_APPLICATION,

    /**
     * Spring Boot Application
     */
    SPRING_BOOT

    /**
     * Get the application type for a project
     *
     * @param project
     *      the project to get the type for
     */
    static ApplicationType get(Project project) {
        if (project.plugins.findPlugin('org.springframework.boot')) {
            return SPRING_BOOT
        }
        WEB_APPLICATION
    }
}
