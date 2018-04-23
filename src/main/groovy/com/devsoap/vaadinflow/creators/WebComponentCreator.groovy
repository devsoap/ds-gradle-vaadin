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

import com.devsoap.vaadinflow.models.WebComponent
import com.devsoap.vaadinflow.util.TemplateWriter

import java.nio.file.Paths

/**
 * Creates a new Web component from a template
 *
 * @author John Ahlroos
 * @since 1.0
 */
class WebComponentCreator {

    /**
     * Generates the component
     *
     * @param webComponent
     *      the model of the component
     */
    void generate(WebComponent webComponent) {

        File root = webComponent.rootDirectory
        File javaSourceDir = Paths.get(root.canonicalPath, 'src', 'main', 'java').toFile()
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, webComponent.componentPackage.split('\\.')).toFile()
        String componentClassName = TemplateWriter.makeStringJavaCompatible(webComponent.componentName)

        TemplateWriter.builder()
                .targetDir(pkgDir)
                .templateFileName('WebComponent.java')
                .targetFileName("${componentClassName}.java")
                .substitutions([
                    'componentPackage' : webComponent.componentPackage,
                    'componentTag' : webComponent.componentTag,
                    'dependencyPackage': webComponent.dependencyPackage,
                    'dependencyHtml': webComponent.dependencyHtml,
                    'componentName' : componentClassName
                ]).build().write()
    }
}
