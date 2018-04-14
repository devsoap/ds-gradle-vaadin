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
import com.moowork.gradle.node.npm.NpmExecRunner
import com.moowork.gradle.node.npm.NpmSetupTask
import com.sun.security.ntlm.Client
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import java.util.logging.Level

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

    final NpmExecRunner npmExecRunner = new NpmExecRunner(project).with {
        execOverrides = { ExecSpec spec ->
            spec.standardOutput = LogUtils.getLogOutputStream(Level.FINE)
            spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
        }
        it
    }

    @InputDirectory
    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    @OutputFile
    final File bowerJson = new File(workingDir, 'bower.json')

    @InputFile
    final File packageJsonOut = new File(workingDir, 'package.json')

    InstallBowerDependenciesTask() {
        dependsOn( InstallNpmDependenciesTask.NAME )
        onlyIf {
            !project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies.isEmpty()
        }

        description = 'Installs Vaadin bower client dependencies'
        group = 'Vaadin'

        npmExecRunner.workingDir = workingDir

        inputs.property('bowerDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies
        }
    }

    /**
     * Default action. Runs the task.
     */
    @TaskAction
    void run() {

        VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)

        // Create bower.json
        LOGGER.info('Creating bower.json...')
        ClientPackage bowerModel = new ClientPackage(name: 'frontend', version: '1.0.0').with { model ->
            deps.bowerDependencies.each { String name, String version ->
                model.dependencies[name] = version
            }
            model
        }
        bowerJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(bowerModel))

        // Run bower install
        LOGGER.info('Installing bower dependencies')
        npmExecRunner.arguments = ['run', BOWER_COMMAND, INSTALL_COMMAND, '--config.interactive=false' ]
        npmExecRunner.execute().assertNormalExitValue()
    }
}
