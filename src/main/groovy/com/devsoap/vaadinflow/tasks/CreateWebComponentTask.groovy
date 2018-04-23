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

import com.devsoap.vaadinflow.creators.WebComponentCreator
import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.models.WebComponent
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Creates a new Web Component
 *
 * Usage:
 * <code>
 *     vaadinCreateWebComponent --dependency 'bower:PolymerElements/paper-slider'
 * </code>
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateWebComponentTask extends DefaultTask {

    private static final String YARN_PREFIX = 'yarn:'
    private static final String BOWER_PREFIX = 'bower:'
    private static final String COLON = ':'
    private static final String PATH_SEPARATOR = '/'

    static final String NAME = 'vaadinCreateWebComponent'

    @Option(option = 'name', description = 'Component name')
    String componentName

    @Option(option = 'package', description = 'Component package')
    String componentPackage

    @Option(option = 'tag', description = 'Component tag')
    String componentTag

    @Option(option = 'dependency', description = 'Component dependency (prefix with bower: or yarn:')
    String componentDependency

    private final WebComponentCreator webComponentCreator = new WebComponentCreator()

    CreateWebComponentTask() {
        description = 'Creates a new Vaadin web component'
        group = 'Vaadin'
        finalizedBy(InstallYarnDependenciesTask.NAME)
        finalizedBy(InstallBowerDependenciesTask.NAME)
    }

    @TaskAction
    void run() {

        componentName = componentName ?: 'ExampleWebComponent'
        componentPackage = componentPackage ?: 'com.example.' + project.name.toLowerCase()
        componentTag = componentTag ?: componentName.replaceAll(/\B[A-Z]/) { '-' + it }.toLowerCase()
        componentDependency = componentDependency ?: componentTag

        File buildFile = project.file('build.gradle')
        String dependencyPackage
        String dependencyHtml
        String dep
        if (componentDependency.startsWith(YARN_PREFIX)) {
            dep = componentDependency - YARN_PREFIX
            String depNoVersion = dep.split(COLON).first()
            dependencyPackage = depNoVersion
            dependencyHtml = dep.split(COLON).first().split(PATH_SEPARATOR).last()

            buildFile << """
            ${VaadinClientDependenciesExtension.NAME}.yarn('$dep')
            """.stripIndent()

        } else if (componentDependency.startsWith(BOWER_PREFIX)) {
            dep = componentDependency - BOWER_PREFIX
            String depNoVersion = dep.split(COLON).first()
            dependencyPackage = depNoVersion.split(PATH_SEPARATOR).last()
            dependencyHtml = dependencyPackage

            buildFile << """
            ${VaadinClientDependenciesExtension.NAME}.bower('$dep')
            """.stripIndent()

        } else {
            throw new GradleException("Dependency needs too start with either $YARN_PREFIX or $BOWER_PREFIX")
        }

        WebComponent component = WebComponent
                .builder()
                .componentName(componentName)
                .componentPackage(componentPackage)
                .componentTag(componentTag)
                .dependencyPackage(dependencyPackage)
                .dependencyHtml(dependencyHtml)
                .rootDirectory(project.rootDir)
                .build()

        webComponentCreator.generate(component)
    }

}
