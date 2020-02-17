/*
 * Copyright 2018-2020 Devsoap Inc.
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
package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.models.ClientPackage
import com.devsoap.vaadinflow.util.ClientPackageUtils
import com.devsoap.vaadinflow.util.VaadinYarnRunner
import com.devsoap.vaadinflow.util.WebJarHelper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Installs Bower dependencies into the webapp frontend
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
@CacheableTask
class InstallBowerDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinInstallBowerDependencies'

    protected final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    protected final VaadinYarnRunner yarnRunner = new VaadinYarnRunner(project, workingDir)

    @InputFile
    @PathSensitive(PathSensitivity.ABSOLUTE)
    final File packageJson = new File(workingDir, 'package.json')

    @OutputFile
    final File bowerJson = new File(workingDir, 'bower.json')

    @OutputDirectory
    final File bowerComponents = new File(workingDir, 'bower_components')

    @OutputDirectory
    final File staticResourcesDir = new File(workingDir, 'static')

    InstallBowerDependenciesTask() {
        dependsOn( InstallYarnDependenciesTask.NAME )
        onlyIf {
            VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            vaadin.compatibilityMode && (!client.bowerDependencies.isEmpty() || client.compileFromSources)
        }

        description = 'Installs Vaadin bower client dependencies'
        group = 'vaadin-compatibility'

        inputs.property('bowerDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies
        }

        inputs.property('compileFromSources') {
            project.extensions.getByType(VaadinClientDependenciesExtension).compileFromSources
        }
    }

    /**
     * Default action. Runs the task.
     */
    @TaskAction
    void run() {

        LOGGER.info('Creating bower.json...')
        VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)
        ClientPackage bowerModel = new ClientPackage(name: project.name.toLowerCase(), version: project.version)
                .with { model ->
            model.dependencies = model.dependencies ?: [:]
            deps.bowerDependencies.each { String name, String version ->
                model.dependencies[name] = version
            }
            model
        }
        bowerJson.text = ClientPackageUtils.toJson(bowerModel)

        LOGGER.info('Installing bower dependencies ... ')
        yarnRunner.bowerInstall()

        LOGGER.info('Extracting bower webjars...')
        WebJarHelper.unpackWebjars(workingDir, staticResourcesDir, project, bowerComponents.name, true)

        LOGGER.info('Validating bower modules...')
        List<String> imports = ClientPackageUtils.findHTMLImportsFromComponents(
                null, bowerComponents, workingDir)
        deps.bowerDependencies.keySet().each { dep ->
            if ( imports.findAll { it.contains( dep.split('/').last()) }.isEmpty()) {
                logger.error('HTML entrypoint file for {} not found, is it a Polymer 2 component?', dep)
            }
        }

    }
}
