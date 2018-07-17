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

import com.moowork.gradle.node.yarn.YarnExecRunner
import org.gradle.util.GFileUtils

import static com.devsoap.vaadinflow.models.PolymerBuild.Build

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.models.ClientPackage
import com.devsoap.vaadinflow.models.PolymerBuild
import com.devsoap.vaadinflow.util.LogUtils
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
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
    private static final String SRC = 'src'
    private static final String NODE_MODULES = 'node_modules'

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
    final File html = new File(workingDir, 'index.html')

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

        // Add polymer-cli as a script
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.scripts[POLYMER_COMMAND] = POLYMER_COMMAND
        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))

        LOGGER.info('Unpacking webjars...')
        unpackWebjars(workingDir, project)

        LOGGER.info( 'Copying generated styles....')
        project.copy { spec -> spec.from(webappGenFrontendDir).include('**/styles/**').into(workingDir) }

        LOGGER.info('Searching for HTML imports')
        List<String> imports = initHTMLImports()

        LOGGER.info('Creating index.html...')
        initHtml(imports)

        LOGGER.info('Creating polymer.json...')
        List<String> sources = initModuleSources(imports)
        initPolymerJson(sources)

        // Run polymer build
        LOGGER.info('Transpiling...')
        yarnRunner.arguments = ['run', POLYMER_COMMAND, 'build', '--npm']
        yarnRunner.execute().assertNormalExitValue()

        // Generate manifest
        // FIXME As a workaround for #60 generate an empty file. In the future we should make this configurable (#63)
        LOGGER.info('Creating manifest...')
        manifestJson.text = '{}'
    }

    private static List<String> initModuleSources(List<String> imports) {
        List<String> sources = imports.collect { htmlPath ->
            String path = htmlPath.split(/\//).dropRight(1).join('/')
            if(path.startsWith(BOWER_COMPONENTS) || path.startsWith(NODE_MODULES)) {
                path += '/src/**/*'
            } else {
                path += '/**/*'
            }
            path
        }
        sources
    }

    private List<String> initHTMLImports() {

        List<String> imports = []
        File nodeModules = new File(workingDir, NODE_MODULES)
        String htmlIncludeGlob = '**/*.html'

        LOGGER.info("Searching for html imports in $nodeModules")
        nodeModules.eachDir { dir ->
            project.fileTree(dir)
                    .include(htmlIncludeGlob)
                    .exclude(
                    '**/index.html',
                    '**/demo/**',
                    '**/test*/**',
                    '**/src/**',
                    '**/lib/**',
                    '**/templates/**')
                    .each { File htmlFile ->


                String path = (htmlFile.path - workingDir.path).substring(1)
                imports.add(path)
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

        imports.each {LOGGER.info("Found import $it")}

        Collections.unmodifiableList(imports)
    }

    private void initPolymerJson(List<String> sources) {
        PolymerBuild buildModel = new PolymerBuild(
                entrypoint: html.name,
                sources: sources,
                extraDependencies : ['node_modules/webcomponentsjs/webcomponents-lite.js'],
                builds: [
                    new Build(name: es5dir.name).with { js.compile = true; it },
                    new Build(name: es6dir.name)
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

    private static List<File> unpackWebjars(File targetDir, Project project) {
        List<File> unpackedJars = []
        File nodeModules = new File(targetDir, NODE_MODULES)
        if (!nodeModules.exists()) {
            nodeModules.mkdirs()
        }

        project.configurations.all.each { Configuration conf ->
            if (['compile', 'implementation'].contains(conf.name)) {
                Configuration cc
                if(conf.canBeResolved) {
                    cc = conf
                } else {
                    // Work on copies which are resolvable
                    cc = conf.copy()
                    cc.canBeResolved = true
                }
                cc.allDependencies.each { Dependency dependency ->
                    if (!(dependency instanceof ProjectDependency) ){
                        cc.files(dependency).each { File file ->
                            if (file.file && file.name.endsWith('.jar')) {

                                // Find package.json
                                String packageJsonFolder = null
                                String componentRootPackage = null
                                file.withInputStream { InputStream stream ->
                                    JarInputStream jarStream = new JarInputStream(stream)
                                    JarEntry entry
                                    while ((entry = jarStream.nextJarEntry) != null) {
                                        if (entry.name.endsWith(PACKAGE_JSON_FILE)) {
                                            packageJsonFolder = entry.name - PACKAGE_JSON_FILE
                                            componentRootPackage = packageJsonFolder.split(SLASH).last()
                                            break
                                        }
                                    }
                                }

                                if(!packageJsonFolder || !componentRootPackage) {
                                    // Some webjars does not ship with a package.json, try bower.json
                                    file.withInputStream { InputStream stream ->
                                        JarInputStream jarStream = new JarInputStream(stream)
                                        JarEntry entry
                                        while ((entry = jarStream.nextJarEntry) != null) {
                                            if (entry.name.endsWith(BOWER_JSON_FILE)) {
                                                packageJsonFolder = entry.name - BOWER_JSON_FILE
                                                componentRootPackage = packageJsonFolder.split(SLASH).last()
                                                break
                                            }
                                        }
                                    }
                                }

                                // Unpack directory with package.json
                                if (packageJsonFolder && componentRootPackage) {
                                    TranspileDependenciesTask.LOGGER.info("Found package.json in ${file.name}, " +
                                            "unpacking ${packageJsonFolder} ...")

                                    File componentRoot = new File(nodeModules, componentRootPackage)

                                    if (componentRoot.exists()) {
                                        TranspileDependenciesTask.LOGGER.info("Skipped ${packageJsonFolder}, " +
                                                'directory already exists.')
                                    } else {
                                        componentRoot.mkdirs()
                                        unpackedJars.add(componentRoot)
                                        TranspileDependenciesTask.LOGGER.info(
                                                "Unpacked ${dependency.group}.${dependency.name} into $componentRoot")
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
                            }
                        }
                    }
                }
            }
        }
        unpackedJars
    }
}
