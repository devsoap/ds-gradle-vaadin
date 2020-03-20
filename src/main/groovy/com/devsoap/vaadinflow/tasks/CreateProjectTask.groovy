/*
 * Copyright 2018-2020 Devsoap Inc.
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

import com.devsoap.vaadinflow.actions.JavaPluginAction
import com.devsoap.vaadinflow.models.ApplicationType
import com.devsoap.vaadinflow.creators.VaadinProjectCreator
import com.devsoap.vaadinflow.creators.VaadinThemeCreator
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.models.ProjectType
import com.devsoap.vaadinflow.models.VaadinProject

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.nio.file.Paths

/**
 * Creates a new project using a template
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateProjectTask extends DefaultTask {

    static final String NAME = 'vaadinCreateProject'

    private static final String LUMO = 'lumo'

    @Input
    @Optional
    @Option(option = 'name', description = 'Application name')
    String applicationName

    @Input
    @Optional
    @Option(option = 'package', description = 'Application UI package')
    String applicationPackage

    @Input
    @Optional
    @Option(option = 'baseTheme', description = "The base theme of the application, can be either 'lumo' or 'material'")
    String applicationBaseTheme

    private final VaadinProjectCreator projectCreator = new VaadinProjectCreator()
    private final VaadinThemeCreator themeCreator = new VaadinThemeCreator()

    CreateProjectTask() {
        description = 'Creates a Vaadin Flow project'
        group = 'Vaadin'
    }

    @TaskAction
    void run() {

        if (applicationBaseTheme &&
                applicationBaseTheme.toLowerCase() != 'material' &&
                applicationBaseTheme.toLowerCase() != LUMO) {
            throw new GradleException("Wrong base theme value. Valid values are 'lumo' or 'material'")
        }

        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
        VaadinProject vaadinProject = new VaadinProject(
                applicationName: applicationName ?: project.name.capitalize(),
                applicationPackage : applicationPackage ?: "com.example.${project.name.toLowerCase()}",
                applicationBaseTheme :applicationBaseTheme ?: LUMO,
                rootDirectory : project.projectDir,
                webappDirectory: assembleTask.webappDir,
                productionMode : vaadin.productionMode,
                compatibilityMode: vaadin.compatibilityMode,
                projectType: ProjectType.get(project),
                applicationType: ApplicationType.get(project)
        )

        projectCreator.generate(vaadinProject)

        themeCreator.generateCssTheme(vaadinProject)
    }

}
