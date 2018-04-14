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

import static com.devsoap.vaadinflow.models.PolymerBuild.Build

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.models.ClientPackage
import com.devsoap.vaadinflow.models.PolymerBuild
import com.devsoap.vaadinflow.util.LogUtils
import com.moowork.gradle.node.npm.NpmExecRunner
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
import org.gradle.api.tasks.OutputDirectories
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
    public static final String POLYMER_COMMAND = 'polymer'
    public static final String BOWER_JSON_FILE = 'bower.json'
    public static final String SLASH = '/'

    final NpmExecRunner npmExecRunner = new NpmExecRunner(project).with {
        execOverrides = { ExecSpec spec ->
            spec.standardOutput = LogUtils.getLogOutputStream(Level.FINE)
            spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
        }
        it
    }

    @InputDirectory
    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    final File frontendDir = project.file(VaadinClientDependenciesExtension.FRONTEND_DIR)

    @InputFile
    @OutputFile
    final File packageJson = new File(workingDir, 'package.json')

    @OutputFile
    final File polymerJson = new File(workingDir, 'polymer.json')

    @InputFile
    final File bowerJson = new File(workingDir, BOWER_JSON_FILE)

    @OutputFile
    final File html = new File(workingDir, 'index.html')

    @OutputDirectories
    final File es5dir = new File(workingDir, 'frontend-es5')

    @OutputDirectory
    final File es6dir = new File(workingDir, 'frontend-es6')

    TranspileDependenciesTask() {
        dependsOn(InstallBowerDependenciesTask.NAME, InstallYarnDependenciesTask.NAME)
        onlyIf {
            VaadinFlowPluginExtension vaadin = project.extensions[VaadinFlowPluginExtension.NAME]
            vaadin.supportLegacyBrowsers
        }
        description = 'Compiles client modules to support legacy browsers'
        group = 'Vaadin'
        npmExecRunner.workingDir = workingDir
        project.afterEvaluate {
            if (frontendDir.exists()) {
                inputs.dir(frontendDir)
            }
        }
    }

    @TaskAction
    void run() {
        if (frontendDir.exists()) {
            copyFrontend(frontendDir.parentFile, workingDir, project)
        }

        unpackWebjars(workingDir, project)

        // Add polymer-cli as a script
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.scripts[POLYMER_COMMAND] = POLYMER_COMMAND
        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))

        initPolymerJson()

        initHtml()

        // Run polymer build
        npmExecRunner.arguments = ['run', POLYMER_COMMAND, 'build']
        npmExecRunner.execute().assertNormalExitValue()
    }

    private void initPolymerJson() {
        PolymerBuild buildModel = PolymerBuild.builder()
                .entrypoint(html.name)
                .sources([bowerJson.name])
                .extraDependencies(['bower_components/webcomponentsjs/webcomponents-lite.js'])
                .builds([
                new Build(name: es5dir.name).with { js.compile = true; it },
                new Build(name: es6dir.name)
        ]).build()
        polymerJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(buildModel))
    }

    private void initHtml() {
        if (!html.exists()) {
            html.createNewFile()
        }
        // FIXME Populate html file
    }

    private static void copyFrontend(File sourceDir, File targetDir, Project project) {
        project.copy { spec ->
            spec.from(sourceDir)
                .include('frontend/**')
                .into(targetDir)
        }
    }

    private static void unpackWebjars(File targetDir, Project project) {
        File bowerComponents = new File(targetDir, 'bower_components')
        if (!bowerComponents.exists()) {
            bowerComponents.mkdirs()
        }

        project.configurations.all.each { Configuration conf ->
            if (conf.canBeResolved && ['compile', 'implementation'].contains(conf.name)) {
                conf.allDependencies.each { Dependency dependency ->
                    if (!(dependency instanceof ProjectDependency)) {
                        conf.files(dependency).each { File file ->
                            if (file.file && file.name.endsWith('.jar')) {

                                // Find bower.json
                                String bowerJsonPackage
                                String componentRootPackage
                                file.withInputStream { InputStream stream ->
                                    JarInputStream jarStream = new JarInputStream(stream)
                                    JarEntry entry
                                    while ((entry = jarStream.nextJarEntry) != null) {
                                        if (entry.name.endsWith(BOWER_JSON_FILE)) {
                                            bowerJsonPackage = entry.name - BOWER_JSON_FILE
                                            componentRootPackage = bowerJsonPackage.split(SLASH).last()
                                            break
                                        }
                                    }
                                }

                                // Unpack directory with bower.json
                                if (bowerJsonPackage) {
                                    TranspileDependenciesTask.LOGGER.info("Found bower.json in ${file.name}, " +
                                            "unpacking ${bowerJsonPackage} ...")

                                    File componentRoot = new File(bowerComponents, componentRootPackage)
                                    if (componentRoot.exists()) {
                                        TranspileDependenciesTask.LOGGER.info("Skipped ${bowerJsonPackage}, " +
                                                'directory already exists.')
                                    } else {
                                        componentRoot.mkdirs()
                                        file.withInputStream { InputStream stream ->
                                            JarInputStream jarStream = new JarInputStream(stream)
                                            JarEntry entry
                                            JarFile jarFile = new JarFile(file)
                                            while ((entry = jarStream.nextJarEntry) != null) {
                                                if (entry.name.startsWith(bowerJsonPackage) &&
                                                        entry.name != bowerJsonPackage) {
                                                    String filename = entry.name[bowerJsonPackage.length()..-1]
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
    }
}
