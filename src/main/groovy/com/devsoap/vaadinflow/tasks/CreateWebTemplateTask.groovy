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

import com.devsoap.vaadinflow.creators.ComponentCreator
import com.devsoap.vaadinflow.models.ProjectType
import com.devsoap.vaadinflow.models.WebTemplate
import com.devsoap.vaadinflow.util.PathUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Creates a new Web Template
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateWebTemplateTask extends DefaultTask {

    static final String NAME = 'vaadinCreateWebTemplate'

    @Option(option = 'name', description = 'Template name')
    String templateName

    @Option(option = 'package', description = 'Template package')
    String templatePackage

    @Option(option = 'tag', description = 'Template tag')
    String templateTag

    private final ComponentCreator componentCreator = new ComponentCreator()

    CreateWebTemplateTask() {
        group = 'Vaadin'
        description = 'Creates a new Web Template'
    }

    @TaskAction
    void run() {
        templateName = templateName ?: 'ExampleWebTemplate'
        templatePackage = templatePackage ?: 'com.example.' + project.name.toLowerCase()
        templateTag = templateTag ?: templateName.replaceAll(/\B[A-Z]/) { '-' + it }.toLowerCase()

        componentCreator.generate new WebTemplate(
                componentName : templateName,
                componentPackage : templatePackage,
                componentTag : templateTag,
                rootDirectory : PathUtils.getSubmoduleSensitiveProjectRootDir(project),
                projectType: ProjectType.get(project)
        )
    }
}
