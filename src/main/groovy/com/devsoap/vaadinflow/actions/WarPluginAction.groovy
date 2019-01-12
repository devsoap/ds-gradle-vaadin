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
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.War

/**
 * Configures the War plugin
 *
 * @author John Ahlroos
 * @since 1.0
 */
class WarPluginAction extends PluginAction {

    final String pluginId = 'war'

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)
        project.tasks.withType(War) { War task ->
            task.from(new File(project.buildDir, 'webapp-gen'))
            AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
            if (assembleTask.webappDirSet) {
                task.from(assembleTask.webappDir)
            }
        }
    }
}
