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
package com.devsoap.vaadinflow.util

import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import groovy.util.logging.Log
import io.github.classgraph.AnnotationInfo
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import io.github.classgraph.Resource
import io.github.classgraph.ResourceList
import io.github.classgraph.ScanResult
import org.gradle.api.GradleException
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
    private static final String NPM_PACKAGE_IMPORT_FQN = 'com.vaadin.flow.component.dependency.NpmPackage'
    private static final String ABSTRACT_THEME_FQN = 'com.vaadin.flow.theme.AbstractTheme'
    private static final String JS_MODULE_FQN = 'com.vaadin.flow.component.dependency.JsModule'
    private static final String CSS_IMPORT_FQN = 'com.vaadin.flow.component.dependency.CssImport'
    private static final String ID_FQN = 'com.vaadin.flow.component.polymertemplate.Id'
    private static final String ROUTE_FQN = 'com.vaadin.flow.router.Route'
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
    static final Map<String, File> findHtmlImports(Project project, ScanResult scan, String selectedTheme) {
        Map<String, Map<String,String>> themes = findThemes(project)
        Map<String,File> htmlImports = [:]
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
    @Deprecated
    static final Map<String, String> findJsImports(ScanResult scan) {
        Map<String, String> jsImports = [:]
        scan.getClassesWithAnnotation(JS_IMPORT_FQN).each { clz ->
            clz.getAnnotationInfoRepeatable(JS_IMPORT_FQN).each {
                jsImports.put(it.parameterValues.value.value.toString() - FRONTEND_PROTOCOL, clz.name)
            }
        }
        jsImports
    }

    /**
     * Get all Javascript imports by route
     *
     * @since 1.3
     * @param scan
     *       The classpath scan with the annotations
     * @return
     *        a list of Js imports
     */
    static final Map<String, String> findJsImportsByRoute(ScanResult scan) {
        Map<String, String> modules = [:]
        List<String> processedClasses = []
        scan.getClassesWithAnnotation(ROUTE_FQN).each { ClassInfo ci ->
            findImportModulesByDependencies(ci, modules, JS_IMPORT_FQN, processedClasses)
        }
        LOGGER.info("Scanned ${processedClasses.size()} classes.")
        modules
    }

    /**
     * Get all Javascript imports by whitelist
     *
     * @since 1.3
     * @param scan
     *       The classpath scan with the annotations
     * @return
     *        a list of Js imports
     */
    static final Map<String, String> findJsImportsByWhitelist(ScanResult scan) {
        Map<String, String> jsImports = [:]
        scan.getClassesWithAnnotation(JS_IMPORT_FQN).each { clz ->
            clz.getAnnotationInfoRepeatable(JS_IMPORT_FQN).each {
                jsImports.put(it.parameterValues.value.value.toString(), clz.name)
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
    static final Map<String, String> findStylesheetImports(ScanResult scan) {
        Map<String,String> styleImports = [:]
        scan.getClassesWithAnnotation(STYLE_IMPORT_FQN).each { clz ->
            clz.getAnnotationInfoRepeatable(STYLE_IMPORT_FQN).each {
                styleImports.put(it.parameterValues.value.value.toString() - FRONTEND_PROTOCOL, clz.name)
            }
        }
        styleImports
    }

    /**
     * Get all NPM dependencies from classpath
     *
     * @param scan
     *      The classpath scan with the annotations
     * @return
     *      the Npm packages as a packageName -> version mapping
     */
    static final Map<String,String> findNpmPackages(ScanResult scan) {
        Map<String,String> npmPackages = [:]
        scan.getClassesWithAnnotation(NPM_PACKAGE_IMPORT_FQN).each {
            it.getAnnotationInfo(NPM_PACKAGE_IMPORT_FQN).each {
                String packageName = it.parameterValues.getValue('value')
                String version = it.parameterValues.getValue('version')
                npmPackages.put(packageName, version)
            }
        }
        npmPackages
    }

    /**
     * Find all JsModules in project using route strategy
     *
     * @param scan
     *      the classpath scan with the annotations
     * @return
     *      the values of the js modules
     */
    static final Map<String, String> findJsModulesByRoute(ScanResult scan) {
        Map<String, String> modules = [:]
        List<String> processedClasses = []
        scan.getClassesWithAnnotation(ROUTE_FQN).each { ClassInfo ci ->
            findImportModulesByDependencies(ci, modules, JS_MODULE_FQN, processedClasses)
        }
        LOGGER.info("Scanned ${processedClasses.size()} classes.")
        modules
    }

    /**
     * Gets all JsModules from the whitelisted scan
     *
     * The whitelist is set in #VaadinFlowPluginExtension.whitelistedPackages and is performed
     * when the scan is created.
     *
     * @param scan
     *      the classpath scan with the annotations
     * @return
     *      the values of the js modules
     */
    static final Map<String,String> findJsModulesByWhitelist(ScanResult scan) {
        Map<String, String> modules = [:]
        scan.getClassesWithAnnotation(JS_MODULE_FQN).each { ClassInfo ci ->
            ci.getAnnotationInfoRepeatable(JS_MODULE_FQN).each { AnnotationInfo a ->
                modules[a.parameterValues.value.value.toString()] = ci.name
            }
        }
        modules
    }

    /**
     * Find all CSSImports in project using route strategy
     *
     * @since 1.3
     * @param scan
     *      the classpath scan with the annotations
     * @return
     *      the values of the css imports
     */
    static final Map<String,Map<String,String>> findCssImportsByRoute(ScanResult scan) {
        Map<String, Map<String,String>> imports = [:]
        List<String> processedClasses = []
        scan.getClassesWithAnnotation(ROUTE_FQN).each { ClassInfo ci ->
            findImportModulesByDependencies(ci, imports, CSS_IMPORT_FQN, processedClasses) { info, a ->
                [
                    'className' : info.name,
                    'include' : a.parameterValues.include?.value,
                    'id' : a.parameterValues.id?.value,
                    'target' : a.parameterValues.themeFor?.value
                ]
            }
        }
        LOGGER.info("Scanned ${processedClasses.size()} classes.")
        imports
    }

    /**
     * Find all CSSImports in project using whitelist
     *
     * @since 1.3
     * @param scan
     *      the classpath scan with the annotations
     * @return
     *      the values of the css imports
     */
    static final Map<String, Map<String,String>> findCssImportsByWhitelist(ScanResult scan) {
        Map<String, Map<String,String>> imports = [:]
        scan.getClassesWithAnnotation(CSS_IMPORT_FQN).collect { ClassInfo ci ->
            ci.getAnnotationInfoRepeatable(CSS_IMPORT_FQN).each { AnnotationInfo a ->
                imports[a.parameterValues.value.value.toString()] = [
                    'className' : ci.name,
                    'include' : a.parameterValues.include?.value,
                    'id' : a.parameterValues.id?.value,
                    'target' : a.parameterValues.themeFor?.value
                ]
            }
        }
        imports
    }

    static final Map<String, Map<String,String>> findCssImports(VaadinFlowPluginExtension vaadin, ScanResult scan) {
        switch (vaadin.scanStrategy) {
            case 'route':
                return findCssImportsByRoute(scan)
            case 'whitelist':
                return findCssImportsByWhitelist(scan)
            default:
                throw new GradleException("Scan strategy $vaadin.scanStrategy has not been implemented.")
        }
    }

    /**
     * Find all classes which has an @Id filed annotation
     *
     * @param scan
     *  the classpath scan with the annoations
     */
    static final List<String> findIdUsages(ScanResult scan) {
        scan.getClassesWithFieldAnnotation(ID_FQN)*.name
    }

    /**
     * Finds import modules by traversing the dependency tree of the root info
     *
     * @since 1.3
     * @param info
     *      the root class to start the search from
     * @param imports
     *      the accumulated imports
     * @param annotation
     *      the annotation to search for
     * @param processedClasses
     *      the accumulated processedClasses
     * @param valueGenerator
     *      the generator that generates the returned map values
     */
    static final <T> void findImportModulesByDependencies(ClassInfo info, Map<String, T> imports, String annotation,
                                                          List<String> processedClasses,
                                                          Closure<T> valueGenerator = { i, a -> i.name }) {
        processedClasses << info.name
        if (info.hasAnnotation(annotation)) {
            info.getAnnotationInfoRepeatable(annotation).each { AnnotationInfo a ->
                imports[a.parameterValues.value.value.toString()] = valueGenerator.call(info, a)
            }
        }
        info.classDependencies.each { ClassInfo childInfo ->
            if (!processedClasses.contains(childInfo.name)) {
                findImportModulesByDependencies(childInfo, imports, annotation, processedClasses, valueGenerator)
            }
        }
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
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        Collection<String> whitelist = vaadin.whitelistedPackages
        LOGGER.info('Scanning with whitelist:')
        whitelist.each { LOGGER.info("\t${it}") }

        new ClassGraph()
                .overrideClassLoaders(getClassLoader(project))
                .enableAnnotationInfo()
                .enableInterClassDependencies()
                .whitelistPackages(whitelist as String[])
                .scan()
    }

    /**
     * Finds CSS Files in the classpath of the project
     *
     * @param project
     *      the project to scan
     * @param paths
     *      the paths to whitelist
     * @return
     *      the css files found in the project
     */
    static final Map<String, String> findCssInResources(Project project, Collection<String> paths) {
        Map<String,String> cssFiles = [:]
        new ClassGraph()
            .overrideClassLoaders(getClassLoader(project))
            .scan()
            .withCloseable {
                it.getResourcesWithExtension('css').each { Resource resource ->
                    if (paths.find { resource.pathRelativeToClasspathElement.endsWith(it) }) {
                        cssFiles[resource.pathRelativeToClasspathElement] = resource.contentAsString
                    }
                }
            }
        cssFiles
    }

    private static Map<String, Map<String,String>> findThemes(Project project) {
        Map<String, Map<String,String>> themes = [:]
        URLClassLoader cl = getClassLoader(project)
        new ClassGraph()
            .overrideClassLoaders(cl)
            .enableClassInfo()
            .scan()
            .withCloseable {
                it.getClassesImplementing(ABSTRACT_THEME_FQN).each {
                    Object instance = cl.loadClass(it.name).newInstance()
                    themes[it.simpleName.toLowerCase()] = [
                        'baseUrl' :  instance.baseUrl.toString(),
                        'themeUrl' : instance.themeUrl.toString()
                    ]
                }
            }
        themes
    }

    private static URLClassLoader getClassLoader(Project project) {
        URL[] classpath = []
        classpath += project.sourceSets.main.runtimeClasspath.collect { it.toURI().toURL() } as URL[]
        classpath += project.sourceSets.main.compileClasspath.collect { it.toURI().toURL() } as URL[]
        new URLClassLoader(classpath)
    }

    private static void findHTMLImportsRecursively(Project project, File template, Map<String, File> imports,
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

    private static void addImport(Project project, File importFile , Map<String, File> imports, String htmlImport,
                                  Map<String,String> theme) {
        File frontendDir = new File(project.buildDir, FRONTEND_DIR)
        if (!imports.containsKey(htmlImport)) {
            if (theme && htmlImport.contains(theme.baseUrl) ) {
                String htmlThemeImport = htmlImport.replace(theme.baseUrl, theme.themeUrl)
                File themeFile = new File(frontendDir, htmlThemeImport)
                if (themeFile.exists()) {
                    imports.put(htmlThemeImport, themeFile)
                } else {
                    imports.put(htmlImport, importFile)
                }
            } else {
                imports.put(htmlImport, importFile)
            }

            findHTMLImportsRecursively(project, importFile, imports, theme)
        }
    }

}
