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
 * Creates Vaadin projects from templates
 *
 * @author John Ahlroos
 * @since 1.0
 */
class VaadinProjectCreator {

    /**
     * Creates the source files and file structure according to the VaadinProject model and the associated file
     * templates
     *
     * @param vaadinProject
     *      the project model
     */
    void generate(VaadinProject vaadinProject) {

        File root = vaadinProject.rootDirectory
        File sourceMain = Paths.get(root.canonicalPath, 'src', 'main').toFile()
        File javaSourceDir = new File(sourceMain, 'java')
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, vaadinProject.applicationPackage.split('\\.')).toFile()
        String appClassName = TemplateWriter.makeStringJavaCompatible(vaadinProject.applicationName)
        File webappDir = new File(sourceMain, 'webapp')
        File frontendDir = new File(webappDir, 'frontend')

        TemplateWriter.builder()
                .templateFileName('Servlet.java')
                .targetDir(pkgDir)
                .targetFileName("${appClassName}Servlet.java")
                .substitutions([
                    'applicationPackage' : vaadinProject.applicationPackage,
                    'applicationName' : vaadinProject.applicationName,
                    'productionMode' : vaadinProject.productionMode
                ])
                .build().write()

        TemplateWriter.builder()
                .templateFileName('AppView.java')
                .targetDir(pkgDir)
                .targetFileName("${appClassName}View.java")
                .substitutions([
                        'applicationPackage': vaadinProject.applicationPackage,
                        'applicationName' : appClassName,
                ])
                .build().write()

        TemplateWriter.builder()
                .templateFileName('UI.java')
                .targetDir(pkgDir)
                .targetFileName("${appClassName}UI.java")
                .substitutions([
                        'applicationPackage': vaadinProject.applicationPackage,
                        'applicationName' : appClassName,
                        'applicationTheme' : vaadinProject.applicationName.toLowerCase() + '-theme.html',
                        'applicationBaseTheme' : vaadinProject.applicationBaseTheme
                ])
                .build().write()

        TemplateWriter.builder()
                .templateFileName('index.html')
                .targetDir(frontendDir)
                .build().write()
    }
}
