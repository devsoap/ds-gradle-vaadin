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
import com.moowork.gradle.node.yarn.YarnExecRunner
import com.moowork.gradle.node.yarn.YarnSetupTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Installs client dependencies into the webapp frontend
 *
 * @author John Ahlroos
 * @since 1.0
 */
class InstallClientDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinInstallClientDependencies'

    /**
     * Runner to run yarn tasks
     */
    final YarnExecRunner yarnRunner

    /**
     * Creates an installation task
     */
    InstallClientDependenciesTask() {
        dependsOn( YarnSetupTask.NAME )
        onlyIf {
            VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)
            !deps.yarnDependencies.empty
        }

        description = 'Installs Vaadin client dependencies'
        group = 'Vaadin'

        yarnRunner = new YarnExecRunner(project)

        this.project.afterEvaluate {
            yarnRunner.workingDir = yarnRunner.workingDir ?: project.node.nodeModulesDir
            if (!yarnRunner.workingDir.exists()) {
                yarnRunner.workingDir.mkdirs()
            }
        }
    }

    /**
     * Default action. Runs the task.
     */
    @TaskAction
    void run() {
        VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)

        // Install yarn dependencies
        deps.yarnDependencies.each { String name, String version ->
            yarnRunner.arguments = ['add', "$name@$version"]
            yarnRunner.execute().assertNormalExitValue()
        }
    }
}
