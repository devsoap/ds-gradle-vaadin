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
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Assembles the files into the frontend directory
 *
 * @author John Ahlroos
 * @since 1.0
 */
class AssembleClientDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinAssembleClient'

    @InputDirectory
    final File sourceDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    @OutputDirectory
    final File targetDir = project.file(VaadinClientDependenciesExtension.WEBAPP_DIR)

    AssembleClientDependenciesTask() {
        dependsOn(TranspileDependenciesTask.NAME, InstallBowerDependenciesTask.NAME, InstallYarnDependenciesTask.NAME)
        onlyIf {
            VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            !client.bowerDependencies.isEmpty() || !client.yarnDependencies.isEmpty() || vaadin.supportLegacyBrowsers
        }
        group = 'Vaadin'
        description = 'Copies built client dependencies into the right target directory'
    }

    @TaskAction
    void run() {

        List<String> excludes = [
             '**/LICENSE*',
             '**/demo/**',
             '**/docs/**',
             '**/test*/**',
             '**/build/**',
             '**/.*',
             '**/*.md',
             '**/bower.json',
             '**/package.json',
             '**/package-lock.json',
             '**/yarn.lock'
        ]

        project.copy { spec ->
            spec.from(sourceDir,)
                .exclude(excludes)
                .into(new File(targetDir, 'frontend'))
        }

        project.copy { spec ->
            spec.from(new File(sourceDir, 'build'))
                .include('frontend*/**')
                .exclude(excludes)
                .into(targetDir)
        }
    }
}
