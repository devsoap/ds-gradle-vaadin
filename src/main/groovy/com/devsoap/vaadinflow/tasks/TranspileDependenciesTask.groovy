package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.models.ClientPackage
import com.devsoap.vaadinflow.models.PolymerBuild
import com.moowork.gradle.node.npm.NpmExecRunner
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.*

import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream

import static com.devsoap.vaadinflow.models.PolymerBuild.Build

@Log("LOGGER")
class TranspileDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinTranspileDependencies'

    final NpmExecRunner npmExecRunner = new NpmExecRunner(project)

    @InputDirectory
    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    @InputFile
    @OutputFile
    final File packageJson = new File(workingDir, 'package.json')

    @OutputFile
    final File polymerJson = new File(workingDir, 'polymer.json')

    @InputFile
    final File bowerJson = new File(workingDir, 'bower.json')

    @OutputFile
    final File html = new File(workingDir, 'index.html')

    @OutputDirectories
    final File es5dir = new File(workingDir, 'frontend-es5')

    @OutputDirectory
    final File es6dir = new File(workingDir, 'frontend-es6')

    TranspileDependenciesTask() {
        dependsOn(InstallBowerDependenciesTask.NAME, InstallYarnDependenciesTask.NAME)
        description = 'Compiles client modules to support legacy browsers'
        group = 'Vaadin'
        npmExecRunner.workingDir = workingDir
    }

    @TaskAction
    void run() {

        unpackWebjars(workingDir, project)

        // Add polymer-cli as a script
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.scripts['polymer'] = 'polymer'
        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))

        // Create polymer.json
        PolymerBuild buildModel = PolymerBuild.builder()
            .entrypoint(html.name)
            .sources([bowerJson.name])
            .extraDependencies(['bower_components/webcomponentsjs/webcomponents-lite.js'])
            .builds([
                new Build(name: es5dir.name).with { js.compile = true; it },
                new Build(name: es6dir.name)
            ])
            .build()
        polymerJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(buildModel))

        // Create index.html
        if(!html.exists()){
            html.createNewFile()
        }

        // Run polymer build
        npmExecRunner.arguments = ['run', 'polymer', 'build']
        npmExecRunner.execute().assertNormalExitValue()
    }

    private static void unpackWebjars(File targetDir, Project project) {
        File bowerComponents = new File(targetDir, 'bower_components')
        if(!bowerComponents.exists()) {
            bowerComponents.mkdirs()
        }

        project.configurations.all.each { Configuration conf ->
            if(conf.canBeResolved && ['compile', 'implementation'].contains(conf.name)) {
                conf.allDependencies.each { Dependency dependency ->
                    if(!(dependency instanceof ProjectDependency)) {
                        conf.files(dependency).each { File file ->
                            if (file.file && file.name.endsWith('.jar')) {

                                // Find bower.json
                                String bowerJsonPackage
                                String componentRootPackage
                                file.withInputStream { InputStream stream ->
                                    JarInputStream jarStream = new JarInputStream(stream)
                                    JarEntry entry
                                    while ((entry = jarStream.nextJarEntry) != null) {
                                        if(entry.name.endsWith('bower.json')) {
                                            bowerJsonPackage = entry.name - 'bower.json'
                                            componentRootPackage = bowerJsonPackage.split('/').last()
                                            break
                                        }
                                    }
                                }

                                // Unpack directory with bower.json
                                if(bowerJsonPackage) {
                                    TranspileDependenciesTask.LOGGER.info("Found bower.json in ${file.name}, unpacking ${bowerJsonPackage} ...")

                                    File componentRoot = new File(bowerComponents, componentRootPackage)
                                    if(!componentRoot.exists()) {
                                        componentRoot.mkdirs()
                                        file.withInputStream { InputStream stream ->
                                            JarInputStream jarStream = new JarInputStream(stream)
                                            JarEntry entry
                                            JarFile jarFile = new JarFile(file)
                                            while ((entry = jarStream.nextJarEntry) != null) {
                                                if(entry.name.startsWith(bowerJsonPackage)) {
                                                    String filename = entry.name.substring(bowerJsonPackage.length())
                                                    if(filename) {
                                                        File f = Paths.get(componentRoot.canonicalPath, filename.split('/')).toFile()
                                                        if(!f.parentFile.exists()) {
                                                            f.parentFile.mkdirs()
                                                        }

                                                        if(entry.directory && !f.exists()) {
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
                                    } else {
                                        TranspileDependenciesTask.LOGGER.info("Skipped ${bowerJsonPackage}, directory already exists.")
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
