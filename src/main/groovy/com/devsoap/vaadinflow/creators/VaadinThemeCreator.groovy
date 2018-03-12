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
package com.devsoap.vaadinflow.creators

import com.devsoap.vaadinflow.models.VaadinProject
import com.devsoap.vaadinflow.util.TemplateWriter

import java.nio.file.Paths

/**
 * Creates Vaadin themes
 *
 * @author John Ahlroos
 * @since 1.0
 */
class VaadinThemeCreator {

    /**
     * Generates a Css theme that can be included with the @StyleSheet annotation
     *
     * @param vaadinProject
     *      the project model
     */
    void generateCssTheme(VaadinProject vaadinProject) {

        File root = vaadinProject.rootDirectory
        File webappDir = Paths.get(root.canonicalPath, 'src', 'main', 'webapp').toFile()
        File frontendDir = new File(webappDir, 'frontend')
        String appClassName = TemplateWriter.makeStringJavaCompatible(vaadinProject.applicationName)

        TemplateWriter.builder()
                .templateFileName('AppTheme.css')
                .targetDir(frontendDir)
                .targetFileName("${appClassName.toLowerCase()}-theme.css")
                .build().write()

    }
}
