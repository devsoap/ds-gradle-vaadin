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

import com.devsoap.vaadinflow.models.Component
import com.devsoap.vaadinflow.models.Composite
import com.devsoap.vaadinflow.models.VaadinProject
import com.devsoap.vaadinflow.models.WebComponent
import com.devsoap.vaadinflow.models.WebTemplate
import com.devsoap.vaadinflow.util.TemplateWriter

import java.nio.file.Paths

/**
 * Creates a new Web component from a template
 *
 * @author John Ahlroos
 * @since 1.0
 */
class ComponentCreator {

    private static final String DOT_REGEX = '\\.'
    private static final String SRC = 'src'
    private static final String MAIN = 'main'

    /**
     * Creates a new Web component from a template
     *
     * @param webComponent
     *      the model of the component
     */
    void generate(WebComponent webComponent) {

        File root = webComponent.rootDirectory
        File javaSourceDir = getJavaSourceDirectory(root)
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, webComponent.componentPackage.split(DOT_REGEX)).toFile()
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
                    'componentName' : componentClassName,
                    'packageManager' : webComponent.packageManager.package
                ]).build().write()
    }

    /**
     * Creates a new Web template from a template
     *
     * @param webTemplate
     *      the model of the template
     */
    void generate(WebTemplate webTemplate) {
        File root = webTemplate.rootDirectory
        File javaSourceDir = getJavaSourceDirectory(root)
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, webTemplate.componentPackage.split(DOT_REGEX)).toFile()
        String componentClassName = TemplateWriter.makeStringJavaCompatible(webTemplate.componentName)

        TemplateWriter.builder()
                .targetDir(pkgDir)
                .templateFileName('WebTemplate.java')
                .targetFileName("${componentClassName}.java")
                .substitutions([
                    'componentPackage' : webTemplate.componentPackage,
                    'componentTag' : webTemplate.componentTag,
                    'componentName' : componentClassName,
        ]).build().write()

        TemplateWriter.builder()
                .targetDir(getTemplatesDir(root))
                .templateFileName('WebTemplate.html')
                .targetFileName("${componentClassName}.html")
                .substitutions([
                    'componentPackage' : webTemplate.componentPackage,
                    'componentTag' : webTemplate.componentTag,
                    'componentName' : componentClassName,
        ]).build().write()
    }

    /**
     * Create a new Composite component from a template
     *
     * @param composite
     *      the model of the composite to create
     */
    void generate(Composite composite) {

        File root = composite.rootDirectory
        File javaSourceDir = getJavaSourceDirectory(root)
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, composite.componentPackage.split(DOT_REGEX)).toFile()
        String componentClassName = TemplateWriter.makeStringJavaCompatible(composite.componentName)

        TemplateWriter.builder()
                .targetDir(pkgDir)
                .templateFileName('Composite.java')
                .targetFileName("${componentClassName}.java")
                .substitutions([
                'componentPackage'  : composite.componentPackage,
                'componentBaseClass': composite.componentBaseClass,
                'componentName'     : componentClassName,
        ]).build().write()
    }

    /**
     * Create a plain Vaadin component
     *
     * @param component
     *      the model of the component to create
     */
    void generate(Component component) {

        File root = component.rootDirectory
        File javaSourceDir = getJavaSourceDirectory(root)
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, component.componentPackage.split(DOT_REGEX)).toFile()
        String componentClassName = TemplateWriter.makeStringJavaCompatible(component.componentName)

        TemplateWriter.builder()
                .targetDir(pkgDir)
                .templateFileName('Component.java')
                .targetFileName("${componentClassName}.java")
                .substitutions([
                'componentPackage' : component.componentPackage,
                'componentTag' : component.componentTag,
                'componentName' : componentClassName,
        ]).build().write()
    }

    private static File getJavaSourceDirectory(File root) {
        Paths.get(root.canonicalPath,  SRC, MAIN, 'java').toFile()
    }

    private static File getTemplatesDir(File root) {
        Paths.get(root.canonicalPath, SRC, MAIN, 'webapp', 'frontend', 'templates').toFile()
    }
}
