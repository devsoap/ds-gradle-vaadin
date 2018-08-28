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
package com.devsoap.vaadinflow.models

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
     *      the project to get the project type fore
     */
    @Memoized
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
    String getTemplateExtension() {
        if (this == GROOVY) {
            return 'tpl'
        }
        'html'
    }
}
