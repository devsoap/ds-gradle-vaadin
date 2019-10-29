/*
 * Copyright 2018-2019 Devsoap Inc.
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
package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.actions.JavaPluginAction
import com.devsoap.vaadinflow.actions.SpringBootAction
import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.models.PolymerBuild
import com.devsoap.vaadinflow.util.ClassIntrospectionUtils
import com.devsoap.vaadinflow.util.ClientPackageUtils
import com.devsoap.vaadinflow.util.LogUtils
import com.devsoap.vaadinflow.util.TemplateWriter
import com.devsoap.vaadinflow.util.VaadinYarnRunner
import groovy.json.JsonSlurper
import groovy.transform.PackageScope
import groovy.util.logging.Log
import io.github.classgraph.ScanResult
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.CopySpec
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
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

    // Need to be public for closure to have access to them
    static final String DOTSLASH = './'
    static final String JAVASCRIPT_FILE_TYPE = '.js'

    private static final String PACKAGE_JSON_FILE = 'package.json'
    private static final String BOWER_COMPONENTS = 'bower_components'
    private static final String NODE_MODULES = 'node_modules'
    private static final String BUILD = 'build'
    private static final String FRONTEND = 'frontend'
    private static final String TEMPLATES_GLOB = '**/templates/**'
    private static final String STYLES_GLOB = '**/styles/**'

    private static final String STYLES = 'styles'
    private static final String TEMPLATES = 'templates'
    private static final String MISSING_PROPERTY = '_missing'
    private static final String IMPORTS_JS_FILE = 'index.js'
    private static final String SLASH = '/'

    private static final String VAADIN = 'VAADIN'
    private static final String WARN_BUNDLE_EXCLUDES_ONLY_AVAILABLE_IN_COMP_MODE =
            'bundleExcludes only supported in compatibility mode.'
    private static final String RUN_WITH_INFO_FOR_MORE_INFORMATION = 'Run with --info to get more information.'
    private static final String VAADIN_FLOW_PACKAGE = 'com.vaadin.flow'

    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)
    final VaadinYarnRunner yarnRunner = new VaadinYarnRunner(project, workingDir)
    final File srcDir = new File(workingDir, 'src')
    final File webappGenDir = new File(project.buildDir, 'webapp-gen')
    final File webappGenFrontendDir = new File(webappGenDir, FRONTEND)
    final Closure<File> configDir = {
        File rootDir = SpringBootAction.isActive(project) ? webappGenDir :
                Paths.get(project.buildDir.canonicalPath, 'resources', 'main').toFile()
        Paths.get(rootDir.canonicalPath, 'META-INF', VAADIN, 'config').toFile()
    }
    private final ListProperty<String> bundleExcludes = project.objects.listProperty(String)

    private final ListProperty<String> importExcludes = project.objects.listProperty(String)

    @Internal
    @Option(option = 'ignore-id-usage', description = 'Should @Id be checked (#285)')
    boolean ignoreIdUsage = false

    @Deprecated
    @Optional
    @InputDirectory
    final Closure<File> webappGenFrontendStylesDir = {
        new File(webappGenFrontendDir, STYLES).with { it.exists() ? it : null }
    }

    @Deprecated
    @Optional
    @InputDirectory
    final Closure<File> webappGenFrontendTemplatesDir = {
        new File(webappGenFrontendDir, TEMPLATES).with { it.exists() ? it : null }
    }

    @Deprecated
    @Optional
    @InputDirectory
    final Closure<File> webTemplatesDir = {
        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
        Paths.get(assembleTask.webappDir.canonicalPath, FRONTEND, TEMPLATES).toFile().with { it.exists() ? it : null }
    }

    @Deprecated
    @Optional
    @InputDirectory
    final Closure<File> webStylesDir = {
        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
        Paths.get(assembleTask.webappDir.canonicalPath, FRONTEND, STYLES).toFile().with { it.exists() ? it : null }
    }

    @InputDirectory
    final File nodeModules = new File(workingDir, NODE_MODULES)

    @Optional
    @OutputDirectory
    final Closure<File> appNodeModules = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        vaadin.compatibilityMode ?  null : Paths.get(workingDir.canonicalPath, 'dist', NODE_MODULES).toFile()
    }

    @Deprecated
    @Optional
    @InputDirectory
    final Closure<File> bowerComponents = {
        new File(workingDir, BOWER_COMPONENTS).with { it.exists() ? it : null }
    }

    @Optional
    @InputDirectory
    final Closure<File> unpackedStaticResources = {
        new File(workingDir, 'static').with { it.exists() ? it : null }
    }

    @Optional
    @InputDirectory
    final Closure<File> stylesheetsSources = {
        Paths.get(project.projectDir.canonicalPath,
                JavaPluginAction.STYLESHEETS_SOURCES.split(SLASH))
                .toFile().with { it.exists() ? it : null }
    }

    @Optional
    @InputDirectory
    final Closure<File> javascriptSources = {
        Paths.get(project.projectDir.canonicalPath,
                JavaPluginAction.JAVASCRIPT_SOURCES.split(SLASH))
                .toFile().with { it.exists() ? it : null }
    }

    @InputFile
    final File packageJson = new File(workingDir, PACKAGE_JSON_FILE)

    @Optional
    @OutputFile
    final Closure<File> polymerJson = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        vaadin.compatibilityMode ? new File(workingDir, 'polymer.json') : null
    }

    @Optional
    @OutputFile
    final Closure<File> statsJson = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        vaadin.compatibilityMode ? null : new File(configDir.call(), 'stats.json')
    }

    @Optional
    @OutputFile
    final Closure<File> infoJson = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        vaadin.compatibilityMode ? null : new File(configDir.call(), 'flow-build-info.json')
    }

    @Optional
    @OutputFile
    @Deprecated
    final Closure<File> mainJs = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        vaadin.compatibilityMode ? null : Paths.get(webappGenDir.canonicalPath, VAADIN, 'main.js').toFile()
    }

    @Optional
    @OutputFile
    @Deprecated
    final Closure<File> html = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        if (vaadin.compatibilityMode) {
            String fileHash = inputs.files.collect { it.exists() ? HashUtil.sha256(it) : '' }.join('')
            String propertyHash = inputs.properties.collect { key, value -> HashUtil.createCompactMD5(key + value) }
            String manifestHash = HashUtil.createCompactMD5(fileHash + propertyHash)
            return new File(workingDir, "vaadin-flow-bundle-${manifestHash}.cache.html")
        }
        null
    }

    @Optional
    @OutputDirectory
    @Deprecated
    final Closure<File> es5dir = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        vaadin.compatibilityMode ? Paths.get(workingDir.canonicalPath, BUILD, 'frontend-es5').toFile() : null
    }

    @Optional
    @OutputDirectory
    @Deprecated
    final Closure<File> es6dir = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        vaadin.compatibilityMode ? Paths.get(workingDir.canonicalPath, BUILD, 'frontend-es6').toFile() : null
    }

    @Optional
    @OutputFile
    @Deprecated
    final Closure<File> manifestJson = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        vaadin.compatibilityMode ? new File(workingDir, 'vaadin-flow-bundle-manifest.json') : null
    }

    @Optional
    @OutputDirectory
    @Deprecated
    final Closure<File> templatesDir = {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        vaadin.compatibilityMode ? new File(workingDir, TEMPLATES) : null
    }

    TranspileDependenciesTask() {
        dependsOn(
                'classes',
                InstallBowerDependenciesTask.NAME,
                InstallYarnDependenciesTask.NAME,
                WrapCssTask.NAME,
                ConvertGroovyTemplatesToHTML.NAME
        )
        onlyIf {
            VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
            if (vaadin.compatibilityMode) {
                project.extensions.getByType(VaadinClientDependenciesExtension).compileFromSources
            } else {
                true
            }
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

    @Internal
    @PackageScope
    void bundle(ScanResult scan) {

        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)

        LOGGER.info('Generating base theme module...')
        TemplateWriter.builder()
                .targetDir(srcDir)
                .templateFileName('theme.js')
                .targetFileName("${vaadin.baseTheme}-theme.js")
                .substitutions(['theme': vaadin.baseTheme])
                .build().write()

        LOGGER.info('Searching for JS modules...')
        Map<String, String> modules = [:]
        LogUtils.measureTime('Scanning Js modules completed') {
            modules = ClassIntrospectionUtils.findJsModules(scan)
        }

        LOGGER.info('Validating JS module imports...')
        removeInvalidModules(modules)

        LOGGER.info('Checking for theme variants of JS modules...')
        replaceBaseThemeModules(modules)

        LOGGER.info('Search for JS imports...')
        Map<String, String> jsImports = [:]
        LogUtils.measureTime('Scanning Js imports completed') {
            jsImports = ClassIntrospectionUtils.findJsImportsByRoute(scan)
                .collectEntries { k, v -> [ (k - 'frontend://') : v ] }
                .collectEntries { k, v -> [ (DOTSLASH + k) : v ] }
                .findAll { k, v -> !modules.containsKey(k.toString().replace(JAVASCRIPT_FILE_TYPE, '-es6.js')) }
        }

        LOGGER.info('Searching for CSS imports...')
        Map<String,String> cssImports = [:]
        LogUtils.measureTime('Scanning CSS imports completed') {
            cssImports = ClassIntrospectionUtils.findCssImports(scan)
        }

        LOGGER.info('Validating CSS imports..')
        removeInvalidCSSImports(cssImports)

        LOGGER.info('Excluding excluded imports...')
        getImportExcludes().each { filter ->
            cssImports.removeAll { m, c -> m.matches(filter) }
            modules.removeAll { m, c -> m.matches(filter) }
            jsImports.removeAll { m, c -> m.matches(filter) }
        }

        File importsJs = new File(srcDir, IMPORTS_JS_FILE)
        LOGGER.info("Creating ${importsJs.name}...")
        importsJs.parentFile.mkdirs()

        importsJs.text = '// Theme\n'
        importsJs << "import '${frontendAlias(DOTSLASH + vaadin.baseTheme)}-theme.js';\n"

        importsJs << '// Polymer modules\n'
        modules.keySet().sort { a, b ->
            (isInSourceFolder(a) <=> isInSourceFolder(b))
        }.each {
            importsJs << "import '${frontendAlias(it)}';\n"
        }

        importsJs << '// Javascript imports\n'
        jsImports.each { p, c -> importsJs << "import '${frontendAlias(p)}';\n" }

        importsJs << '// CSS imports\n'
        cssImports.each { p, c -> importsJs << "import '${frontendAlias(p) - '.css'}.js';\n" }

        LOGGER.info('Checking for legacy imports...')
        Map<String, File> legacyImports = ClassIntrospectionUtils.findHtmlImports(project, scan, vaadin.baseTheme)
        if (!legacyImports.isEmpty()) {
            LOGGER.warning('Found the following Flow 1 HTML imports:')
            legacyImports.each { importPath, importFile ->
                LOGGER.warning("\t$importPath in $importFile")
            }
            LOGGER.warning('To use them, please convert them to Polymer 3 Javascript imports')
        }

        Map<String, String> legacyStylesheetImports = ClassIntrospectionUtils
                .findStylesheetImports(scan)
                .findAll { k, c -> c.startsWith('com.vaadin') }
        if (!legacyStylesheetImports.isEmpty()) {
            LOGGER.warning('Found the following Flow 1 Stylesheet imports:')
            legacyStylesheetImports.each { importPath, className ->
                LOGGER.warning("\t$importPath in $className")
            }
            LOGGER.warning('To use them, please convert them to Css imports')
        }

        LOGGER.info('Bundling...')
        LogUtils.measureTime('Bundling completed') {
            yarnRunner.webpackBundle(project, statsJson.call(), infoJson.call())
        }
    }

    @Internal
    @PackageScope
    void checkIdUsage(ScanResult scan) {
        if (!ignoreIdUsage) {
            List<String> ids = ClassIntrospectionUtils.findIdUsages(scan)
            if (!ids.isEmpty()) {
                LOGGER.severe('Plugin does not currently support @Id annotations in Polymer templates')
                LOGGER.severe('The following classes contains @Id annotations:')
                ids.each { LOGGER.severe("\t$it") }
                LOGGER.severe('Please replace them with model access instead.')
                throw new GradleException('Unsupported @Id annotations found in polymer templates. ' +
                        RUN_WITH_INFO_FOR_MORE_INFORMATION)
            }
        }
    }

    @Internal
    @PackageScope
    void checkJsModulesInCompatibilityMode(ScanResult scan) {
        Map<String,String> modules = ClassIntrospectionUtils
                .findJsModules(scan)
                .findAll { k, v -> !v.startsWith(VAADIN_FLOW_PACKAGE) }
        if (!modules.isEmpty()) {
            LOGGER.severe('Javascript modules is not supported in compatibility mode.')
            LOGGER.severe('The following classes contains @JSModule annotations')
            modules.fineach { k, v -> LOGGER.severe("\t$v") }
            LOGGER.severe('Please use HTML imports instead.')
            throw new GradleException('Unsupported @JavascriptModule annotations found in compatibility mode. ' +
                    RUN_WITH_INFO_FOR_MORE_INFORMATION)
        }
    }

    @Internal
    @PackageScope
    void checkCssImportsInCompatibilityMode(ScanResult scan) {
        Map<String,String> imports = ClassIntrospectionUtils
                .findCssImports(scan)
                .findAll { k, v -> !v.startsWith(VAADIN_FLOW_PACKAGE) }
        if (!imports.isEmpty()) {
            LOGGER.severe('Css imports is not supported in compatibility mode.')
            LOGGER.severe('The following classes contains @CssImport annotations')
            imports.each { k, v -> LOGGER.severe("\t$v") }
            LOGGER.severe('Please use Stylesheet imports instead.')
            throw new GradleException('Unsupported @CssImport annotations found in compatibility mode. ' +
                    RUN_WITH_INFO_FOR_MORE_INFORMATION)
        }
    }

    @TaskAction
    void run() {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)

        if (vaadin.compatibilityMode) {
            LOGGER.info('Copying unpacked static resources...')
            project.copy { spec -> spec.from(unpackedStaticResources).into(workingDir) }
        }

        LOGGER.info( 'Copying generated styles...')
        if (vaadin.compatibilityMode) {
            project.copy { spec -> spec.from(webappGenFrontendDir).include(STYLES_GLOB).into(workingDir) }
        } else {
            project.copy { spec -> spec.from(JavaPluginAction.STYLESHEETS_SOURCES).into(srcDir) }
            project.copy { spec -> spec.from(JavaPluginAction.JAVASCRIPT_SOURCES).into(srcDir) }
        }

        if (vaadin.compatibilityMode) {
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
        }

        File stylesDir = webStylesDir.call()
        if (stylesDir) {
            LOGGER.info( 'Copying html styles...')
            project.copy { CopySpec spec ->
                spec.from(stylesDir.parentFile).include(STYLES_GLOB)
                        .into(vaadin.compatibilityMode ? workingDir : srcDir)
            }

            if (vaadin.compatibilityMode) {
                LOGGER.info('Validating html styles...')
                File templatesTargetDir = new File(workingDir, STYLES)
                validateImports(templatesTargetDir)
            }
        }

        File templatesDir = webTemplatesDir.call()
        if (templatesDir && !vaadin.compatibilityMode) {
            LOGGER.info( 'Copying Javascript templates...')
            project.copy { CopySpec spec ->
                spec.from(templatesDir.parentFile).include(TEMPLATES_GLOB)
                        .into(srcDir)
            }
        }

        LOGGER.info('Performing annotation scan...')
        LogUtils.measureTime('Scan completed') {
            ClassIntrospectionUtils.getAnnotationScan(project)
        }.withCloseable { ScanResult scan ->
            if (vaadin.compatibilityMode) {
                checkJsModulesInCompatibilityMode(scan)
                checkCssImportsInCompatibilityMode(scan)
                bundleInCompatibilityMode(scan)
            } else {
                checkIdUsage(scan)
                bundle(scan)
            }
        }
    }

    @Internal
    @PackageScope
    void bundleInCompatibilityMode(ScanResult scan) {
        LOGGER.info('Searching for HTML imports...')
        List<String> imports = initHTMLImportsFromComponents(scan)

        LOGGER.info('Removing excluded HTML imports..')
        getImportExcludes().each { filter -> imports.removeIf { it.matches(filter) } }

        File htmlFile = html.call()
        LOGGER.info("Creating ${htmlFile.name}...")
        initHtml(htmlFile, imports)

        LOGGER.info("Creating ${polymerJson.call().name}...")
        initPolymerJson(htmlFile, imports)

        LOGGER.info("Creating ${manifestJson.name}...")
        VaadinYarnRunner yarnBundleRunner = new VaadinYarnRunner(project, workingDir, true)
        yarnBundleRunner.polymerBundle(manifestJson.call(), htmlFile, getBundleExcludes())

        LOGGER.info('Checking for missing dependencies...')
        checkForMissingDependencies()

        LOGGER.info('Transpiling...')
        LogUtils.measureTime('Transpiling done successfully') {
            yarnRunner.transpile(getBundleExcludes())
        }

        LOGGER.info('Validating transpilation...')
        validateTranspilation()

        LOGGER.info('Cleaning up unpacked static resources...')
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
    }

    /**
     * Sets the bundle exclusions
     *
     * @deprecated since 1.3 in favour of importExcludes
     */
    @Deprecated
    void setBundleExcludes(List<String> excludes) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        if (vaadin.compatibilityMode) {
            bundleExcludes.set(excludes)
        } else {
            LOGGER.warning(WARN_BUNDLE_EXCLUDES_ONLY_AVAILABLE_IN_COMP_MODE)
        }
    }

    /**
     * Get bundle exclusions
     *
     * @deprecated since 1.3 in favour of importExcludes
     */
    @Deprecated
    List<String> getBundleExcludes() {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        if (vaadin.compatibilityMode) {
            return bundleExcludes.getOrElse([])
        }
        throw new GradleException(WARN_BUNDLE_EXCLUDES_ONLY_AVAILABLE_IN_COMP_MODE)
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
        imports += ClassIntrospectionUtils.findHtmlImports(project, scan, vaadin.baseTheme).keySet()
        imports += ClassIntrospectionUtils.findJsImports(scan).keySet()
        imports += ClassIntrospectionUtils.findStylesheetImports(scan).keySet()
        imports.sort()
    }

    private void initPolymerJson(File html, List<String> sources) {

        List<String> extraDependencies = [manifestJson.call().name]

        if (bowerComponents.call()?.exists()) {
            File webcomponentsjs = new File(bowerComponents.call(), 'webcomponentsjs')
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
                    new PolymerBuild.CustomBuild(name: es5dir.call().name).with { js.compile = true; it },
                    new PolymerBuild.CustomBuild(name: es6dir.call().name)
                ]
        )

        polymerJson.call().text = ClientPackageUtils.toJson(buildModel)
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
        File manifestFile = manifestJson.call()
        Map<String, Object> manifest = new JsonSlurper().parse(manifestFile) as Map
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
                        RUN_WITH_INFO_FOR_MORE_INFORMATION)
            }
            manifest.remove(MISSING_PROPERTY)
            manifestFile.text = ClientPackageUtils.toJson(manifest)
        }
    }

    private void removeInvalidModules(Map<String,String> modules) {
        modules.removeAll { m, c ->
            File nodeDependency = Paths.get(appNodeModules.call().canonicalPath, m.split(SLASH)).toFile()
            File staticFile = Paths.get(srcDir.canonicalPath, m.split(SLASH)).toFile()
            File templateFile = Paths.get(srcDir.parentFile.canonicalPath, m.split(SLASH)).toFile()
            boolean exists = nodeDependency.exists() || staticFile.exists() || templateFile.exists()
            if (!exists) {
                LOGGER.warning("$c: No Javascript module with the name '$m' could be found. Module ignored.")
                return true
            }
            if (!nodeDependency.exists() && staticFile.exists() && !m.startsWith(DOTSLASH)) {
                LOGGER.warning("$c: Static file Javascript module '$m' does not start with './'. Module ignored.")
                return true
            }
            false
        }
    }

    private void replaceBaseThemeModules(Map<String,String> modules) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        new HashSet<String>(modules.keySet()).each { m ->
            File nodeDependency = Paths.get(appNodeModules.call().canonicalPath, m.split(SLASH)).toFile()
            if (nodeDependency.exists()) {
                File baseTheme = Paths.get(nodeDependency.parentFile.parentFile.canonicalPath,
                        'theme', vaadin.baseTheme).toFile()
                if (baseTheme.exists()) {
                    File nodeThemeDependency = new File(baseTheme, nodeDependency.name)
                    if (nodeThemeDependency.exists()) {
                        String mt = (nodeThemeDependency.canonicalPath - appNodeModules.call().canonicalPath)
                                .substring(1)
                        mt = mt.replace('\\', SLASH) // Do not use Windows paths
                        modules[mt] = modules.remove(m)
                        LOGGER.info("\tReplaced: $m -> $mt")
                    }
                }
            }
        }
    }

    private void removeInvalidCSSImports(Map<String,String> imports) {
        imports.removeAll { p, c ->
            File importFile = Paths.get(srcDir.canonicalPath, p.split(SLASH)).toFile()
            if (!importFile.exists()) {
                LOGGER.warning("$c: No Css import with the name '$p' could be found. Import ignored.")
                return true
            }
            false
        }
    }

    private void validateTranspilation() {
        if (!es5dir.call().exists()) {
            throw new GradleException(
                    "Transpile did not generate ES5 result in $es5dir. Run with --info to get more information.")
        }
        if (!es6dir.call().exists()) {
            throw new GradleException(
                    "Transpile did not generate ES6 result in $es6dir. Run with --info to get more information.")
        }
    }

    @PackageScope
    boolean isInSourceFolder(String path) {
        new File(srcDir, path).exists()
    }

    @PackageScope
    static String frontendAlias(String path) {
        path.replaceFirst(/^\./, 'Frontend')
    }
}
