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
package com.devsoap.vaadinflow.models

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import groovy.transform.Memoized
import org.gradle.api.Project

/**
 * Project language type
 *
 * @author John Ahlroos
 * @since 1.0
 */
enum ProjectType {

    /**
     * Project with Java plugin applied
     */
    JAVA,

    /**
     * Project with Groovy plugin applied
     */
    GROOVY,

    /**
     * Project with Kotlin plugin applied
     */
    KOTLIN

    /**
     * Get the project type for a project
     *
     * @param project
     *      the project to get the project type for
     */
    static ProjectType get(Project project) {
        if (project.plugins.findPlugin('groovy')) {
            return GROOVY
        } else if (project.plugins.findPlugin('org.jetbrains.kotlin.jvm')) {
            return KOTLIN
        }
        JAVA
    }

    /**
     * Get the file extension of source file of this type
     */
    String getExtension() {
        if (this == KOTLIN) {
            return 'kt'
        }
        this.name().toLowerCase()
    }

    /**
     * Get the source directory of this type of project
     */
    String getSourceDir() {
        this.name().toLowerCase()
    }

    /**
     * Get the file extension for the web template
     */
    String getTemplateExtension(boolean compatibilityMode) {
        if (this == GROOVY) {
            return 'tpl'
        }
        compatibilityMode ? 'html' : 'js'
    }
}
