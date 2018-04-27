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
import com.devsoap.vaadinflow.util.LogUtils
import com.moowork.gradle.node.npm.NpmSetupTask
import com.moowork.gradle.node.yarn.YarnExecRunner
import com.moowork.gradle.node.yarn.YarnSetupTask
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
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

    final YarnExecRunner yarnRunner = new YarnExecRunner(project).with {
        execOverrides = { ExecSpec spec ->
            spec.standardOutput = LogUtils.getLogOutputStream(Level.FINE)
            spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
        }
        it
    }

    @OutputDirectory
    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    /**
     * Creates an installation task
     */
    InstallYarnDependenciesTask() {
        dependsOn( InstallNpmDependenciesTask.NAME, YarnSetupTask.NAME )
        onlyIf {
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            !client.yarnDependencies.isEmpty()
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

        LOGGER.info('Installing yarn dependencies...')
        VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)
        deps.yarnDependencies.each { String name, String version ->
            LOGGER.info( "Found $name")
            yarnRunner.arguments = ['add', "$name@$version"]
            yarnRunner.execute().assertNormalExitValue()
        }

        LOGGER.info('Copying yarn dependencies into bower components')
        File bowerComponents = new File(workingDir, 'bower_components')
        project.fileTree(new File(workingDir, 'node_modules'))
                .include('**/**/bower.json')
                .each { File bowerJson ->
            File componentDir = bowerJson.parentFile
            LOGGER.info( "Copying yarn dependency $componentDir.name to $bowerComponents")
            project.copy { spec -> spec.from(componentDir).into(new File(bowerComponents, componentDir.name)) }
        }
    }
}
