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

import com.devsoap.vaadinflow.actions.JavaPluginAction
import com.devsoap.vaadinflow.models.Component
import com.devsoap.vaadinflow.models.Composite
import com.devsoap.vaadinflow.models.ProjectType
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
        File sourceDir = getSourceDirectory(root, webComponent.projectType)
        File pkgDir = Paths.get(sourceDir.canonicalPath, webComponent.componentPackage.split(DOT_REGEX)).toFile()
        String componentClassName = TemplateWriter.makeStringJavaCompatible(webComponent.componentName)
        String extension = webComponent.projectType.extension

        TemplateWriter.builder()
                .targetDir(pkgDir)
                .templateFileName(webComponent.compatibilityMode ?
                    "WebComponent.$extension" : "WebComponent.v14.$extension")
                .targetFileName("${componentClassName}.$extension")
                .substitutions([
                    'componentPackage' : webComponent.componentPackage,
                    'componentTag' : webComponent.componentTag,
                    'dependencyPackage': webComponent.dependencyPackage,
                    'dependencyHtml': webComponent.dependencyHtml,
                    'componentName' : componentClassName,
                    'packageManager' : webComponent.packageManager.packageDir
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
        File javaSourceDir = getSourceDirectory(root, webTemplate.projectType)
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, webTemplate.componentPackage.split(DOT_REGEX)).toFile()
        String componentClassName = TemplateWriter.makeStringJavaCompatible(webTemplate.componentName)
        String extension = webTemplate.projectType.extension
        String templateExtension = webTemplate.projectType.getTemplateExtension(webTemplate.compatibilityMode)

        TemplateWriter.builder()
                .targetDir(pkgDir)
                .templateFileName(webTemplate.compatibilityMode ?
                    "WebTemplate.$extension" : "WebTemplate.v14.$extension")
                .targetFileName("${componentClassName}.$extension")
                .substitutions([
                    'componentPackage' : webTemplate.componentPackage,
                    'componentTag' : webTemplate.componentTag,
                    'componentName' : componentClassName,
        ]).build().write()

        TemplateWriter.builder()
                .targetDir(getTemplatesDir(
                    webTemplate.webappDirectory, webTemplate.rootDirectory, webTemplate.compatibilityMode))
                .templateFileName("WebTemplate.$templateExtension")
                .targetFileName(webTemplate.compatibilityMode ?
                    "${componentClassName}.$templateExtension" : "${componentClassName}Element.$templateExtension")
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
        File javaSourceDir = getSourceDirectory(root, composite.projectType)
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, composite.componentPackage.split(DOT_REGEX)).toFile()
        String componentClassName = TemplateWriter.makeStringJavaCompatible(composite.componentName)
        String extension = composite.projectType.extension

        TemplateWriter.builder()
                .targetDir(pkgDir)
                .templateFileName("Composite.$extension")
                .targetFileName("${componentClassName}.$extension")
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
        File javaSourceDir = getSourceDirectory(root, component.projectType)
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, component.componentPackage.split(DOT_REGEX)).toFile()
        String componentClassName = TemplateWriter.makeStringJavaCompatible(component.componentName)
        String extension = component.projectType.extension

        TemplateWriter.builder()
                .targetDir(pkgDir)
                .templateFileName("Component.$extension")
                .targetFileName("${componentClassName}.$extension")
                .substitutions([
                'componentPackage' : component.componentPackage,
                'componentTag' : component.componentTag,
                'componentName' : componentClassName,
        ]).build().write()
    }

    private static File getSourceDirectory(File root, ProjectType projectType) {
        Paths.get(root.canonicalPath, SRC, MAIN, projectType.sourceDir).toFile()
    }

    private static File getTemplatesDir(File webappDir, File rootDir, boolean compatibilityMode) {
        if (compatibilityMode) {
            Paths.get(webappDir.canonicalPath, 'frontend', 'templates').toFile()
        } else {
           Paths.get(rootDir.canonicalPath, JavaPluginAction.JAVASCRIPT_SOURCES.split('/')).toFile()
        }
    }
}
