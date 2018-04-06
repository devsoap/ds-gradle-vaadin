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
import com.moowork.gradle.node.npm.NpmExecRunner
import com.moowork.gradle.node.yarn.YarnExecRunner
import com.moowork.gradle.node.yarn.YarnSetupTask
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Installs client dependencies into the webapp frontend
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class InstallClientDependenciesTask extends DefaultTask {
    private static final String BOWER_COMMAND = 'bower'
    private static final String INSTALL_COMMAND = 'install'

    static final String NAME = 'vaadinInstallClientDependencies'

    /**
     * Runner to run yarn tasks
     */
    final YarnExecRunner yarnRunner

    final NpmExecRunner npmExecRunner

    /**
     * Creates an installation task
     */
    InstallClientDependenciesTask() {
        dependsOn( YarnSetupTask.NAME )
        onlyIf {
            VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)
            !deps.yarnDependencies.empty || !deps.bowerDependencies.empty
        }

        description = 'Installs Vaadin client dependencies'
        group = 'Vaadin'

        yarnRunner = new YarnExecRunner(project)
        npmExecRunner = new NpmExecRunner(project)

        inputs.property('yarnDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).yarnDependencies
        }

        inputs.property('bowerDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies
        }

        this.project.afterEvaluate {

            yarnRunner.workingDir = yarnRunner.workingDir ?: project.node.nodeModulesDir
            if (!yarnRunner.workingDir.exists()) {
                yarnRunner.workingDir.mkdirs()
            }
            outputs.dir(yarnRunner.workingDir)
            npmExecRunner.workingDir = npmExecRunner.workingDir ?: project.node.nodeModulesDir
            if (!npmExecRunner.workingDir.exists()) {
                npmExecRunner.workingDir.mkdirs()
            }
            outputs.dir(npmExecRunner.workingDir)
        }
    }

    /**
     * Default action. Runs the task.
     */
    @TaskAction
    void run() {
        VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)

        // Create package.json
        npmExecRunner.arguments = ['init', '-y', '-f']
        npmExecRunner.execute().assertNormalExitValue()

        // Install yarn dependencies
        deps.yarnDependencies.each { String name, String version ->
            yarnRunner.arguments = ['add', "$name@$version"]
            yarnRunner.execute().assertNormalExitValue()
        }

        if (!deps.bowerDependencies.isEmpty()) {
            LOGGER.info('Installing Bower first...')
            // Install bower
            npmExecRunner.arguments = [INSTALL_COMMAND, BOWER_COMMAND, '--save-dev']
            npmExecRunner.execute().assertNormalExitValue()

            // Add bower as a script to package.json
            File pkgjson = new File(npmExecRunner.workingDir, 'package.json')
            pkgjson.text = pkgjson.text.replace('"scripts": {',
                    '"scripts": {\n"bower":"bower",\n')
        }

        // Install bower dependencies
        deps.bowerDependencies.each { String name, String version ->
            npmExecRunner.arguments = ['run', BOWER_COMMAND, INSTALL_COMMAND, "$name@$version"]
            npmExecRunner.execute().assertNormalExitValue()
        }
    }
}
