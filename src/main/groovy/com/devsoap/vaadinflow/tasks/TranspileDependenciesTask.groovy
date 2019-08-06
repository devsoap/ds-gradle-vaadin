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
package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.models.PolymerBuild
import com.devsoap.vaadinflow.util.ClassIntrospectionUtils
import com.devsoap.vaadinflow.util.LogUtils
import com.devsoap.vaadinflow.util.VaadinYarnRunner
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import io.github.classgraph.ScanResult
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.hash.HashUtil
import org.gradle.util.GFileUtils

import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger
import java.util.regex.Matcher

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
    private static final String BOWER_COMPONENTS = 'bower_components'
    private static final String NODE_MODULES = 'node_modules'
    private static final String BUILD = 'build'
    private static final String FRONTEND = 'frontend'
    private static final String TEMPLATES_GLOB = '**/templates/**'
    private static final String STYLES_GLOB = '**/styles/**'
    private static final String JAVASCRIPT_FILE_TYPE = '.js'
    private static final String STYLES = 'styles'
    private static final String TEMPLATES = 'templates'
    private static final String MISSING_PROPERTY = '_missing'
    private static final String IMPORTS_JS_FILE = 'index.js'
    private static final String SLASH = '/'
    private static final String VAADIN = 'VAADIN'

    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)
    final VaadinYarnRunner yarnRunner = new VaadinYarnRunner(project, workingDir)

    final File webappGenDir = new File(project.buildDir, 'webapp-gen')
    final File webappGenFrontendDir = new File(webappGenDir, FRONTEND)

    private final ListProperty<String> bundleExcludes = project.objects.listProperty(String)

    private final ListProperty<String> importExcludes = project.objects.listProperty(String)

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
        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
        Paths.get(assembleTask.webappDir.canonicalPath, FRONTEND, TEMPLATES).toFile().with { it.exists() ? it : null }
    }

    @Optional
    @InputDirectory
    final Closure<File> webStylesDir = {
        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
        Paths.get(assembleTask.webappDir.canonicalPath, FRONTEND, STYLES).toFile().with { it.exists() ? it : null }
    }

    @InputDirectory
    final File nodeModules = new File(workingDir, NODE_MODULES)

    @InputDirectory
    final File bowerComponents = new File(workingDir, BOWER_COMPONENTS)

    @InputDirectory
    final File unpackedStaticResources = new File(workingDir, 'static')

    // Temporary directory to store Javascript sources. Removed after build.
    final File javascriptSources = new File(workingDir, 'src')
    final File importsJs = new File(javascriptSources, IMPORTS_JS_FILE)

    @InputFile
    final File packageJson = new File(workingDir, PACKAGE_JSON_FILE)

    @OutputFile
    final File polymerJson = new File(workingDir, 'polymer.json')

    @OutputFile
    final File statsJson = Paths.get(project.buildDir.canonicalPath, 'resources',
            'main', 'META-INF', VAADIN, 'config', 'stats.json').toFile()

    @OutputFile
    final File mainJs = Paths.get(webappGenDir.canonicalPath, VAADIN, 'main.js').toFile()

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

    TranspileDependenciesTask() {
        dependsOn(
                'classes',
                InstallBowerDependenciesTask.NAME,
                InstallYarnDependenciesTask.NAME,
                ConvertCssToHtmlStyleTask.NAME,
                ConvertGroovyTemplatesToHTML.NAME
        )
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
        LOGGER.info('Copying unpacked static resources...')
        project.copy { spec -> spec.from(unpackedStaticResources).into(workingDir) }
        project.copy { spec -> spec.from(unpackedStaticResources).include('**/*.js').into(javascriptSources) }

        LOGGER.info( 'Copying generated styles...')
        project.copy { spec -> spec.from(webappGenFrontendDir).include(STYLES_GLOB).into(workingDir) }

        LOGGER.info( 'Copying generated templates...')
        project.copy { spec -> spec.from(webappGenFrontendDir).include(TEMPLATES_GLOB).into(workingDir) }

        File templatesDir = webTemplatesDir.call()
        if (templatesDir) {
            LOGGER.info( 'Copying html templates...')
            project.copy { spec ->
                spec.from(templatesDir.parentFile).include(TEMPLATES_GLOB).into(workingDir)
            }

            LOGGER.info('Validating html templates...')
            File templatesTargetDir = new File(workingDir, TEMPLATES)
            validateImports(templatesTargetDir)
        }

        File stylesDir = webStylesDir.call()
        if (stylesDir) {
            LOGGER.info( 'Copying html styles...')
            project.copy { spec ->
                spec.from(stylesDir.parentFile).include(STYLES_GLOB).into(workingDir)
            }

            LOGGER.info('Validating html styles...')
            File templatesTargetDir = new File(workingDir, STYLES)
            validateImports(templatesTargetDir)
        }

        LOGGER.info('Performing annotation scan...')
        ScanResult scan = LogUtils.measureTime('Scan completed') {
            ClassIntrospectionUtils.getAnnotationScan(project)
        }

        LOGGER.info('Searching for HTML imports...')
        List<String> imports = initHTMLImportsFromComponents(scan)

        LOGGER.info('Searching for JS modules...')
        Map<String, String> modules = ClassIntrospectionUtils.findJsModules(scan)

        LOGGER.info( 'Validating JS module imports...')
        removeInvalidModules(modules)

        LOGGER.info("Creating ${importsJs.name}...")
        importsJs.text = ''
        modules.each { m, c -> importsJs << "import '$m';\n" }

        LOGGER.info( 'Removing excluded HTML imports..')
        getImportExcludes().each { filter -> imports.removeIf { it.matches(filter) } }

        File htmlFile = html.call()
        LOGGER.info("Creating ${htmlFile.name}...")
        initHtml(htmlFile, imports)

        LOGGER.info("Creating ${polymerJson.name}...")
        initPolymerJson(htmlFile, imports)

        LOGGER.info("Creating ${manifestJson.name}...")
        VaadinYarnRunner yarnBundleRunner = new VaadinYarnRunner(project, workingDir, new ByteArrayOutputStream())
        yarnBundleRunner.polymerBundle(manifestJson, htmlFile, getBundleExcludes())

        LOGGER.info('Checking for missing dependencies...')
        checkForMissingDependencies()

        LOGGER.info('Transpiling...')
        LogUtils.measureTime('Transpiling done successfully') {
            yarnRunner.transpile(getBundleExcludes())
        }

        LOGGER.info('Validating transpilation...')
        validateTranspilation()

        LOGGER.info('Bundling...')
        LogUtils.measureTime('Bundling completed') {
            yarnRunner.webpackBundle(statsJson, mainJs)
        }

        LOGGER.info( 'Cleaning up unpacked static resources...')
        project.fileTree(unpackedStaticResources).each {
            String relativePath = it.path - workingDir.path - "static${File.separator}"
            String absolutePath = workingDir.canonicalPath + relativePath
            File file = new File(absolutePath)
            GFileUtils.deleteFileQuietly(file)
            while (file.parentFile.listFiles().length == 0) {
                file = file.parentFile
                GFileUtils.deleteDirectory(file)
            }
        }
        GFileUtils.deleteDirectory(javascriptSources)
    }

    /**
     * Sets the bundle exclusions
     */
    void setBundleExcludes(List<String> excludes) {
        bundleExcludes.set(excludes)
    }

    /**
     * Get bundle exclusions
     */
    List<String> getBundleExcludes() {
        bundleExcludes.getOrElse([])
    }

    /**
     * Get import exclusions
     */
    List<String> getImportExcludes() {
        importExcludes.getOrElse([])
    }

    /**
     * Get import exclusions
     */
    void setImportExcludes(List<String> excludes) {
        importExcludes.set(excludes)
    }

    private List<String> initHTMLImportsFromComponents(ScanResult scan) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        List<String> imports = []
        imports += ClassIntrospectionUtils.findHtmlImports(project, scan, vaadin.baseTheme)
        imports += ClassIntrospectionUtils.findJsImports(scan)
        imports += ClassIntrospectionUtils.findStylesheetImports(scan)
        imports.sort()
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
        workingDir.listFiles({ File dir, String name -> name.endsWith '.cache.html' } as FilenameFilter).each {
            if (html.canonicalPath != it.canonicalPath) {
                it.delete()
            }
        }

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

    private void validateImports(File templatesTargetDir) {
        Logger logger = LOGGER
        project.fileTree(templatesTargetDir).each { File template ->
            template.text.findAll('.*rel="import".*href="(.*)".*')
            .collect {
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
                if (!importFile.exists()) {
                    logger.severe("${workingDir.relativePath(template)}: ${project.relativePath(importFile)}" +
                            ' not found on filesystem!')
                    throw new GradleException(
                            "Imported file '$importFile' in " +
                                    "${workingDir.relativePath(template)} does not exist!")
                }
            }
        }
    }

    private void checkForMissingDependencies() {
        Map<String, Object> manifest = new JsonSlurper().parse(manifestJson) as Map
        if (manifest[MISSING_PROPERTY]) {
            List<String> missing = manifest[MISSING_PROPERTY] as List
            missing.removeAll(getBundleExcludes())
            if (!missing.empty) {
                missing.each {
                    LOGGER.severe("HTML import '${it}' could not be resolved.")
                }
                LOGGER.severe('Please check that you have included the necessary dependencies or ' +
                        'alternatively exclude them with:\n')
                LOGGER.severe('vaadinTranspileDependencies.bundleExcludes = [')
                missing.each {
                    if (missing.last() == it) {
                        LOGGER.severe("\t'$it'")
                    } else {
                        LOGGER.severe("\t'$it',")
                    }
                }
                LOGGER.severe(']')
                throw new GradleException('Manifest contains imports to missing dependencies. ' +
                        'Run with --info to get more information.')
            }
            manifest.remove(MISSING_PROPERTY)
            manifestJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(manifest))
        }
    }

    private void removeInvalidModules(Map<String,String> modules) {
        modules.removeAll { m, c ->
            File nodeDependency = Paths.get(nodeModules.canonicalPath, m.split(SLASH)).toFile()
            File staticFile = Paths.get(javascriptSources.canonicalPath, m.split(SLASH)).toFile()
            boolean exists = nodeDependency.exists() || staticFile.exists()
            if (!exists) {
                LOGGER.warning("No Javascript module with the name '$m' could be found. Module ignored.")
                return true
            }
            if (!nodeDependency.exists() && staticFile.exists() && !m.startsWith('./')) {
                LOGGER.warning("Static file Javascript module '$m' does not start with './'. Module ignored.")
                return true
            }
            false
        }
    }

    private void validateTranspilation() {
        if (!es5dir.exists()) {
            throw new GradleException(
                    "Transpile did not generate ES5 result in $es5dir. Run with --info to get more information.")
        }
        if (!es6dir.exists()) {
            throw new GradleException(
                    "Transpile did not generate ES6 result in $es6dir. Run with --info to get more information.")
        }
    }
}
