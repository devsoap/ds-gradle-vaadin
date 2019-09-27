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

import static com.devsoap.vaadinflow.models.WebComponent.PackageManager.BOWER
import static com.devsoap.vaadinflow.models.WebComponent.PackageManager.YARN

import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
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
    private static final String NPM_PREFIX = 'npm:'
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
        finalizedBy(InstallYarnDependenciesTask.NAME, InstallBowerDependenciesTask.NAME)
    }

    @TaskAction
    void run() {

        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)

        componentName = componentName ?: 'ExampleWebComponent'
        componentPackage = componentPackage ?: 'com.example.' + project.name.toLowerCase()

        componentDependency = componentDependency ?: "yarn:$componentTag"

        File buildFile = project.file('build.gradle')

        // Resolve package manager
        String dep
        if (componentDependency.startsWith(YARN_PREFIX)) {
            dep = componentDependency - YARN_PREFIX
        } else if (componentDependency.startsWith(BOWER_PREFIX)) {
            dep = componentDependency - BOWER_PREFIX
        } else if (componentDependency.startsWith(NPM_PREFIX)) {
            dep = componentDependency - NPM_PREFIX
        } else if (vaadin.compatibilityMode) {
            throw new GradleException("Dependency needs too start with either $YARN_PREFIX or $BOWER_PREFIX")
        } else {
            dep = componentDependency
        }

        // Resolve dependency without version
        String depNoVersion
        String depVersion
        if (dep.contains(COLON)) {
            depNoVersion = dep.split(COLON).first()
            depVersion = dep.split(COLON).last()
        } else if (dep.contains(HASH)) {
            depNoVersion = dep.split(HASH).first()
            depVersion = dep.split(HASH).last()
        } else {
            depNoVersion = dep
            depVersion = ''
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

        componentTag = componentTag ?: dependencyHtml

        // Add dependency import to build.gradle
        if (vaadin.compatibilityMode) {
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            if (componentDependency.startsWith(YARN_PREFIX)) {
                buildFile << """
            ${VaadinClientDependenciesExtension.NAME}.yarn('${dep}')
            """.stripIndent()
                client.yarn(dep)

            } else if (componentDependency.startsWith(NPM_PREFIX)) {
                buildFile << """
            ${VaadinClientDependenciesExtension.NAME}.npm('${dep}')
            """.stripIndent()
                client.npm(dep)

            } else if (componentDependency.startsWith(BOWER_PREFIX)) {
                buildFile << """
            ${VaadinClientDependenciesExtension.NAME}.bower('$dep')
            """.stripIndent()
                client.bower(dep)
            }
        }

        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)

        webComponentCreator.generate new WebComponent(
                compatibilityMode: vaadin.compatibilityMode,
                componentName : componentName,
                componentPackage : componentPackage,
                componentTag : componentTag,
                dependencyPackage : dependencyPackage,
                dependencyHtml : dependencyHtml,
                dependencyVersion: depVersion,
                dependencyArtifact: depNoVersion,
                rootDirectory : project.projectDir,
                webappDirectory: assembleTask.webappDir,
                packageManager : componentDependency.startsWith(YARN_PREFIX) ||
                        componentDependency.startsWith(NPM_PREFIX) ? YARN : BOWER,
                projectType: ProjectType.get(project)
        )
    }

}
