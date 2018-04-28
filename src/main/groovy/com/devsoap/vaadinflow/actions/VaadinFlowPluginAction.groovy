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
package com.devsoap.vaadinflow.actions

import com.devsoap.vaadinflow.VaadinFlowPlugin
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import com.devsoap.vaadinflow.util.Versions
import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

/**
 * Action taken when the Vaadin plugin is applied to a project
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinFlowPluginAction extends PluginAction {

    final String pluginId = VaadinFlowPlugin.PLUGIN_ID

    @Override
    void apply(Project project) {
        super.apply(project)
        project.plugins.apply('java')
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)
        project.tasks['processResources'].dependsOn(AssembleClientDependenciesTask.NAME)
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)
        VersionPrinter.instance.printIfNotPrintedBefore(project)
        VaadinFlowPluginExtension vaadin = project.extensions['vaadin']
        if (!vaadin.version) {
            LOGGER.warning('vaadin.version is not set, falling back to latest Vaadin version')
        }
    }

    @Singleton(lazy = false, strict = true)
    private static class VersionPrinter {
        private Gradle gradle
        void printIfNotPrintedBefore(Project project) {
            if (project.gradle == gradle) {
                return
            }
            gradle = project.gradle
            String version = Versions.version('vaadin.plugin.version')
            project.logger.quiet "Using Gradle Vaadin Flow Plugin $version"
        }
    }
}
