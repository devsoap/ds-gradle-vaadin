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
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

/**
 * Assembles the files into the frontend directory
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class AssembleClientDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinAssembleClient'

    final File sourceDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)
    final File sourceDirEs5 = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR + '/build/frontend-es5')
    final File sourceDirEs6 = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR + '/build/frontend-es6')

    final File targetDir = project.file(VaadinClientDependenciesExtension.WEBAPP_DIR + '/frontend')
    final File targetDirEs5 = project.file(VaadinClientDependenciesExtension.WEBAPP_DIR + '/frontend-es5')
    final File targetDirEs6 = project.file(VaadinClientDependenciesExtension.WEBAPP_DIR + '/frontend-es6')

    /**
     * Assembles the built client artifacts into the webapp frontend directories
     */
    AssembleClientDependenciesTask() {
        dependsOn(TranspileDependenciesTask.NAME, InstallBowerDependenciesTask.NAME, InstallYarnDependenciesTask.NAME)
        onlyIf {
            VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            !client.bowerDependencies.isEmpty() || !client.yarnDependencies.isEmpty() || vaadin.productionMode
        }
        group = 'Vaadin'
        description = 'Copies built client dependencies into the right target directory'
        project.afterEvaluate {
            VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
            if (vaadin.productionMode) {
                inputs.dir(sourceDirEs5)
                inputs.dir(sourceDirEs6)
                outputs.dirs(targetDirEs5, targetDirEs6)
            } else {
                inputs.dir(sourceDir)
                outputs.dir(targetDir)
            }
        }
    }

    @TaskAction
    void run() {
        List<String> excludes = [
             '**/LICENSE*',
             '**/demo/**',
             '**/docs/**',
             '**/test*/**',
             '**/build/**',
             '**/frontend-*/**',
             '**/node_modules/**',
             '**/.*',
             '**/*.md',
             '**/bower.json',
             '**/polymer.json',
             '**/package.json',
             '**/package-lock.json',
             '**/yarn.lock',
        ]

        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)

        if (vaadin.productionMode) {
            project.with {
                String bowerComponentsGlob = '**/bower_components/**'
                copy { spec -> spec.from(targetDir).exclude([bowerComponentsGlob]).into(targetDirEs5) }
                copy { spec -> spec.from(sourceDir).exclude(excludes).into(targetDirEs5) }
                copy { spec -> spec.from(sourceDirEs5).exclude(excludes).into(targetDirEs5) }

                copy { spec -> spec.from(targetDir).exclude([bowerComponentsGlob]).into(targetDirEs6) }
                copy { spec -> spec.from(sourceDir).exclude(excludes).into(targetDirEs6) }
                copy { spec -> spec.from(sourceDirEs6).exclude(excludes).into(targetDirEs6) }
            }
        } else {
            project.copy { spec -> spec.from(sourceDir).exclude(excludes).into(targetDir) }
        }
    }
}
