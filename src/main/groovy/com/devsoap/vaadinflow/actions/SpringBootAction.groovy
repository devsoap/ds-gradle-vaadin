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

import static com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension.WEBAPP_DIR

import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.War
import java.nio.file.Paths

/**
 * Configures the Spring Boot plugin to support the Vaadin resources
 *
 * @author John Ahlroos
 * @since 1.0
 */
class SpringBootAction extends PluginAction {

    final String pluginId = 'org.springframework.boot'

    private static final String BOOT_RUN_TASK = 'bootRun'

    private static final String BOOT_JAR_TASK = 'bootJar'
    private static final String BOOT_WAR_TASK = 'bootWar'

    private static final String WEBAPP_GEN = 'webapp-gen'
    private static final String SPRING_BOOT_RESOURCES_PATH = 'META-INF/resources'

    @Override
    protected void execute(Project project) {
        super.execute(project)

        File webappDir = Paths.get(project.projectDir.canonicalPath, WEBAPP_DIR).toFile()
        File webappGen = Paths.get(project.buildDir.canonicalPath, WEBAPP_GEN).toFile()

        // Configure source set to include web resources
        JavaPluginConvention javaPlugin =  project.convention.getPlugin(JavaPluginConvention)
        SourceSet mainSourceSet = javaPlugin.sourceSets.main
        mainSourceSet.resources.srcDir(webappDir)
        mainSourceSet.resources.srcDir(webappGen)

        // Configure tasks
        project.tasks.with {
            findByName(BOOT_JAR_TASK)?.dependsOn(AssembleClientDependenciesTask.NAME)
            findByName(BOOT_WAR_TASK)?.dependsOn(AssembleClientDependenciesTask.NAME)
            findByName(BOOT_RUN_TASK)?.with {
                dependsOn(AssembleClientDependenciesTask.NAME)
                sourceResources(mainSourceSet)
            }
        }
    }

    @Override
    protected void beforeTaskExecuted(Task task) {
        super.beforeTaskExecuted(task)
        switch (task.name) {
            case BOOT_RUN_TASK:
                configureBootRun(task)
                break
            case BOOT_JAR_TASK:
                configureBootJar(task)
                break
            case BOOT_WAR_TASK:
                configureBootWar(task)
                break
        }
    }

    private static void configureBootRun(Task task) {
        JavaExec bootRun = (JavaExec) task
        VaadinFlowPluginExtension vaadin = task.project.extensions.getByType(VaadinFlowPluginExtension)
        bootRun.jvmArgs( bootRun.jvmArgs + ["-Dvaadin.productionMode=$vaadin.productionMode".toString()] )
        if (vaadin.productionMode) {
            // TODO Resolve if supporting production mode is even possible with bootRun
            throw new GradleException('Production mode is not supported with bootRun. ' +
                    'Instead package with bootJar or bootWar and execute them directly.')
        }
    }

    private static void configureBootJar(Task task) {
        Jar jar = (Jar) task
        Project project = task.project

        // Include web-app dirs
        File webappDir = Paths.get(project.projectDir.canonicalPath, WEBAPP_DIR).toFile()
        jar.from(webappDir) {
            it.into(SPRING_BOOT_RESOURCES_PATH)
        }

        // Include web-app gen
        File webappGen = Paths.get(project.buildDir.canonicalPath, WEBAPP_GEN).toFile()
        jar.from(webappGen) {
            it.into(SPRING_BOOT_RESOURCES_PATH)
        }
    }

    private static void configureBootWar(Task task) {
        War war = (War) task
        war.from(new File(task.project.buildDir, WEBAPP_GEN))
    }
}
