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
package com.devsoap.vaadinflow.creators

import com.devsoap.vaadinflow.actions.JavaPluginAction
import com.devsoap.vaadinflow.models.VaadinProject
import com.devsoap.vaadinflow.util.TemplateWriter
import groovy.util.logging.Log

import java.nio.file.Paths

/**
 * Creates Vaadin themes
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinThemeCreator {

    /**
     * Generates a Css theme that can be included with the @StyleSheet annotation
     *
     * @param vaadinProject
     *      the project model
     */
    void generateCssTheme(VaadinProject vaadinProject) {
        String appClassName = TemplateWriter.makeStringJavaCompatible(vaadinProject.applicationName)
        String themeName = vaadinProject.compatibilityMode ? "${appClassName.toLowerCase()}-theme.css" : 'theme.css'
        TemplateWriter.builder()
                .templateFileName('AppTheme.css')
                .targetDir(getStylesDir(vaadinProject))
                .targetFileName(themeName)
                .build().write()
    }

    private static File getStylesDir(VaadinProject vaadinProject) {
        vaadinProject.compatibilityMode ?
            Paths.get(vaadinProject.webappDirectory.canonicalPath,
                    'frontend', 'styles').toFile() :
            Paths.get(vaadinProject.rootDirectory.canonicalPath,
                    JavaPluginAction.STYLESHEETS_SOURCES.split('/')).toFile()
    }
}
