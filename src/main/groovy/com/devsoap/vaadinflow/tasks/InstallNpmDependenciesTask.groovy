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
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.models.ClientPackage
import com.devsoap.vaadinflow.util.LogUtils
import com.devsoap.vaadinflow.util.Versions
import com.moowork.gradle.node.npm.NpmExecRunner
import com.moowork.gradle.node.npm.NpmInstallTask
import com.moowork.gradle.node.npm.NpmSetupTask
import com.moowork.gradle.node.yarn.YarnSetupTask
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Installs required NPM dependencies
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
@CacheableTask
class InstallNpmDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinInstallNpmDependencies'

    private static final String PACKAGE_JSON_FILE = 'package.json'
    private static final String BOWER_COMMAND = 'bower'
    private static final String INSTALL_COMMAND = 'install'
    private static final String SAVE_AS_DEV_PARAM = '--save-dev'
    private static final String POLYMER_COMMAND = 'polymer'

    final NpmExecRunner npmExecRunner = new NpmExecRunner(project).with {
        execOverrides = { ExecSpec spec ->
            spec.standardOutput = LogUtils.getLogOutputStream(Level.FINE)
            spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
        }
        it
    }

    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    @OutputDirectory
    final File nodeModulesDir = new File(workingDir, 'node_modules')

    @OutputFile
    final File packageJson = new File(workingDir, PACKAGE_JSON_FILE)

    InstallNpmDependenciesTask() {
        description = 'Installs Vaadin npm client dependencies'
        group = 'Vaadin'

        dependsOn(NpmSetupTask.NAME)

        onlyIf {
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            !client.yarnDependencies.isEmpty() || !client.bowerDependencies.isEmpty() || client.compileFromSources
        }

        inputs.property('bowerDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies
        }

        inputs.property('yarnDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).yarnDependencies
        }

        inputs.property('vaadinCompileFromSources') {
            project.extensions.getByType(VaadinClientDependenciesExtension).compileFromSources
        }

        npmExecRunner.workingDir = workingDir
    }

    @TaskAction
    void run() {

        // Ensure working directory is empty
        workingDir.deleteDir()
        workingDir.mkdirs()

        // Create package.json
        npmExecRunner.arguments = ['init', '-y', '-f']
        npmExecRunner.execute().assertNormalExitValue()

        // Set proper defaults for package.json
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.main = ''
        pkg.version = '1.0.0'
        pkg.name = 'frontend'

        // Install bower (required by polymer-cli, even if no bower dependencies are added)
        LOGGER.info('Installing Bower ...')
        npmExecRunner.arguments = [INSTALL_COMMAND, BOWER_COMMAND, SAVE_AS_DEV_PARAM]
        npmExecRunner.execute().assertNormalExitValue()
        pkg.scripts[BOWER_COMMAND] = BOWER_COMMAND

        // Install polymer-build
        LOGGER.info('Installing polymer-build ...')
        npmExecRunner.arguments = [INSTALL_COMMAND,
                                   "polymer-cli@${Versions.rawVersion('polymer.cli.version')}",
                                   SAVE_AS_DEV_PARAM]
        npmExecRunner.execute().assertNormalExitValue()
        pkg.scripts[POLYMER_COMMAND] = POLYMER_COMMAND
        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))

        LOGGER.info('Removing node_modules/.bin symlinks ...')
        project.delete(new File(nodeModulesDir, '.bin'))
    }
}