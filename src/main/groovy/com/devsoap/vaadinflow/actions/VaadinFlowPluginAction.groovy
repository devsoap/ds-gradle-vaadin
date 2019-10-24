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

import static com.devsoap.license.DevsoapLicenseExtension.Credential

import org.gradle.api.internal.artifacts.BaseRepositoryFactory
import com.devsoap.license.DevsoapLicenseExtension
import com.devsoap.license.DevsoapLicensePlugin
import com.devsoap.license.Validator
import com.devsoap.vaadinflow.VaadinFlowPlugin
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import com.devsoap.vaadinflow.tasks.ConvertGroovyTemplatesToHTML
import com.devsoap.vaadinflow.tasks.InstallBowerDependenciesTask
import com.devsoap.vaadinflow.tasks.InstallYarnDependenciesTask
import com.devsoap.vaadinflow.tasks.VersionCheckTask
import com.devsoap.vaadinflow.tasks.WrapCssTask
import com.devsoap.vaadinflow.util.LogUtils
import com.devsoap.vaadinflow.util.Versions
import com.devsoap.vaadinflow.util.WebJarHelper
import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 * Action taken when the Vaadin plugin is applied to a project
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinFlowPluginAction extends PluginAction {

    final String pluginId = VaadinFlowPlugin.PLUGIN_ID

    private static final String PLUGIN_VERSION_KEY = 'vaadin.plugin.version'
    private static final String PROCESS_RESOURCES = 'processResources'
    private static final String RUNNING_IN_COMPATIBILITY_MODE_MESSAGE =
            'The project will be compiled for Vaadin 13 (Flow 1) compatibility mode. '
    private static final String COMPILE_ONLY = 'compileOnly'

    @Override
    void apply(Project project) {
        super.apply(project)
        project.plugins.apply('java')

        project.pluginManager.apply(DevsoapLicensePlugin)

        DevsoapLicenseExtension devsoap = project.extensions.getByType(DevsoapLicenseExtension)
        devsoap.credential(VaadinFlowPlugin.PRODUCT_NAME,
                System.getProperty('devsoap.gradle-flow-plugin.license.email'),
                System.getProperty('devsoap.gradle-flow-plugin.license.key')
        ) { Credential c ->
            c.signature =  Versions.rawVersion('vaadin.plugin.signature')
        }

        String licenseDependency =
                "com.devsoap:devsoap-license-plugin:${ Versions.rawVersion('devsoap.license.version') }"
        Dependency license = project.dependencies.create(licenseDependency)
        project.configurations[COMPILE_ONLY].dependencies.add(license)
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)
        project.with {
            tasks[PROCESS_RESOURCES].with {
                dependsOn(VersionCheckTask.NAME)
                dependsOn(WrapCssTask.NAME)
                dependsOn(ConvertGroovyTemplatesToHTML.NAME)
            }

            tasks['jar'].dependsOn(AssembleClientDependenciesTask.NAME)

            repositories.maven { repository ->
                repository.name = 'Gradle Plugin Portal'
                repository.url = (
                        System.getProperty(BaseRepositoryFactory.PLUGIN_PORTAL_OVERRIDE_URL_PROPERTY) ?:
                                BaseRepositoryFactory.PLUGIN_PORTAL_DEFAULT_URL
                )
            }

            String pluginDependency =
                    "com.devsoap:gradle-vaadin-flow-plugin:${Versions.rawVersion(PLUGIN_VERSION_KEY)}"
            Dependency vaadin = dependencies.create(pluginDependency) {
                description = 'Gradle Vaadin Plugin'
            }
            configurations[COMPILE_ONLY].dependencies.add(vaadin)
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

        String vaadinVersion = Versions.version(PLUGIN_VERSION_KEY)
        if (Validator.isValidLicense(project, VaadinFlowPlugin.PRODUCT_NAME)) {
            DevsoapLicenseExtension devsoap = project.extensions[DevsoapLicenseExtension.NAME]
            Credential credential = devsoap.getCredential(VaadinFlowPlugin.PRODUCT_NAME)
            LogUtils.printIfNotPrintedBefore( project,
                    "Using DS Gradle Vaadin Flow Plugin $vaadinVersion (Licensed to ${credential.email})"
            )
        } else {
            LogUtils.printIfNotPrintedBefore( project,
                    "Using DS Gradle Vaadin Flow Plugin $vaadinVersion (UNLICENSED)"
            )
        }

        VaadinFlowPluginExtension vaadin = project.extensions[VaadinFlowPluginExtension.NAME]
        if (!vaadin.versionSet) {
            LOGGER.warning('vaadin.version is not set, falling back to latest Vaadin version')
        }

        if (vaadin.submitStatisticsUnset) {
            LOGGER.warning('Allow Vaadin to gather usage statistics by setting ' +
                    'vaadin.submitStatistics=true (hide this message by setting it to false)')
        }

        if (vaadin.compatibilityMode && Validator.isValidLicense(project, VaadinFlowPlugin.PRODUCT_NAME)) {
            LOGGER.warning(
                    RUNNING_IN_COMPATIBILITY_MODE_MESSAGE +
                    'To disable compatibility mode set vaadin.compatibilityMode=false. (experimental)')
        } else if (vaadin.compatibilityMode) {
            LOGGER.warning(
                    RUNNING_IN_COMPATIBILITY_MODE_MESSAGE +
                    ' Running in NPM mode is only available for PRO subscribers.')
        }
    }
}
