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

import com.devsoap.vaadinflow.actions.GrettyPluginAction
import com.devsoap.vaadinflow.actions.NodePluginAction
import com.devsoap.vaadinflow.actions.PluginAction
import com.devsoap.vaadinflow.actions.SpringBootAction
import com.devsoap.vaadinflow.actions.VaadinFlowPluginAction
import com.devsoap.vaadinflow.actions.WarPluginAction
import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import com.devsoap.vaadinflow.tasks.ConvertCssToHtmlStyleTask
import com.devsoap.vaadinflow.tasks.ConvertGroovyTemplatesToHTML
import com.devsoap.vaadinflow.tasks.CreateComponentTask
import com.devsoap.vaadinflow.tasks.CreateCompositeTask
import com.devsoap.vaadinflow.tasks.CreateProjectTask
import com.devsoap.vaadinflow.tasks.CreateWebComponentTask
import com.devsoap.vaadinflow.tasks.CreateWebTemplateTask
import com.devsoap.vaadinflow.tasks.InstallBowerDependenciesTask
import com.devsoap.vaadinflow.tasks.InstallYarnDependenciesTask
import com.devsoap.vaadinflow.tasks.TranspileDependenciesTask
import com.devsoap.vaadinflow.tasks.VersionCheckTask
import com.devsoap.vaadinflow.util.Versions
import groovy.util.logging.Log
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.invocation.Gradle
import org.gradle.internal.reflect.Instantiator
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.VersionNumber

import javax.inject.Inject

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

    @Inject
    VaadinFlowPlugin(Gradle gradle, Instantiator instantiator) {
        validateGradleVersion(gradle)

        actions << instantiator.newInstance(VaadinFlowPluginAction)
        actions << instantiator.newInstance(NodePluginAction)
        actions << instantiator.newInstance(WarPluginAction)
        actions << instantiator.newInstance(GrettyPluginAction)
        actions << instantiator.newInstance(SpringBootAction)
    }

    @Override
    void apply(Project project) {
        // FeaturePreviews was only introduced in Gradle 4.6 so if we import it the plugin will fail in a
        // cryptic error message (#108). Instead we use dynamic imports allowing the constructor to run and throw
        // a readable exception so users understand to update their Gradle version.
        org.gradle.api.internal.FeaturePreviews settings =
                project.gradle.services.get(org.gradle.api.internal.FeaturePreviews)

        project.with {

            actions.each { action ->
                action.apply(project)
            }

            extensions.with {
                create(VaadinFlowPluginExtension.NAME, VaadinFlowPluginExtension, project, settings)
                create(VaadinClientDependenciesExtension.NAME, VaadinClientDependenciesExtension, project)
            }

            tasks.with {
                create(CreateProjectTask.NAME, CreateProjectTask)
                create(CreateWebComponentTask.NAME, CreateWebComponentTask)
                create(InstallYarnDependenciesTask.NAME, InstallYarnDependenciesTask)
                create(InstallBowerDependenciesTask.NAME, InstallBowerDependenciesTask)
                create(TranspileDependenciesTask.NAME, TranspileDependenciesTask)
                create(AssembleClientDependenciesTask.NAME, AssembleClientDependenciesTask)
                create(ConvertCssToHtmlStyleTask.NAME, ConvertCssToHtmlStyleTask)
                create(CreateCompositeTask.NAME, CreateCompositeTask)
                create(CreateComponentTask.NAME, CreateComponentTask)
                create(CreateWebTemplateTask.NAME, CreateWebTemplateTask)
                create(ConvertGroovyTemplatesToHTML.NAME, ConvertGroovyTemplatesToHTML)
                create(VersionCheckTask.NAME, VersionCheckTask)
            }

            afterEvaluate {
                disableStatistics(project)
            }
        }
    }

    private static void disableStatistics(Project project) {
        VaadinFlowPluginExtension vaadin = project.extensions[VaadinFlowPluginExtension.NAME]
        if (!vaadin.submitStatistics) {
            Dependency statistics = vaadin.disableStatistics()
            project.configurations['implementation'].dependencies.add(statistics)
            project.configurations.all { config ->
                config.resolutionStrategy.force("${statistics.group}:${statistics.name}:${statistics.version}")
            }
        }
    }

    private static void validateGradleVersion(Gradle gradle) {
        VersionNumber version = VersionNumber.parse(gradle.gradleVersion)
        VersionNumber requiredVersion = Versions.version('vaadin.plugin.gradle.version')
        if ( version.baseVersion < requiredVersion ) {
            throw new UnsupportedVersionException("Your gradle version ($version) is too old. " +
                    "Plugin requires Gradle $requiredVersion+")
        }
    }
}
