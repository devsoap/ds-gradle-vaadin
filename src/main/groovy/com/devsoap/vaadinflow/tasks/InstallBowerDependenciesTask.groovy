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
import com.moowork.gradle.node.npm.NpmSetupTask
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Installs Bower dependencies into the webapp frontend
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class InstallBowerDependenciesTask extends DefaultTask {

    private static final String BOWER_COMMAND = 'bower'
    private static final String INSTALL_COMMAND = 'install'

    static final String NAME = 'vaadinInstallBowerDependencies'

    final NpmExecRunner npmExecRunner

    InstallBowerDependenciesTask() {
        dependsOn( NpmSetupTask.NAME )
        onlyIf {
            !project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies.empty
        }

        description = 'Installs Vaadin bower client dependencies'
        group = 'Vaadin'

        npmExecRunner = new NpmExecRunner(project)

        inputs.property('bowerDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies
        }

        this.project.afterEvaluate {
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

        // Install bower
        LOGGER.info('Installing Bower first...')
        npmExecRunner.arguments = [INSTALL_COMMAND, BOWER_COMMAND, '--save-dev']
        npmExecRunner.execute().assertNormalExitValue()

        // Add bower as a script to package.json
        File pkgjson = new File(npmExecRunner.workingDir, 'package.json')
        pkgjson.text = pkgjson.text.replace('"scripts": {',
                '"scripts": {\n"bower":"bower",\n')

        // Install bower dependencies
        deps.bowerDependencies.each { String name, String version ->
            npmExecRunner.arguments = ['run', BOWER_COMMAND, INSTALL_COMMAND, "$name@$version"]
            npmExecRunner.execute().assertNormalExitValue()
        }
    }
}
