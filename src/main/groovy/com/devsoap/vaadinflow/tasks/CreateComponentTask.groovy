/*
 * Copyright 2018-2019 Devsoap Inc.
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0
 * International Public License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      https://creativecommons.org/licenses/by-nd/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.creators.ComponentCreator
import com.devsoap.vaadinflow.models.Component
import com.devsoap.vaadinflow.models.ProjectType

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Creates a new Vaadin Component
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateComponentTask extends DefaultTask {

    static final String NAME = 'vaadinCreateComponent'

    @Option(option = 'name', description = 'Component name')
    String componentName

    @Option(option = 'package', description = 'Component package')
    String componentPackage

    @Option(option = 'tag', description = 'Component tag')
    String componentTag

    private final ComponentCreator componentCreator = new ComponentCreator()

    CreateComponentTask() {
        description = 'Creates a new Vaadin component'
        group = 'Vaadin'
    }

    @TaskAction
    void run() {
        componentName = componentName ?: 'ExampleTextField'
        componentPackage = componentPackage ?: 'com.example.' + project.name.toLowerCase()
        componentTag = componentTag ?: 'input'

        componentCreator.generate new Component(
            rootDirectory : project.projectDir,
            componentPackage : this.componentPackage,
            componentName : this.componentName,
            componentTag : componentTag,
            projectType: ProjectType.get(project)
        )
    }
}
