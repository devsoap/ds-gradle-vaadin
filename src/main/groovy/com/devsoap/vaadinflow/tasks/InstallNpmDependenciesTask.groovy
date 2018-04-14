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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import java.util.logging.Level

/**
 * Installs required NPM dependencies
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
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

    @OutputDirectory
    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    @OutputFile
    final File packageJsonOut = new File(VaadinClientDependenciesExtension.FRONTEND_DIR, PACKAGE_JSON_FILE)

    InstallNpmDependenciesTask() {
        dependsOn(NpmSetupTask.NAME)
        onlyIf {
            VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            !client.bowerDependencies.isEmpty() || !client.yarnDependencies.isEmpty() || vaadin.supportLegacyBrowsers
        }

        description = 'Installs Vaadin npm client dependencies'
        group = 'Vaadin'

        npmExecRunner.workingDir = workingDir
    }

    @TaskAction
    void run() {

        // Create package.json
        npmExecRunner.arguments = ['init', '-y', '-f']
        npmExecRunner.execute().assertNormalExitValue()

        // Set proper defaults for package.json
        File packageJson = new File(workingDir, PACKAGE_JSON_FILE)
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.main = ''
        pkg.version = '1.0.0'
        pkg.name = project.name + '-frontend'

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

        // Generate package.json
        LOGGER.info("Generating ${packageJsonOut} ...")
        if (!packageJsonOut.parentFile.exists()) {
            packageJsonOut.parentFile.mkdirs()
        }
        packageJsonOut.createNewFile()
        packageJsonOut.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))
        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))
    }
}
