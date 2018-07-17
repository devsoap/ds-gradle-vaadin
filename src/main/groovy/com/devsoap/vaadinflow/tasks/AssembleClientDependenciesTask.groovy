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
import org.gradle.api.tasks.TaskAction

/**
 * Assembles the files into the frontend directory
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class AssembleClientDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinAssembleClient'

    final File frontendDir = project.file(VaadinClientDependenciesExtension.FRONTEND_DIR)
    final File frontendBuildDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)
    final File sourceDirEs5 = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR + '/build/frontend-es5')
    final File sourceDirEs6 = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR + '/build/frontend-es6')

    final File webAppGenDir = new File(project.buildDir, 'webapp-gen')
    final File webAppGenFrontendDir = new File(webAppGenDir, 'frontend')
    final File targetDirEs5 = new File(webAppGenDir, 'frontend-es5')
    final File targetDirEs6 = new File(webAppGenDir, 'frontend-es6')

    /**
     * Assembles the built client artifacts into the webapp frontend directories
     */
    AssembleClientDependenciesTask() {
        dependsOn(TranspileDependenciesTask.NAME,
                InstallBowerDependenciesTask.NAME,
                InstallYarnDependenciesTask.NAME,
                ConvertCssToHtmlStyleTask.NAME
        )
        onlyIf {
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            boolean hasClientDependencies = !client.bowerDependencies.isEmpty() || !client.yarnDependencies.isEmpty()
            hasClientDependencies || client.compileFromSources
        }

        group = 'Vaadin'
        description = 'Copies built client dependencies into the right target directory'
        project.afterEvaluate {
            VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            if (vaadin.productionMode && client.compileFromSources) {
                inputs.dir(sourceDirEs5)
                inputs.dir(sourceDirEs6)
                outputs.dirs(targetDirEs5, targetDirEs6)
            } else {
                inputs.dir(frontendBuildDir)
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
             '**/.*',
             '**/*.md',
             '**/bower.json',
             '**/polymer.json',
             '**/package.json',
             '**/package-lock.json',
             '**/yarn.lock',
        ]

        List<String> frontendIncludes = [
             'vaadin-flow-bundle-manifest.json',
             'styles/**'
        ]

        VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
        if (client.compileFromSources) {
            project.with {
                copy { spec -> spec.from(frontendDir).include(frontendIncludes).into(targetDirEs5) }
                copy { spec -> spec.from(frontendBuildDir).include(frontendIncludes).into(targetDirEs5) }
                copy { spec -> spec.from(sourceDirEs5).into(targetDirEs5) }

                copy { spec -> spec.from(frontendDir).include(frontendIncludes).into(targetDirEs6) }
                copy { spec -> spec.from(frontendBuildDir).include(frontendIncludes).into(targetDirEs6) }
                copy { spec -> spec.from(sourceDirEs6).into(targetDirEs6) }
            }
        } else {
            project.copy { spec -> spec.from(frontendBuildDir).exclude(excludes).into(webAppGenFrontendDir) }
        }
    }
}
