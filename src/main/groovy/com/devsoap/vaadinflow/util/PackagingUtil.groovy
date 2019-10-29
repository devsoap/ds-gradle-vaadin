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

import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

import java.nio.file.Paths

/**
 * Configures the Spring Boot plugin to support the Vaadin resources
 *
 * @author John Ahlroos
 * @since 1.1
 */
class PackagingUtil {

    private static final String WEBAPP_GEN = 'webapp-gen'

    /**
     * Includes static resources in the Jar output
     *
     * @param task
     *      the Jar task to add the resources to
     */
    static void includeResourcesInJar(Jar jar, String resourcesPath) {

        Project project = jar.project

        // Include web-app dirs
        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
        File webappDir = assembleTask.webappDir
        jar.from(webappDir) { CopySpec spec ->
            spec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            spec.into(resourcesPath)
        }

        // Include web-app gen
        File webappGen = Paths.get(project.buildDir.canonicalPath, WEBAPP_GEN).toFile()
        jar.from(webappGen) { CopySpec spec ->
            spec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            spec.into(resourcesPath)
        }

        // Copy static frontend files into compilation result
        VaadinFlowPluginExtension vaadin = jar.project.extensions.getByType(VaadinFlowPluginExtension)
        if (vaadin.productionMode) {
            File frontend = new File(webappDir, 'frontend')
            if (frontend.exists()) {
                jar.from(frontend) { CopySpec spec ->
                    spec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    spec.into(resourcesPath + '/frontend-es5')
                }
                jar.from(frontend) { CopySpec spec ->
                    spec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    spec.into(resourcesPath + '/frontend-es6')
                }
            }
        }
    }
}
