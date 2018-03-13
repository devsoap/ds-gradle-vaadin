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
package com.devsoap.vaadinflow

import com.devsoap.vaadinflow.actions.PluginAction
import com.devsoap.vaadinflow.actions.VaadinFlowPluginAction
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.CreateProjectTask
import com.devsoap.vaadinflow.util.Versions
import groovy.util.logging.Log
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.VersionNumber

/**
 * Main plugin class
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinFlowPlugin implements Plugin<Project> {

    static final String PLUGIN_ID = 'com.devsoap.vaadin-flow'

    private final List<PluginAction> actions = []

    VaadinFlowPlugin() {
        actions << new VaadinFlowPluginAction()
    }

    @Override
    void apply(Project project) {
        validateGradleVersion(project)
        actions.each { it.apply(project) }

        project.extensions.create(VaadinFlowPluginExtension.NAME, VaadinFlowPluginExtension, project)

        project.tasks.create(CreateProjectTask.NAME, CreateProjectTask)

        addServletApi(project)

        workaroundInvalidBomVersionRanges(project)
    }

    private static void addServletApi(Project project) {
        project.configurations.getByName('compileOnly').defaultDependencies { dependencies ->
            String version = Versions.rawVersion('servlet.version')
            dependencies.add(project.dependencies.create("javax.servlet:javax.servlet-api:$version"))
        }
    }

    /**
     * Looks like vaadins BOM is using invalid version ranges which do not account for alpha/beta releases.
     * Workaround it here by using the '+' notation.
     *
     * FIXME This is a ugly hack that should be removed once Vaadin gets its BOMs fixed.
     *
     * @param project
     *      the project
     */
    private static void workaroundInvalidBomVersionRanges(Project project) {
        project.configurations.all {
            it.resolutionStrategy.eachDependency { dep ->
                if (dep.requested.group == 'org.webjars.bowergithub.vaadin' && dep.requested.version.startsWith('[')) {
                    // Convert version range [1.2.3,4) -> 3+
                    int maxVersion = Integer.parseInt(dep.requested.version.split(',')[1] - ')')
                    String baseVersion = (maxVersion - 1) + '+'
                    dep.useVersion(baseVersion)
                }
            }
        }
    }

    private static void validateGradleVersion(Project project) {
        Gradle gradle = project.gradle
        VersionNumber version = VersionNumber.parse(gradle.gradleVersion)
        VersionNumber requiredVersion = Versions.version('vaadin.plugin.gradle.version')
        if ( version.baseVersion < requiredVersion ) {
            throw new UnsupportedVersionException("Your gradle version ($version) is too old. " +
                    "Plugin requires Gradle $requiredVersion+")
        }
    }
}
