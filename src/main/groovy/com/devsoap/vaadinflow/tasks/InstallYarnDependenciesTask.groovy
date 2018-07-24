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
package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.models.ClientPackage
import com.devsoap.vaadinflow.util.LogUtils
import com.devsoap.vaadinflow.util.VaadinYarnRunner
import com.devsoap.vaadinflow.util.Versions
import com.devsoap.vaadinflow.util.WebJarHelper
import com.moowork.gradle.node.yarn.YarnExecRunner
import com.moowork.gradle.node.yarn.YarnSetupTask
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import java.nio.file.Paths
import java.util.logging.Level

/**
 * Installs yarn dependencies into the webapp frontend
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
@CacheableTask
class InstallYarnDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinInstallYarnDependencies'

    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    final VaadinYarnRunner yarnRunner = new VaadinYarnRunner(project, workingDir)

    @OutputFile
    final File packageJson = new File(workingDir, 'package.json')

    @OutputFile
    final File yarnrc = new File(workingDir, '.yarnrc')

    @OutputDirectory
    final File nodeModules = new File(workingDir, 'node_modules')

    /**
     * Creates an installation task
     */
    InstallYarnDependenciesTask() {
        dependsOn( YarnSetupTask.NAME )
        onlyIf {
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            !client.yarnDependencies.isEmpty() || !client.bowerDependencies.isEmpty() || client.compileFromSources
        }

        description = 'Installs Vaadin yarn dependencies'
        group = 'Vaadin'

        inputs.property('compileFromSources') {
            project.extensions.getByType(VaadinClientDependenciesExtension).compileFromSources
        }

        inputs.property('yarnDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).yarnDependencies
        }

        inputs.property('bowerDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies
        }
    }

    /**
     * Default action. Runs the task.
     */
    @TaskAction
    void run() {
        LOGGER.info('Creating package.json ...')
        yarnRunner.init()

        // Add dependencies to package.json
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)
        deps.yarnDependencies.each { String name, String version ->
            pkg.dependencies[name] = version
        }

        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))

        LOGGER.info('Installing yarn dependencies...')
        yarnRunner.install()

        LOGGER.info('Extracting webjars...')
        WebJarHelper.unpackWebjars(workingDir, project, nodeModules.name, false)
    }
}
