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

import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import com.devsoap.vaadinflow.util.Versions
import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle

import javax.inject.Inject

/**
 * Configures the Gretty plugin to be compatible
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class GrettyPluginAction extends PluginAction {

    final String pluginId = 'org.gretty'

    private  static final String PREPARE_INPLACE_WEB_APP_FOLDER = 'prepareInplaceWebAppFolder'

    @Override
    void apply(Project project) {
        project.ext.jetty94Version = Versions.rawVersion('gretty.jetty.version')
        super.apply(project)
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)
        project.gretty.servletContainer = 'jetty9.4'
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)
        project.tasks[PREPARE_INPLACE_WEB_APP_FOLDER].dependsOn(AssembleClientDependenciesTask.NAME)
    }

    @Override
    protected void afterTaskExecuted(Task task) {
        super.afterTaskExecuted(task)
        if (task.name == PREPARE_INPLACE_WEB_APP_FOLDER) {
            LOGGER.info('Copying generated web-app resources into extracted webapp')
            task.project.copy { copy ->
                    copy.from("${task.project.buildDir.canonicalPath}/webapp-gen")
                            .into("${task.project.buildDir.canonicalPath}/inplaceWebapp")
            }
        }
    }
}
