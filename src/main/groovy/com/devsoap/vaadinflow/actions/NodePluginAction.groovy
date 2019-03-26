/*
 * Copyright 2018-2019 Devsoap Inc.
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
package com.devsoap.vaadinflow.actions

import com.devsoap.vaadinflow.NodePlugin
import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.util.HttpUtils
import com.devsoap.vaadinflow.util.TemplateWriter
import com.devsoap.vaadinflow.util.Versions
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.yarn.YarnSetupTask
import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 * Action taken when the Node plugin is applied to a project
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class NodePluginAction extends PluginAction {

    final String pluginId = 'com.moowork.node'

    private static final String NPM_RC_FILENAME = '.npmrc'

    @Override
    void apply(Project project) {
        this.project = project
        project.with {

            // Can't use super.apply() as it uses plugins.withId() which will return the mooworks plugin
            plugins.withType(NodePlugin) {
                project.gradle.taskGraph.removeTaskExecutionListener(taskListener)
                project.gradle.taskGraph.addTaskExecutionListener(taskListener)
                project.gradle.projectsEvaluated {
                    executeAfterAllEvaluations()
                }
                execute(project)
                project.afterEvaluate {
                    executeAfterEvaluate(project)
                }
            }

            pluginManager.apply(NodePlugin)

            String nodeDependnecy =
                    "com.moowork.gradle:gradle-node-plugin:${Versions.rawVersion('node.plugin.version')}"
            Dependency node = dependencies.create(nodeDependnecy) {
                description = 'Node Gradle Plugin'
            }
            configurations['compileOnly'].dependencies.add(node)
        }
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)

        LOGGER.info('Configuring node extension for vaadin project')
        NodeExtension nodeExtension = project.extensions.getByType(NodeExtension)
        nodeExtension.download = true
        nodeExtension.nodeModulesDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)
        nodeExtension.npmVersion = Versions.rawVersion('npm.version')
        nodeExtension.yarnVersion = Versions.rawVersion('yarn.version')
        nodeExtension.version = Versions.rawVersion('node.version')
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)

        File frontend = new File(project.buildDir, 'frontend')
        if (!frontend.exists()) {
            frontend.mkdirs()
        }
        File npmrc = new File(frontend, NPM_RC_FILENAME)
        if (!npmrc.exists()) {
            Map<String, Object> params = [:]

            if (HttpUtils.httpsProxy) {
                params['https-proxy'] = HttpUtils.httpsProxy
            }

            if (HttpUtils.httpProxy) {
                params['proxy'] = HttpUtils.httpProxy
            }

            VaadinClientDependenciesExtension vaadinClient = project.extensions
                    .getByType(VaadinClientDependenciesExtension)
            params.putAll(vaadinClient.customNpmProperties)

            TemplateWriter.builder()
                    .targetDir(frontend)
                    .templateFileName(NPM_RC_FILENAME)
                    .substitutions(['parameters' : params])
                    .build().write()
        }

        YarnSetupTask yarnSetup = project.tasks.getByName(YarnSetupTask.NAME)
        yarnSetup.args.add(2, '--userconfig')
        yarnSetup.args.add(3, npmrc.canonicalPath)
    }
}
