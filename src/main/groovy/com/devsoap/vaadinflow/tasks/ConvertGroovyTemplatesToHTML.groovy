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
package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.models.ProjectType
import groovy.text.Template
import groovy.text.markup.MarkupTemplateEngine
import groovy.text.markup.TemplateConfiguration
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

/**
 * TPL to HTML converter task for frontend web templates
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class ConvertGroovyTemplatesToHTML extends DefaultTask {

    static final String NAME = 'vaadinConvertGroovyTemplatesToHtml'

    static final String TEMPLATES_TARGET_PATH = 'webapp-gen/frontend/templates'

    @InputFiles
    final Closure<FileTree> templateFiles = {
        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
        File templatesPath = Paths.get(assembleTask.webappDir.canonicalPath, 'frontend', 'templates').toFile()
        project.fileTree(templatesPath).matching { it.include( '**/*.tpl') }
    }

    @OutputDirectory
    final File targetPath = new File(project.buildDir, TEMPLATES_TARGET_PATH)

    ConvertGroovyTemplatesToHTML() {
        group = 'vaadin'
        description = 'Converts Groovy layout templates (tpl) into html'
        onlyIf { ProjectType.get(project) == ProjectType.GROOVY }
    }

    @TaskAction
    void run() {
        TemplateConfiguration config = new TemplateConfiguration()
        MarkupTemplateEngine engine = new MarkupTemplateEngine(config)
        templateFiles.call().each {
            LOGGER.info("Converting $it.name Groovy template into HTML")
            targetPath.mkdirs()
            File htmlFile = new File(targetPath, "${ it.name - '.tpl' }.html" )
            Template template = engine.createTemplate(it)
            Writer writer = new FileWriter(htmlFile)
            Writable output = template.make([:])
            output.writeTo(writer)
        }
    }

}

