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
package com.devsoap.vaadinflow

import com.devsoap.vaadinflow.actions.GrettyDeprecatedPluginAction
import com.devsoap.vaadinflow.actions.GrettyPluginAction
import com.devsoap.vaadinflow.actions.NodePluginAction
import com.devsoap.vaadinflow.actions.PluginAction
import com.devsoap.vaadinflow.actions.SassJavaPluginAction
import com.devsoap.vaadinflow.actions.SassWarPluginAction
import com.devsoap.vaadinflow.actions.SpringBootAction
import com.devsoap.vaadinflow.actions.VaadinFlowPluginAction
import com.devsoap.vaadinflow.actions.WarPluginAction
import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import com.devsoap.vaadinflow.tasks.WrapCssTask
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
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
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

    private static final String COMPILE_CONFIGURATION = 'compile'

    private final List<PluginAction> actions = []

    @Inject
    VaadinFlowPlugin(Gradle gradle, Instantiator instantiator) {
        validateGradleVersion(gradle)

        actions << instantiator.newInstance(VaadinFlowPluginAction)
        actions << instantiator.newInstance(NodePluginAction)
        actions << instantiator.newInstance(WarPluginAction)
        actions << instantiator.newInstance(GrettyDeprecatedPluginAction)
        actions << instantiator.newInstance(GrettyPluginAction)
        actions << instantiator.newInstance(SpringBootAction)
        actions << instantiator.newInstance(SassJavaPluginAction)
        actions << instantiator.newInstance(SassWarPluginAction)
    }

    @Override
    void apply(Project project) {
        project.with {

            actions.each { action ->
                action.apply(project)
            }

            extensions.with {
                create(VaadinFlowPluginExtension.NAME, VaadinFlowPluginExtension, project)
                create(VaadinClientDependenciesExtension.NAME, VaadinClientDependenciesExtension, project)
            }

            tasks.with {
                register(CreateProjectTask.NAME, CreateProjectTask)
                register(CreateWebComponentTask.NAME, CreateWebComponentTask)
                register(InstallYarnDependenciesTask.NAME, InstallYarnDependenciesTask)
                register(InstallBowerDependenciesTask.NAME, InstallBowerDependenciesTask)
                register(TranspileDependenciesTask.NAME, TranspileDependenciesTask)
                register(AssembleClientDependenciesTask.NAME, AssembleClientDependenciesTask)
                register(WrapCssTask.NAME, WrapCssTask)
                register(CreateCompositeTask.NAME, CreateCompositeTask)
                register(CreateComponentTask.NAME, CreateComponentTask)
                register(CreateWebTemplateTask.NAME, CreateWebTemplateTask)
                register(ConvertGroovyTemplatesToHTML.NAME, ConvertGroovyTemplatesToHTML)
                register(VersionCheckTask.NAME, VersionCheckTask)
            }

            afterEvaluate {
                disableStatistics(project)
                enableProductionMode(project)
                validateVaadinVersion(project)
            }
        }
    }

    private static void disableStatistics(Project project) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        if (!vaadin.submitStatistics) {
            Dependency statistics = vaadin.disableStatistics()
            project.configurations[COMPILE_CONFIGURATION].dependencies.add(statistics)
            project.configurations.all { DefaultConfiguration config ->
                config.resolutionStrategy.force("${statistics.group}:${statistics.name}:${statistics.version}")
            }
        }
    }

    private static void enableProductionMode(Project project) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        if (vaadin.productionMode) {
            Dependency productionMode = vaadin.enableProductionMode()
            project.configurations[COMPILE_CONFIGURATION].dependencies.add(productionMode)
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

    private static void validateVaadinVersion(Project project) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        if (vaadin.unSupportedVersion) {
            LOGGER.severe(
                    "The Vaadin version ($vaadin.version) you have selected is not supported by the plugin. " +
                            'Since vaadin.unsupportedVersion is set to True, continuing anyway. You are on your own.')

        } else if (!vaadin.isSupportedVersion()) {
            String[] supportedVersions = Versions.rawVersion('vaadin.supported.versions').split(',')
            throw new UnsupportedVersionException(
                    "The Vaadin version ($vaadin.version) you have selected is not supported by the plugin. " +
                        "Please pick one of the following supported Vaadin versions $supportedVersions. " +
                        'Alternatively you can add vaadin.unsupportedVersion=true to your build.gradle to override ' +
                        'this check but there is no guarantee it will work or that the build will be stable.')
        }
    }
}
