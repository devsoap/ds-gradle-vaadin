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

import com.devsoap.vaadinflow.creators.VaadinProjectCreator
import com.devsoap.vaadinflow.models.VaadinProject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Creates a new project using a template
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateProjectTask extends DefaultTask {

    static final String NAME = 'vaadinCreateProject'

    @Option(option = 'name', description = 'Application name')
    String applicationName

    @Option(option = 'package', description = 'Application UI package')
    String applicationPackage

    private final VaadinProjectCreator projectCreator = new VaadinProjectCreator()

    CreateProjectTask() {
        description = 'Creates a Vaadin Flow project'
        group = 'Vaadin'
    }

    @TaskAction
    void run() {
        VaadinProject vaadinProject = VaadinProject.builder()
                .applicationName(applicationName ?: project.name.capitalize())
                .applicationPackage(applicationPackage ?: "com.example.${project.name.toLowerCase()}")
                .rootDirectory(project.rootDir)
                .build()
        projectCreator.generate(vaadinProject)
    }
}
