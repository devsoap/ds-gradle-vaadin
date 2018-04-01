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
package com.devsoap.vaadinflow.actions

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.moowork.gradle.node.NodeExtension
import groovy.util.logging.Log
import org.gradle.api.Project

/**
 * Action taken when the Node plugin is applied to a project
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class NodePluginAction extends PluginAction {

    String pluginId = 'com.moowork.node'

    @Override
    protected void execute(Project project) {
        super.execute(project)

        LOGGER.info('Configuring node extension for vaadin project')
        NodeExtension nodeExtension = project.extensions.getByType(NodeExtension)
        nodeExtension.download = true
        nodeExtension.nodeModulesDir = project.file(VaadinClientDependenciesExtension.FRONTEND_DIR)
    }
}
