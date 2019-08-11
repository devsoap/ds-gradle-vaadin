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
package com.devsoap.vaadinflow.actions

import com.devsoap.vaadinflow.VaadinFlowPlugin
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import com.devsoap.vaadinflow.tasks.ConvertCssToHtmlStyleTask
import com.devsoap.vaadinflow.tasks.ConvertGroovyTemplatesToHTML
import com.devsoap.vaadinflow.tasks.InstallBowerDependenciesTask
import com.devsoap.vaadinflow.tasks.InstallYarnDependenciesTask
import com.devsoap.vaadinflow.tasks.VersionCheckTask
import com.devsoap.vaadinflow.util.Versions
import com.devsoap.vaadinflow.util.WebJarHelper
import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
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

    private static final String PROCESS_RESOURCES = 'processResources'

    @Override
    void apply(Project project) {
        super.apply(project)
        project.plugins.apply('java')
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)
        project.with {
            tasks[PROCESS_RESOURCES].with {
                dependsOn(VersionCheckTask.NAME)
                dependsOn(ConvertCssToHtmlStyleTask.NAME)
                dependsOn(ConvertGroovyTemplatesToHTML.NAME)
            }

            tasks['jar'].dependsOn(AssembleClientDependenciesTask.NAME)

            repositories.maven { repository ->
                repository.name = 'Gradle Plugin Portal'
                repository.url = 'https://plugins.gradle.org/m2/'
            }

            String pluginDependency =
                    "com.devsoap:gradle-vaadin-flow-plugin:${Versions.rawVersion('vaadin.plugin.version')}"
            Dependency vaadin = dependencies.create(pluginDependency) {
                description = 'Gradle Vaadin Plugin'
            }
            configurations['compileOnly'].dependencies.add(vaadin)
        }
    }

    @Override
    protected void executeAfterAllEvaluations() {
        super.executeAfterAllEvaluations()
        WebJarHelper.findDependantJarTasks(project).each {
            project.tasks[InstallYarnDependenciesTask.NAME].dependsOn(it)
            project.tasks[InstallBowerDependenciesTask.NAME].dependsOn(it)
        }
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)
        VersionPrinter.instance.printIfNotPrintedBefore(project)
        VaadinFlowPluginExtension vaadin = project.extensions['vaadin']
        if (!vaadin.versionSet) {
            LOGGER.warning('vaadin.version is not set, falling back to latest Vaadin version')
        }

        if (vaadin.submitStatisticsUnset) {
            LOGGER.warning('Allow Vaadin to gather usage statistics by setting ' +
                    'vaadin.submitStatistics=true (hide this message by setting it to false)')
        }

        if (vaadin.compatibilityMode) {
            LOGGER.warning(
                    'The project will be compiled for Vaadin 13 (Flow 1) compatibility mode. ' +
                    'To disable compatibility mode set vaadin.compatibilityMode=false.')
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
