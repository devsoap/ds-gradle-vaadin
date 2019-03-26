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
package com.devsoap.vaadinflow

import com.devsoap.vaadinflow.tasks.NodeSetupTask
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.npm.NpmInstallTask
import com.moowork.gradle.node.npm.NpmSetupTask
import com.moowork.gradle.node.npm.NpmTask
import com.moowork.gradle.node.task.NodeTask
import com.moowork.gradle.node.variant.VariantBuilder
import com.moowork.gradle.node.yarn.YarnInstallTask
import com.moowork.gradle.node.yarn.YarnSetupTask
import com.moowork.gradle.node.yarn.YarnTask
import groovy.transform.Internal
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A modified version of com.moowork.gradle.node.NodePlugin to fix Gradle 6 issues
 *
 * <b>Note:</b> This class is internal and might be removed if the upstream plugin is updated. Do not rely on this
 * class to exist.
 *
 *  @author John Ahlroos
 *  @since 1.1
 */
@Internal
class NodePlugin implements Plugin<Project> {

    private Project project
    private NodeExtension config
    private NodeSetupTask setupTask
    private NpmSetupTask npmSetupTask
    private YarnSetupTask yarnSetupTask

    @Override
    void apply( final Project project ) {
        this.project = project
        this.config = NodeExtension.create( this.project )
        addGlobalTypes()
        addTasks()
        project.afterEvaluate {
            this.config.variant = new VariantBuilder( this.config ).build()
            configureSetupTask()
            configureNpmSetupTask()
            configureYarnSetupTask()
        }
    }

    private void addGlobalTypes() {
        addGlobalTaskType( NodeTask )
        addGlobalTaskType( NpmTask )
        addGlobalTaskType( YarnTask )
    }

    private void addTasks() {
        this.project.tasks.create( NpmInstallTask.NAME, NpmInstallTask )
        this.project.tasks.create( YarnInstallTask.NAME, YarnInstallTask )
        this.setupTask = this.project.tasks.create( NodeSetupTask.NAME, NodeSetupTask )
        this.npmSetupTask = this.project.tasks.create( NpmSetupTask.NAME, NpmSetupTask )
        this.yarnSetupTask = this.project.tasks.create( YarnSetupTask.NAME, YarnSetupTask )
    }

    private void addGlobalTaskType( Class type ) {
        project.extensions.extraProperties.set( type.simpleName, type )
    }

    private void configureSetupTask() {
        setupTask.setEnabled( config.download )
    }

    private void configureNpmSetupTask() {
        npmSetupTask.configureVersion( config.npmVersion )
    }

    private void configureYarnSetupTask() {
        yarnSetupTask.configureVersion( config.yarnVersion )
    }
}
