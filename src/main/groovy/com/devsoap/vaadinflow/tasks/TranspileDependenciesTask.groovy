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
import com.devsoap.vaadinflow.util.LogUtils
import com.moowork.gradle.node.yarn.YarnExecRunner
import groovy.json.JsonOutput
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.logging.Level

/**
 * Transpiles web components to ES5 and ES6 production artifacts
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class TranspileDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinTranspileDependencies'

    private static final String POLYMER_COMMAND = 'polymer'
    private static final String BOWER_JSON_FILE = 'bower.json'
    private static final String PACKAGE_JSON_FILE = 'package.json'
    private static final String SLASH = '/'
    private static final String BOWER_COMPONENTS = 'bower_components'
    private static final String NODE_MODULES = 'node_modules'
    private static final String RUN_COMMAND = 'run'

    final YarnExecRunner yarnRunner = new YarnExecRunner(project).with {
        execOverrides = { ExecSpec spec ->
            spec.standardOutput = LogUtils.getLogOutputStream(Level.FINE)
            spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
        }
        it
    }

    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)
    final File webappGenDir = new File(project.buildDir, 'webapp-gen')
    final File webappGenFrontendDir = new File(webappGenDir, 'frontend')

    @InputDirectory
    final File webappGenFrontendStylesDir = new File(webappGenFrontendDir, 'styles')

    @InputFile
    final File packageJson = new File(workingDir, PACKAGE_JSON_FILE)

    @OutputFile
    final File polymerJson = new File(workingDir, 'polymer.json')

    @OutputFile
    final File html = new File(workingDir, "vaadin-flow-bundle-${ UUID.randomUUID().toString()[0..8] }.html")

    @OutputDirectory
    final File es5dir = new File(workingDir, 'frontend-es5')

    @OutputDirectory
    final File es6dir = new File(workingDir, 'frontend-es6')

    @OutputFile
    final File manifestJson = new File(workingDir, 'vaadin-flow-bundle-manifest.json')

    TranspileDependenciesTask() {
        dependsOn(InstallBowerDependenciesTask.NAME, InstallYarnDependenciesTask.NAME, ConvertCssToHtmlStyleTask.NAME)
        onlyIf {
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            client.compileFromSources
        }

        description = 'Compiles client modules to support legacy browsers'
        group = 'Vaadin'
        yarnRunner.workingDir = workingDir
    }

    @TaskAction
    void run() {

        LOGGER.info('Unpacking webjars...')
        unpackWebjars(workingDir, project)

        LOGGER.info( 'Copying generated styles....')
        project.copy { spec -> spec.from(webappGenFrontendDir).include('**/styles/**').into(workingDir) }

        LOGGER.info('Searching for HTML imports')
        List<String> imports = initHTMLImports()

        LOGGER.info("Creating ${html.name}...")
        initHtml(imports)

        LOGGER.info("Creating ${polymerJson.name}...")
        List<String> sources = initModuleSources(imports)
        initPolymerJson(sources)

        LOGGER.info("Creating ${manifestJson.name}...")
        YarnExecRunner yarnBundleRunner = new YarnExecRunner(project).with {
            execOverrides = { ExecSpec spec ->
                spec.standardOutput = new ByteArrayOutputStream()
                spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
            }
            it
        }
        yarnBundleRunner.workingDir = workingDir
        yarnBundleRunner.arguments = [RUN_COMMAND, 'polymer-bundler',
                                      "--manifest-out=${manifestJson.canonicalPath}", html.name]
        yarnBundleRunner.execute().assertNormalExitValue()

        LOGGER.info('Transpiling...')
        yarnRunner.arguments = [RUN_COMMAND, POLYMER_COMMAND, 'build', '--npm', "--component-dir='node_modules'"]
        yarnRunner.execute().assertNormalExitValue()
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
        List<File> scanDirs = [new File(workingDir, NODE_MODULES), new File(workingDir, BOWER_COMPONENTS)]
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
                        '**/templates/**',
                        '**/polymer-cli/**')
                        .each { File htmlFile ->

                    if (new File(dir, TranspileDependenciesTask.BOWER_JSON_FILE).exists()) {
                        String path = (htmlFile.path - workingDir.path).substring(1)
                        imports.add(path)
                    }
                }
            }
        }

        LOGGER.info("Searching for html imports in $webappGenFrontendStylesDir")
        webappGenFrontendDir.eachDir { dir ->
            project.fileTree(dir)
                    .include(htmlIncludeGlob)
                    .each { File htmlFile ->
                String path = (htmlFile.path - webappGenFrontendDir.path).substring(1)
                imports.add(path)
            }
        }

        imports.each { LOGGER.info("Found import $it") }

        Collections.unmodifiableList(imports)
    }

    private void initPolymerJson(List<String> sources) {

        List<String> extraDependencies = [manifestJson.name]

        File bowerComponents = new File(workingDir, BOWER_COMPONENTS)
        File webcomponentsjs = new File(bowerComponents, 'webcomponentsjs')
        webcomponentsjs.eachFile {
            if (it.name.endsWith('.js') || it.name.endsWith('.js.map')) {
                extraDependencies.add("$BOWER_COMPONENTS/webcomponentsjs/$it.name")
            }
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

    private static void unpackWebjars(File targetDir, Project project) {
        File nodeModules = new File(targetDir, NODE_MODULES)
        if (!nodeModules.exists()) {
            nodeModules.mkdirs()
        }

        File bowerComponents = new File(targetDir, BOWER_COMPONENTS)
        if (!bowerComponents.exists()) {
            bowerComponents.mkdirs()
        }

        List<Configuration> configs = project.configurations
                .findAll { ['compile', 'implementation'].contains(it.name) }
                .collect { it.canBeResolved ? it : it.copy().with { canBeResolved = true; it } }

        configs.each { Configuration conf ->

            Set<Dependency> artifactDependencies = conf.allDependencies.findAll { !(it instanceof ProjectDependency) }

            artifactDependencies.each { Dependency dependency ->

                Set<File> jarFiles = conf.files(dependency).findAll { it.file && it.name.endsWith('.jar') }

                jarFiles.each { File file ->

                    // Check for bower.json
                    Tuple2<String, String> result = findFolderAndPath(TranspileDependenciesTask.BOWER_JSON_FILE, file)
                    boolean isBowerJar = result

                    // Check for package.json if no bower.json was found
                    result = result ?: findFolderAndPath(NODE_MODULES, file)

                    if (result) {
                        String packageJsonFolder = result.first
                        String componentRootPackage = result.second

                        File componentRoot = new File(isBowerJar ? bowerComponents : nodeModules, componentRootPackage)

                        if (componentRoot.exists()) {
                            TranspileDependenciesTask.LOGGER.info(
                                    "Skipped ${packageJsonFolder}, directory already exists.")
                        } else {
                            copyJarToFolder(file, packageJsonFolder, componentRoot)
                            TranspileDependenciesTask.LOGGER.info(
                                    "Unpacked ${dependency.group}.${dependency.name} into $componentRoot")
                        }
                    }
                }
            }
        }
    }

    private void initHtml(List<String> imports) {
        if (!html.exists()) {
            html.createNewFile()
        }

        html.withPrintWriter { writer ->
            imports.each { writer.write("<link rel='import' href='$it' >\n") }
        }
    }

    private static Tuple2<String, String> findFolderAndPath(String searchFileName, File jarFile) {
        Tuple2<String, String> result = null
        jarFile.withInputStream { InputStream stream ->
            JarInputStream jarStream = new JarInputStream(stream)
            JarEntry entry
            while ((entry = jarStream.nextJarEntry) != null) {
                if (entry.name.endsWith(searchFileName)) {
                    String packageJsonFolder = entry.name - searchFileName
                    String componentRootPackage = packageJsonFolder.split(SLASH).last()
                    result = new Tuple2<>(packageJsonFolder, componentRootPackage)
                    break
                }
            }
        }
        result
    }

    private static void copyJarToFolder(File file, String packageJsonFolder, File componentRoot) {
        componentRoot.mkdirs()
        file.withInputStream { InputStream stream ->
            JarInputStream jarStream = new JarInputStream(stream)
            JarEntry entry
            JarFile jarFile = new JarFile(file)
            while ((entry = jarStream.nextJarEntry) != null) {
                if (entry.name.startsWith(packageJsonFolder) &&
                        entry.name != packageJsonFolder) {
                    String filename = entry.name[packageJsonFolder.length()..-1]
                    if (filename) {
                        File f = Paths.get(componentRoot.canonicalPath, filename
                                .split(SLASH)).toFile()
                        if (!f.parentFile.exists()) {
                            f.parentFile.mkdirs()
                        }

                        if (entry.directory && !f.exists()) {
                            f.mkdirs()
                        } else {
                            jarFile.getInputStream(entry).with { is ->
                                Files.copy(is, f.toPath())
                            }
                        }
                    }
                }
            }
        }
    }
}
