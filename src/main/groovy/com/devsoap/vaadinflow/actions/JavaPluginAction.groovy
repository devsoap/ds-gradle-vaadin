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

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

/**
 * Configures the Java plugin
 *
 * @author John Ahlroos
 * @since 1.3
 */
class JavaPluginAction extends PluginAction {

    final String pluginId = 'java'

    static final String JAVASCRIPT_SOURCES = 'src/main/javascript'
    static final String STYLESHEETS_SOURCES = 'src/main/stylesheets'

    @Override
    protected void execute(Project project) {
        super.execute(project)
        JavaPluginConvention javaPlugin =  project.convention.getPlugin(JavaPluginConvention)
        javaPlugin.sourceSets.main.resources.with {
            if (project.file(JAVASCRIPT_SOURCES).exists()) {
                srcDir(JAVASCRIPT_SOURCES)
            }
            if (project.file(STYLESHEETS_SOURCES).exists()) {
                srcDir(STYLESHEETS_SOURCES)
            }
        }
    }
}
