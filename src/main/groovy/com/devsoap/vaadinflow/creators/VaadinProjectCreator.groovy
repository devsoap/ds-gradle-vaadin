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
package com.devsoap.vaadinflow.creators

import com.devsoap.vaadinflow.models.ApplicationType
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

        File languageSourceDir = new File(sourceMain, vaadinProject.projectType.sourceDir)
        String sourceFileExtension = vaadinProject.projectType.extension

        File pkgDir = Paths.get(languageSourceDir.canonicalPath,
                vaadinProject.applicationPackage.split('\\.')).toFile()
        String appClassName = TemplateWriter.makeStringJavaCompatible(vaadinProject.applicationName)

        if (vaadinProject.compatibilityMode) {
            TemplateWriter.builder()
                    .templateFileName("Servlet.$sourceFileExtension")
                    .targetDir(pkgDir)
                    .targetFileName("${appClassName}Servlet.$sourceFileExtension")
                    .substitutions([
                    'applicationPackage' : vaadinProject.applicationPackage,
                    'applicationName' : vaadinProject.applicationName,
                    'productionMode' : vaadinProject.productionMode
            ])
                    .build().write()
        }

        TemplateWriter.builder()
                .templateFileName("AppView.$sourceFileExtension")
                .targetDir(pkgDir)
                .targetFileName("${appClassName}View.$sourceFileExtension")
                .substitutions([
                        'applicationPackage': vaadinProject.applicationPackage,
                        'applicationName' : appClassName,
                ])
                .build().write()

        TemplateWriter.builder()
                .templateFileName(vaadinProject.compatibilityMode ?
                    "UI.$sourceFileExtension" : "UI.14.$sourceFileExtension")
                .targetDir(pkgDir)
                .targetFileName("${appClassName}UI.$sourceFileExtension")
                .substitutions([
                        'applicationPackage': vaadinProject.applicationPackage,
                        'applicationName' : appClassName,
                        'applicationTheme' : vaadinProject.applicationName.toLowerCase() +
                                "-theme.${vaadinProject.compatibilityMode ? 'html' : 'css'}",
                        'applicationBaseTheme' : vaadinProject.applicationBaseTheme
                ])
                .build().write()

        if (vaadinProject.applicationType == ApplicationType.SPRING_BOOT) {
            TemplateWriter.builder()
                .templateFileName("SpringBootApplication.$sourceFileExtension")
                .targetDir(pkgDir)
                .targetFileName("${appClassName}Application.$sourceFileExtension")
                .substitutions([
                    'applicationPackage': vaadinProject.applicationPackage,
                    'applicationName' : appClassName
                ])
                .build().write()
        }
    }
}
