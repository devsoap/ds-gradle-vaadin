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
package com.devsoap.vaadinflow.actions

import com.devsoap.vaadinflow.VaadinFlowPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.plugins.PluginManager
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskState

/**
 * Base class for reacting to other plugins
 *
 * @author John Ahlroos
 * @since 1.0
 */
abstract class PluginAction {

    private final TaskListener taskListener = new TaskListener()

    protected Project project

    /**
     * The unique identifer for the plugin
     */
    abstract String getPluginId()

    /**
     * Executes the defined action when then Gradle Vaadin plugin is applied. This is only called
     * if the user has applied the plugin id for this plugin action.
     *
     * @param project
     *      the project to apply the action to
     */
    protected void execute(Project project) {
        project.logger.info("Applying ${getClass().simpleName} actions to project $project.name")
    }

    /**
     * This is called after project evaluation.
     *
     * @param project
     *      the project which was evaluated
     */
    protected void executeAfterEvaluate(Project project) {
        project.logger.debug("Executing afterEvaluate hook for ${getClass().simpleName}")
    }

    /**
     * This is called after all projects have been evaluated
     */
    protected void executeAfterAllEvaluations() {
        project.logger.debug("Executing afterAllEvaulations hook for ${getClass().simpleName}")
    }

    /**
     * Called before a task is executed. This is only called if the user has applied the plugin id for
     * this plugin action.
     *
     * @param task
     *      task that will be executed
     */
    protected void beforeTaskExecuted(Task task) {
        task.project.logger.debug("Executing pre task hook for ${getClass().simpleName} for task $task.name")
    }

    /**
     * Called after a task has executed. This is only called if the user has applied the plugin id for
     * this plugin action.
     *
     * @param task
     *      task that was executed
     */
    protected void afterTaskExecuted(Task task) {
        task.project.logger.debug("Executing post task hook for ${getClass().simpleName} for task $task.name")
    }

    /**
     * Applies the plugin action to a project.
     *
     * Please note that by applying the action to a project only adds the support for the action to the project. You
     * also have to apply the plugin id the action is for to execute action.
     *
     * @param project
     *      the project to apply the plugin action to
     */
    void apply(Project project) {
        this.project = project
        project.plugins.withId(pluginId) {
            project.gradle.taskGraph.removeTaskExecutionListener(taskListener)
            project.gradle.taskGraph.addTaskExecutionListener(taskListener)
            project.gradle.projectsEvaluated {
                executeAfterAllEvaluations()
            }
            execute(project)
            project.afterEvaluate {
                executeAfterEvaluate(project)
            }
        }
    }

    /**
     * Returns the internal tasklistener
     */
    @Internal
    protected TaskListener getTaskListener() {
        taskListener
    }

    /**
     * Is the task applicable for the project. The task is applicable iff the Gradle Vaadin plugin is applied to the
     * project.
     *
     * @param task
     *      the task to check.
     *
     * @return
     *      <code>true</code> if the task is applicable to the project.
     */
    final static boolean isApplicable(Task task) {
        PluginManager pluginManager = task.project.pluginManager
        pluginManager.hasPlugin(VaadinFlowPlugin.PLUGIN_ID)
    }

    private final class TaskListener implements TaskExecutionListener {

        @Override
        void beforeExecute(Task task) {
            if (isApplicable(task) ) {
                beforeTaskExecuted(task)
            }
        }

        @Override
        void afterExecute(Task task, TaskState state) {
            if (isApplicable(task) && state.executed) {
                afterTaskExecuted(task)
            }
        }
    }
}
