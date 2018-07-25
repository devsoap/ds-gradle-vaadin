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
package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.models.PolymerBuild
import com.devsoap.vaadinflow.util.VaadinYarnRunner
import groovy.json.JsonOutput
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Transpiles web components to ES5 and ES6 production artifacts
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
@CacheableTask
class TranspileDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinTranspileDependencies'

    private static final String BOWER_JSON_FILE = 'bower.json'
    private static final String PACKAGE_JSON_FILE = 'package.json'
    private static final String SLASH = '/'
    private static final String BOWER_COMPONENTS = 'bower_components'
    private static final String NODE_MODULES = 'node_modules'
    private static final String BUILD = 'build'
    private static final String FRONTEND = 'frontend'
    private static final String TEMPLATES_GLOB = '**/templates/**'

    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)
    final VaadinYarnRunner yarnRunner = new VaadinYarnRunner(project, workingDir)

    final File webappGenDir = new File(project.buildDir, 'webapp-gen')
    final File webappGenFrontendDir = new File(webappGenDir, FRONTEND)

    @InputDirectory
    final File webappGenFrontendStylesDir = new File(webappGenFrontendDir, 'styles')

    @Optional
    @InputDirectory
    final Closure<File> webTemplatesDir = {
       Paths.get(project.rootDir.canonicalPath,
               'src', 'main', 'webapp', FRONTEND, 'templates')
               .toFile().with { it.exists() ? it : null }
    }

    @InputDirectory
    final File nodeModules = new File(workingDir, NODE_MODULES)

    @InputDirectory
    final File bowerComponents = new File(workingDir, BOWER_COMPONENTS)

    @InputFile
    final File packageJson = new File(workingDir, PACKAGE_JSON_FILE)

    @OutputFile
    final File polymerJson = new File(workingDir, 'polymer.json')

    @OutputFile
    final File html = new File(workingDir, 'vaadin-flow-bundle.cache.html')

    @OutputDirectory
    final File es5dir = Paths.get(workingDir.canonicalPath, BUILD, 'frontend-es5').toFile()

    @OutputDirectory
    final File es6dir = Paths.get(workingDir.canonicalPath, BUILD, 'frontend-es6').toFile()

    @OutputFile
    final File manifestJson = new File(workingDir, 'vaadin-flow-bundle-manifest.json')

    TranspileDependenciesTask() {
        dependsOn(InstallBowerDependenciesTask.NAME, InstallYarnDependenciesTask.NAME, ConvertCssToHtmlStyleTask.NAME)
        onlyIf {
            project.extensions.getByType(VaadinClientDependenciesExtension).compileFromSources
        }

        description = 'Compiles client modules to support legacy browsers'
        group = 'Vaadin'

        inputs.property('compileFromSources') {
            project.extensions.getByType(VaadinClientDependenciesExtension).compileFromSources
        }

        inputs.property('yarnDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).yarnDependencies
        }

        inputs.property('bowerDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies
        }

    }

    @TaskAction
    void run() {

        LOGGER.info( 'Copying generated styles....')
        project.copy { spec -> spec.from(webappGenFrontendDir).include('**/styles/**').into(workingDir) }

        File templatesDir = webTemplatesDir.call()
        if (templatesDir) {
            LOGGER.info( 'Copying html templates styles....')
            project.copy { spec ->
                spec.from(templatesDir.parentFile).include(TEMPLATES_GLOB).into(workingDir)
            }
        }

        LOGGER.info('Searching for HTML imports')
        List<String> imports = initHTMLImports()

        LOGGER.info("Creating ${html.name}...")
        initHtml(imports)

        LOGGER.info("Creating ${polymerJson.name}...")
        List<String> sources = initModuleSources(imports)
        initPolymerJson(sources)

        LOGGER.info("Creating ${manifestJson.name}...")
        VaadinYarnRunner yarnBundleRunner = new VaadinYarnRunner(project, workingDir, new ByteArrayOutputStream())
        yarnBundleRunner.polymerBundle(manifestJson, html)

        LOGGER.info('Transpiling...')
        yarnRunner.transpile()
    }

    private static List<String> initModuleSources(List<String> imports) {
        Set<String> sources = imports.collect { htmlPath ->
            String path = htmlPath.split(SLASH).dropRight(1).join(SLASH)
            if (path.startsWith(NODE_MODULES) || path.startsWith(BOWER_COMPONENTS)) {
                path += '/src/**/*'
            } else {
                path += '/**/*'
            }
            path
        }
        Collections.unmodifiableList(new ArrayList(sources))
    }

    private List<String> initHTMLImports() {
        List<String> imports = []
        List<File> scanDirs = []
        if (nodeModules.exists()) {
            scanDirs.add(nodeModules)
        }
        if (bowerComponents.exists()) {
            scanDirs.add(bowerComponents)
        }

        String htmlIncludeGlob = '**/*.html'
        scanDirs.each {
            LOGGER.info("Searching for html imports in $it")
            it.eachDir { dir ->
                project.fileTree(dir)
                        .include(htmlIncludeGlob)
                        .exclude(
                        '**/index.html',
                        '**/demo/**',
                        '**/test*/**',
                        '**/src/**',
                        '**/lib/**',
                        TranspileDependenciesTask.TEMPLATES_GLOB,
                        '**/polymer-cli/**')
                        .each { File htmlFile ->

                    if (new File(dir, TranspileDependenciesTask.BOWER_JSON_FILE).exists()) {
                        String path = (htmlFile.path - workingDir.path).substring(1)
                        imports.add(path)
                    }
                }
            }
        }

        scanDirs = [webappGenFrontendDir]
        if (webTemplatesDir.call()) {
            scanDirs.add(webTemplatesDir.call())
        }
        scanDirs.each { File dir ->
            LOGGER.info("Searching for html imports in $dir")
            dir.eachFile { File fileOrDir ->
                if (fileOrDir.directory) {
                    project.fileTree(fileOrDir)
                            .include(htmlIncludeGlob)
                            .each { File htmlFile ->
                        String path = (htmlFile.path - dir.path).substring(1)
                        imports.add(path)
                    }
                } else if (fileOrDir.name.endsWith('.html')) {
                    String path = (fileOrDir.path - dir.parentFile.path).substring(1)
                    imports.add(path)
                }
            }
        }

        imports.each { LOGGER.info("Found import $it") }

        Collections.unmodifiableList(imports)
    }

    private void initPolymerJson(List<String> sources) {

        List<String> extraDependencies = [manifestJson.name]

        if (bowerComponents.exists()) {
            File webcomponentsjs = new File(bowerComponents, 'webcomponentsjs')
            webcomponentsjs.eachFile {
                if (it.name.endsWith('.js') || it.name.endsWith('.js.map')) {
                    extraDependencies.add("$BOWER_COMPONENTS/webcomponentsjs/$it.name")
                }
            }
        } else {
            logger.warn('Skipping loading webcomponentsjs as it was not found in bower_components')
        }

        PolymerBuild buildModel = new PolymerBuild(
                entrypoint: html.name,
                sources: sources,
                extraDependencies : extraDependencies,
                builds: [
                    new PolymerBuild.CustomBuild(name: es5dir.name).with { js.compile = true; it },
                    new PolymerBuild.CustomBuild(name: es6dir.name)
                ]
        )
        polymerJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(buildModel))
    }

    private void initHtml(List<String> imports) {
        if (!html.exists()) {
            html.createNewFile()
        }

        html.withPrintWriter { writer ->
            imports.each { writer.write("<link rel='import' href='$it' >\n") }
        }
    }
}
