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

import static com.devsoap.vaadinflow.models.WebComponent.PackageManager.BOWER
import static com.devsoap.vaadinflow.models.WebComponent.PackageManager.YARN

import com.devsoap.vaadinflow.util.PathUtils
import com.devsoap.vaadinflow.models.ProjectType
import com.devsoap.vaadinflow.creators.ComponentCreator
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
 *     vaadinCreateWebComponent --dependency 'bower:PolymerElements/paper-slider:v2.0.5'
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
    private static final String HASH = '#'

    static final String NAME = 'vaadinCreateWebComponent'

    @Option(option = 'name', description = 'Component name')
    String componentName

    @Option(option = 'package', description = 'Component package')
    String componentPackage

    @Option(option = 'tag', description = 'Component tag')
    String componentTag

    @Option(option = 'dependency', description = 'Component dependency (prefix with bower: or yarn:')
    String componentDependency

    private final ComponentCreator webComponentCreator = new ComponentCreator()

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

        // Resolve package manager
        String dep
        if (componentDependency.startsWith(YARN_PREFIX)) {
            dep = componentDependency - YARN_PREFIX
        } else if (componentDependency.startsWith(BOWER_PREFIX)) {
            dep = componentDependency - BOWER_PREFIX
        } else {
            throw new GradleException("Dependency needs too start with either $YARN_PREFIX or $BOWER_PREFIX")
        }

        // Resolve dependency without version
        String depNoVersion
        if (dep.contains(COLON)) {
            depNoVersion = dep.split(COLON).first()
        } else if (dep.contains(HASH)) {
            depNoVersion = dep.split(HASH).first()
        } else {
            depNoVersion = dep
        }

        // Resolve dependency package
        String dependencyPackage
        String dependencyHtml
        if (depNoVersion.contains(PATH_SEPARATOR)) {
            if (componentDependency.startsWith(YARN_PREFIX)) {
                dependencyPackage = depNoVersion.split(COLON).last()
                dependencyHtml = depNoVersion.split(PATH_SEPARATOR).last()
            } else {
                dependencyPackage = depNoVersion.split(PATH_SEPARATOR).last()
                dependencyHtml = dependencyPackage
            }
        } else {
            dependencyPackage = depNoVersion
            dependencyHtml = dependencyPackage
        }

        // Add dependency import to build.gradle
        VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
        if (componentDependency.startsWith(YARN_PREFIX)) {
            buildFile << """
            ${VaadinClientDependenciesExtension.NAME}.yarn('$dep')
            """.stripIndent()
            client.yarn(dep)

        } else if (componentDependency.startsWith(BOWER_PREFIX)) {
            buildFile << """
            ${VaadinClientDependenciesExtension.NAME}.bower('$dep')
            """.stripIndent()
            client.bower(dep)
        }

        webComponentCreator.generate new WebComponent(
                componentName : componentName,
                componentPackage : componentPackage,
                componentTag : componentTag,
                dependencyPackage : dependencyPackage,
                dependencyHtml : dependencyHtml,
                rootDirectory : PathUtils.getSubmoduleSensitiveProjectRootDir(project),
                packageManager : componentDependency.startsWith('yarn') ? YARN : BOWER,
                projectType: ProjectType.get(project)
        )
    }

}
