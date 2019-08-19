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

import com.devsoap.vaadinflow.tasks.WrapCssTask
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Integrate JSASS Java plugin
 *
 * @author John Ahlroos
 * @since 1.1
 */
class SassJavaPluginAction extends PluginAction {

    final String pluginId = 'io.freefair.jsass-java'

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)
        WrapCssTask convertCss = project.tasks.getByName(WrapCssTask.NAME)

        Task compileSass = project.tasks.getByName('compileSass')
        convertCss.dependsOn(compileSass)
        convertCss.inputs.files(compileSass.outputs.files)
    }
}
