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
import com.devsoap.vaadinflow.util.Versions
import com.moowork.gradle.node.yarn.YarnExecRunner
import com.moowork.gradle.node.yarn.YarnSetupTask
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import java.util.logging.Level

/**
 * Installs yarn dependencies into the webapp frontend
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class InstallYarnDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinInstallYarnDependencies'

    private static final String POLYMER_COMMAND = 'polymer'
    private static final String BOWER_COMMAND = 'bower'
    public static final String POLYMER_BUNDLER_COMMAND = 'polymer-bundler'

    final YarnExecRunner yarnRunner = new YarnExecRunner(project).with {
        execOverrides = { ExecSpec spec ->
            spec.standardOutput = LogUtils.getLogOutputStream(Level.FINE)
            spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
        }
        it
    }

    @OutputDirectory
    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    @OutputFile
    final File packageJson = new File(workingDir, 'package.json')

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

        inputs.property('yarnDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).yarnDependencies
        }

        yarnRunner.workingDir = workingDir
    }

    /**
     * Default action. Runs the task.
     */
    @TaskAction
    void run() {
        VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)

        // Ensure working directory is empty
        workingDir.deleteDir()
        workingDir.mkdirs()

        // Create package.json
        LOGGER.info('Creating package.json ...')
        yarnRunner.arguments = ['init', '-y']
        yarnRunner.execute().assertNormalExitValue()

        // Set proper defaults for package.json
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.main = ''
        pkg.version = '1.0.0'
        pkg.name = 'frontend'

        pkg.devDependencies['polymer-cli'] = Versions.rawVersion('polymer.cli.version')
        pkg.scripts[POLYMER_COMMAND] = POLYMER_COMMAND

        pkg.devDependencies[POLYMER_BUNDLER_COMMAND] = Versions.rawVersion('polymer.bundler.version')
        pkg.scripts[POLYMER_BUNDLER_COMMAND] = POLYMER_BUNDLER_COMMAND

        if(!client.bowerDependencies.isEmpty()) {
            pkg.devDependencies['bower'] = 'latest'
            pkg.scripts[BOWER_COMMAND] = BOWER_COMMAND
        }

        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))

        LOGGER.info('Installing development dependencies ...')
        yarnRunner.arguments = ['install']
        yarnRunner.execute().assertNormalExitValue()

        LOGGER.info('Installing yarn dependencies...')
        VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)
        deps.yarnDependencies.each { String name, String version ->
            LOGGER.info( "Found $name")
            yarnRunner.arguments = ['add', "$name@$version"]
            yarnRunner.execute().assertNormalExitValue()
        }
    }
}
