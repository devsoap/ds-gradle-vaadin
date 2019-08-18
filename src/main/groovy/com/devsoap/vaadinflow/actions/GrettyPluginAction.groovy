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

import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import com.devsoap.vaadinflow.tasks.WrapCssTask
import com.devsoap.vaadinflow.util.PackagingUtil
import com.devsoap.vaadinflow.util.Versions
import com.devsoap.vaadinflow.util.WebJarHelper
import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar

/**
 * Configures the Gretty plugin to be compatible
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class GrettyPluginAction extends PluginAction {

    final String pluginId = 'org.gretty'

    private static final String PREPARE_INPLACE_WEB_APP_FOLDER_TASK = 'prepareInplaceWebAppFolder'
    private static final String SERVLET_CONTAINER = 'jetty9.4'
    private static final String JAR_TASK = 'jar'

    private boolean buildingProduct = false

    @Override
    void apply(Project project) {
        project.ext.jetty94Version = Versions.rawVersion('gretty.jetty.version')
        super.apply(project)
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)
        project.gretty.servletContainer = SERVLET_CONTAINER
        buildingProduct = project.gradle.startParameter.taskNames.findAll { it.contains('buildProduct') }.size() > 0
    }

    @Override
    protected void executeAfterAllEvaluations() {
        super.executeAfterAllEvaluations()
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        project.gretty.systemProperties = project.gretty.systemProperties ?: [:]
        project.gretty.systemProperties.putIfAbsent 'vaadin.productionMode', vaadin.productionMode
        project.gretty.systemProperties.putIfAbsent 'vaadin.compatibilityMode', vaadin.compatibilityMode
        project.gretty.systemProperties.put 'vaadin.enableDevServer', false
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)
        project.tasks[PREPARE_INPLACE_WEB_APP_FOLDER_TASK].dependsOn(AssembleClientDependenciesTask.NAME)
        project.tasks[PREPARE_INPLACE_WEB_APP_FOLDER_TASK].dependsOn(WrapCssTask.NAME)

        if (buildingProduct) {
            project.tasks[JAR_TASK].dependsOn(AssembleClientDependenciesTask.NAME)
        }
    }

    @Override
    protected void afterTaskExecuted(Task task) {
        super.afterTaskExecuted(task)
        if (task.name in [PREPARE_INPLACE_WEB_APP_FOLDER_TASK,
                          WrapCssTask.NAME,
                          AssembleClientDependenciesTask.NAME]) {
            LOGGER.info('Copying generated web-app resources into extracted webapp')
            task.project.copy { copy ->
                    copy.from("${task.project.buildDir.canonicalPath}/webapp-gen")
                            .into("${task.project.buildDir.canonicalPath}/inplaceWebapp")
            }
        }
    }

    @Override
    protected void beforeTaskExecuted(Task task) {
        super.beforeTaskExecuted(task)
        if (task.name == JAR_TASK && buildingProduct) {
            Jar jar = (Jar) task

            // Include resources
            PackagingUtil.includeResourcesInJar(jar, '/')

            // Include classes
            jar.from(jar.project.sourceSets.main.output) { CopySpec spec ->
                spec.into('/WEB-INF/classes')
            }

            // Include dependencies
            List<Configuration> configurations = WebJarHelper.findConfigurations(jar.project)
            configurations.each {
                jar.from(it) { CopySpec spec ->
                    spec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    spec.into('/WEB-INF/lib')
                }
            }
        }
    }
}
