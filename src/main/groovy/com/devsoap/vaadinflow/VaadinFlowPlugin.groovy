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
import com.devsoap.vaadinflow.util.Versions
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
    }

    private static void validateGradleVersion(Project project) {
        Gradle gradle = project.gradle
        VersionNumber version = VersionNumber.parse(gradle.gradleVersion)
        VersionNumber requiredVersion = Versions.version('vaadin.plugin.gradle.version')
        println(requiredVersion)
        if ( version.baseVersion < requiredVersion ) {
            throw new UnsupportedVersionException("Your gradle version ($version) is too old. " +
                    "Plugin requires Gradle $requiredVersion+")
        }
    }
}
