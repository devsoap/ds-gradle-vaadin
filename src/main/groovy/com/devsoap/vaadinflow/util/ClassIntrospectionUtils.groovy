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
package com.devsoap.vaadinflow.util

import groovy.util.logging.Log
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import io.github.classgraph.ScanResult
import org.gradle.api.Project

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher

/**
 * Classpath introspection utilities
 *
 * @author John Ahlroos
 * @since 1.2
 */
@Log('LOGGER')
class ClassIntrospectionUtils {

    private static final String HTML_IMPORT_FQN = 'com.vaadin.flow.component.dependency.HtmlImport'
    private static final String JS_IMPORT_FQN = 'com.vaadin.flow.component.dependency.JavaScript'
    private static final String STYLE_IMPORT_FQN = 'com.vaadin.flow.component.dependency.Stylesheet'
    private static final String ABSTRACT_THEME_FQN = 'com.vaadin.flow.theme.AbstractTheme'
    private static final String FRONTEND_PROTOCOL = 'frontend://'
    private static final String FRONTEND_DIR = 'frontend'

    /**
     * Get all HTML imports
     *
     * @param scan
     *       The classpath scan with the annotations
     * @return
     *      a list of HTML imports
     */
    static final Set<String> findHtmlImports(Project project, ScanResult scan, String selectedTheme) {
        Map<String, Map<String,String>> themes = findThemes(project)
        Set<String> htmlImports = []
        scan.getClassesWithAnnotation(HTML_IMPORT_FQN).each {
            it.getAnnotationInfoRepeatable(HTML_IMPORT_FQN).each {
                String path = it.parameterValues.value.value.toString() - FRONTEND_PROTOCOL
                File frontendDir = new File(project.buildDir, FRONTEND_DIR)
                File file = new File(frontendDir, path)
                if (file.exists()) {
                    addImport(project, file, htmlImports, path, themes.get(selectedTheme, null))
                }
            }
        }
        htmlImports
    }

    /**
     * Get all Javascript imports
     *
     * @param scan
     *       The classpath scan with the annotations
     * @return
     *        a list of HTML imports
     */
    static final Set<String> findJsImports(ScanResult scan) {
        Set<String> jsImports = []
        scan.getClassesWithAnnotation(JS_IMPORT_FQN).each {
            it.getAnnotationInfoRepeatable(JS_IMPORT_FQN).each {
                jsImports << it.parameterValues.value.value.toString() - FRONTEND_PROTOCOL
            }
        }
        jsImports
    }

    /**
     * Get all Stylesheet imports
     *
     * @param scan
     *      The classpath scan with the annotations
     * @return
     *      a list of HTML imports
     */
    static final Set<String> findStylesheetImports(ScanResult scan) {
        Set<String> styleImports = []
        scan.getClassesWithAnnotation(STYLE_IMPORT_FQN).each {
            it.getAnnotationInfoRepeatable(STYLE_IMPORT_FQN).each {
                styleImports << it.parameterValues.value.value.toString() - FRONTEND_PROTOCOL
            }
        }
        styleImports
    }

    /**
     * Performs an annotation scan on the project classpath
     *
     * @param project
     *      the project to scan
     * @return
     *      the result of the scan
     */
    static ScanResult getAnnotationScan(Project project) {
        new ClassGraph()
                .overrideClassLoaders(getClassLoader(project))
                .enableAnnotationInfo()
                .scan()
    }

    private static Map<String, Map<String,String>> findThemes(Project project) {
        ClassLoader cl = getClassLoader(project)
        ClassInfoList themeClasses = new ClassGraph()
                .overrideClassLoaders(cl)
                .enableClassInfo()
                .scan()
                .getClassesImplementing(ABSTRACT_THEME_FQN)

        Map<String, Map<String,String>> themes = [:]
        themeClasses.each {
            Object instance = cl.loadClass(it.name).newInstance()
            themes.put(it.simpleName.toLowerCase(), [ 'baseUrl' :  instance.baseUrl, 'themeUrl' : instance.themeUrl ])
        }
        themes
    }

    private static URLClassLoader getClassLoader(Project project) {
        URL[] classpath = []
        classpath += project.sourceSets.main.runtimeClasspath.collect { it.toURI().toURL() } as URL[]
        classpath += project.sourceSets.main.compileClasspath.collect { it.toURI().toURL() } as URL[]
        new URLClassLoader(classpath)
    }

    private static void findHTMLImportsRecursively(Project project, File template, Set<String> imports,
                                                   Map<String,String> theme=null) {
        File frontendDir = new File(project.buildDir, FRONTEND_DIR)
        template.text.findAll('.*rel="import".*href="(.*)".*').collect {
            Matcher matcher = (it =~ /href=\"(.*)\"/)
            matcher ? matcher.group(1) : null
        }.findAll { String importPath ->
            !importPath?.startsWith('http')
        }.each { String importPath ->
            Path templatePath = Paths.get(template.canonicalPath)
            Path templateFolder = templatePath.parent
            Path relativePath = Paths.get(importPath)
            Path resolvedPath = templateFolder.resolve(relativePath)
            File importFile =  resolvedPath.toFile()
            if (importFile.exists()) {
                String htmlImport = (importFile.canonicalPath - frontendDir.canonicalPath).substring(1)
                addImport(project, importFile, imports, htmlImport, theme)
            }
        }
    }

    private static void addImport(Project project, File importFile , Set<String> imports, String htmlImport,
                                  Map<String,String> theme) {
        File frontendDir = new File(project.buildDir, FRONTEND_DIR)
        if (!imports.contains(htmlImport)) {
            if (theme && htmlImport.contains(theme.baseUrl) ) {
                String htmlThemeImport = htmlImport.replace(theme.baseUrl, theme.themeUrl)
                File themeFile = new File(frontendDir, htmlThemeImport)
                if (themeFile.exists()) {
                    imports.add(htmlThemeImport)
                } else {
                    imports.addAll(htmlImport)
                }
            } else {
                imports.add(htmlImport)
            }

            findHTMLImportsRecursively(project, importFile, imports, theme)
        }
    }

}
