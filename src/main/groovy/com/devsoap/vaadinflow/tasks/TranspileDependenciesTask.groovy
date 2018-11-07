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
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Incubating
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.hash.HashUtil

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

    private static final String PACKAGE_JSON_FILE = 'package.json'
    private static final String SLASH = PATH_SEPARATOR
    private static final String BOWER_COMPONENTS = 'bower_components'
    private static final String NODE_MODULES = 'node_modules'
    private static final String BUILD = 'build'
    private static final String FRONTEND = 'frontend'
    private static final String TEMPLATES_GLOB = '**/templates/**'
    private static final String BOWER_JSON = 'bower.json'
    private static final String HTML_FILE_TYPE = '.html'
    private static final String JAVASCRIPT_FILE_TYPE = '.js'
    private static final String STYLES = 'styles'
    private static final String TEMPLATES = 'templates'
    private static final String PATH_SEPARATOR = '/'

    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)
    final VaadinYarnRunner yarnRunner = new VaadinYarnRunner(project, workingDir)

    final File webappGenDir = new File(project.buildDir, 'webapp-gen')
    final File webappGenFrontendDir = new File(webappGenDir, FRONTEND)

    private final Property<List<String>> bundleExcludes = project.objects.property(String)

    @Optional
    @InputDirectory
    final Closure<File> webappGenFrontendStylesDir = {
        new File(webappGenFrontendDir, STYLES).with { it.exists() ? it : null }
    }

    @Optional
    @InputDirectory
    final Closure<File> webappGenFrontendTemplatesDir = {
        new File(webappGenFrontendDir, TEMPLATES).with { it.exists() ? it : null }
    }

    @Optional
    @InputDirectory
    final Closure<File> webTemplatesDir = {
       Paths.get(project.projectDir.canonicalPath,
               'src', 'main', 'webapp', FRONTEND, TEMPLATES)
               .toFile().with { it.exists() ? it : null }
    }

    @InputDirectory
    final File nodeModules = new File(workingDir, NODE_MODULES)

    @InputDirectory
    final File bowerComponents = new File(workingDir, BOWER_COMPONENTS)

    @InputDirectory
    final File unpackedStaticResources = new File(workingDir, 'static')

    @InputFile
    final File packageJson = new File(workingDir, PACKAGE_JSON_FILE)

    @OutputFile
    final File polymerJson = new File(workingDir, 'polymer.json')

    @OutputFile
    final Closure<File> html = {
        String fileHash = inputs.files.collect { it.exists() ? HashUtil.sha256(it) : '' }.join('')
        String propertyHash = inputs.properties.collect { key, value -> HashUtil.createCompactMD5(key + value) }
        String manifestHash = HashUtil.createCompactMD5(fileHash + propertyHash)
        new File(workingDir, "vaadin-flow-bundle-${manifestHash}.cache.html")
    }

    @OutputDirectory
    final File es5dir = Paths.get(workingDir.canonicalPath, BUILD, 'frontend-es5').toFile()

    @OutputDirectory
    final File es6dir = Paths.get(workingDir.canonicalPath, BUILD, 'frontend-es6').toFile()

    @OutputFile
    final File manifestJson = new File(workingDir, 'vaadin-flow-bundle-manifest.json')

    @OutputDirectory
    final File stylesDir = new File(workingDir, STYLES)

    @OutputDirectory
    final File templatesDir = new File(workingDir, TEMPLATES)

    @OutputFiles
    final Closure<Map<String, File>> staticResources = {
        Map<String, File> map = [:]
        project.fileTree(unpackedStaticResources).each {
            map[it.name] = new File(workingDir, it.name)
        }
        map
    }

    TranspileDependenciesTask() {
        dependsOn(InstallBowerDependenciesTask.NAME, InstallYarnDependenciesTask.NAME, ConvertCssToHtmlStyleTask.NAME,
                ConvertGroovyTemplatesToHTML.NAME)
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
        LOGGER.info('Copying unpacked static resources')
        project.copy { spec -> spec.from(unpackedStaticResources).into(workingDir) }

        LOGGER.info( 'Copying generated styles....')
        project.copy { spec -> spec.from(webappGenFrontendDir).include('**/styles/**').into(workingDir) }

        LOGGER.info( 'Copying generated templates....')
        project.copy { spec -> spec.from(webappGenFrontendDir).include(TEMPLATES_GLOB).into(workingDir) }

        File templatesDir = webTemplatesDir.call()
        if (templatesDir) {
            LOGGER.info( 'Copying html templates ...')
            project.copy { spec ->
                spec.from(templatesDir.parentFile).include(TEMPLATES_GLOB).into(workingDir)
            }
        }

        LOGGER.info('Searching for HTML imports...')
        List<String> imports = initHTMLImportsFromComponents()
        imports += initGeneratedHTMLImports()
        imports += initResourceImports()

        // Replace Windows path back-slashes with forward slashes
        imports*.replace('\\', PATH_SEPARATOR)

        File htmlFile = html.call()
        LOGGER.info("Creating ${htmlFile.name}...")
        initHtml(htmlFile, imports)

        LOGGER.info("Creating ${polymerJson.name}...")
        initModuleSources(imports)
        initPolymerJson(htmlFile, imports)

        LOGGER.info("Creating ${manifestJson.name}...")
        VaadinYarnRunner yarnBundleRunner = new VaadinYarnRunner(project, workingDir, new ByteArrayOutputStream())
        yarnBundleRunner.polymerBundle(manifestJson, htmlFile, getBundleExcludes())

        LOGGER.info('Transpiling...')
        yarnRunner.transpile()

        LOGGER.info('Validating transpilation...')
        if (!es5dir.exists()) {
            throw new GradleException(
                    "Transpile did not generate ES5 result in $es5dir. Run with --info to get more information.")
        }
        if (!es6dir.exists()) {
            throw new GradleException(
                    "Transpile did not generate ES6 result in $es6dir. Run with --info to get more information.")
        }
        LOGGER.info('Transpiling done successfully.')
    }

    /**
     * Sets the bundle exclusions
     */
    @Incubating
    void setBundleExcludes(List<String> excludes) {
        bundleExcludes.set(excludes)
    }

    /**
     * Get bundle exclusions
     */
    @Incubating
    List<String> getBundleExcludes() {
        bundleExcludes.getOrElse([])
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

    private List<String> initHTMLImportsFromComponents() {
        List<String> imports = []

        List<File> scanDirs = []
        if (nodeModules.exists()) {
            scanDirs.add(nodeModules)
        }
        if (bowerComponents.exists()) {
            scanDirs.add(bowerComponents)
        }

        scanDirs.each {

            LOGGER.info("Searching for html imports in $it")
            it.eachDir { dir ->
                File bowerJsonFile = new File(dir, '.bower.json')
                if (!bowerJsonFile.exists()) {
                    bowerJsonFile = new File(dir, TranspileDependenciesTask.BOWER_JSON)
                }
                if (bowerJsonFile.exists()) {
                    Object bowerJson = new JsonSlurper().parse(bowerJsonFile)
                    List<String> entrypoints = []
                    if (bowerJson.main instanceof List) {
                        entrypoints.addAll(bowerJson.main as List)
                    } else {
                        entrypoints.add(bowerJson.main as String)
                    }
                    entrypoints.findAll { it?.endsWith(TranspileDependenciesTask.HTML_FILE_TYPE) }.each {
                        File resourceFile = new File(dir, it)
                        String path = (resourceFile.path - workingDir.path).substring(1)
                        imports.add(path)
                    }
                }
            }
        }
        imports
    }

    private List<String> initGeneratedHTMLImports() {

        List<File> scanDirs = []

        if (webappGenFrontendStylesDir.call()) {
            scanDirs.add(webappGenFrontendStylesDir.call())
        }

        if (webappGenFrontendTemplatesDir.call()) {
            scanDirs.add(webappGenFrontendTemplatesDir.call())
        }

        if (webTemplatesDir.call()) {
            scanDirs.add(webTemplatesDir.call())
        }

        List<String> imports = []
        scanDirs.each { File dir ->
            LOGGER.info("Searching for html imports in $dir")
            dir.eachFile { File fileOrDir ->
                project.fileTree(fileOrDir)
                        .include('**/*.html')
                        .each { File htmlFile ->
                    String path = (htmlFile.path - dir.parentFile.path).substring(1)
                    imports.add(path)
                }
            }
        }
        imports
    }

    private List<String> initResourceImports() {
        List<String> imports = []

        LOGGER.info("Searching for resource HTML/JS imports in $workingDir")
        workingDir.listFiles ({ File file, String name ->
            name.endsWith(HTML_FILE_TYPE) || name.endsWith(JAVASCRIPT_FILE_TYPE)
        } as FilenameFilter).each {
            if (!it.name.startsWith('vaadin-flow-bundle')) {
                String path = (it.path - workingDir.path).substring(1)
                imports.add(path)
            }
        }

        imports
    }

    private void initPolymerJson(File html, List<String> sources) {

        List<String> extraDependencies = [manifestJson.name]

        if (bowerComponents.exists()) {
            File webcomponentsjs = new File(bowerComponents, 'webcomponentsjs')
            webcomponentsjs.eachFile {
                if (it.name.endsWith(JAVASCRIPT_FILE_TYPE) || it.name.endsWith('.js.map')) {
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

    private void initHtml(File html, List<String> imports) {
        if (!html.exists()) {
            html.createNewFile()
        }

        html.withPrintWriter { writer ->
            imports.each { String path ->
                if (path.endsWith(TranspileDependenciesTask.JAVASCRIPT_FILE_TYPE)) {
                    writer.write("<script src='$path'></script>\n")
                } else {
                    writer.write("<link rel='import' href='$path' >\n")
                }
            }
        }
    }
}
