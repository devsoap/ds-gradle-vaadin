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
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.BasePlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GFileUtils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream

/**
 * Helper utilites for handling WebJars
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class WebJarHelper {

    private static final String SLASH = '/'
    private static final String FRONTEND_RESOURCES_META_DIR = 'META-INF/resources/frontend/'
    private static final String PACKAGE_JSON = 'package.json'
    private static final String JAR_EXTENSION = '.jar'
    private static final String BOWER_JSON = 'bower.json'

    /**
     * Webjars which do not contain a bower.json BUT needs to be considered as bower dependencies when unpacking
     */
    private static final List<String> forcedBowerPackages = [
            'shadycss'
    ]

    /**
     * Unpacks WebJars into a target directory
     *
     * @param targetDir
     *      the target directory to unpack to
     * @param project
     *      the project
     * @param moduleDirName
     *      the module directory (bower_components or node_modules)
     * @param bower
     *      Is th webjar a bower dependency
     * @return
     *      A list of directories representing the unpacked target directories
     */
    static void unpackWebjars(File targetDir, File resourceTargetDir, Project project, String moduleDirName,
                              boolean bower) {
        File componentsDir = new File(targetDir, moduleDirName)
        if (!componentsDir.exists()) {
            componentsDir.mkdirs()
        }

        List<Configuration> configs = findConfigurations(project)
        configs.each { Configuration conf ->

            conf.allDependencies.each { Dependency dependency ->

                Set<File> jarFiles = []
                if (dependency instanceof ProjectDependency) {
                    Project dependantProject = dependency.dependencyProject

                    // Java projects
                    dependantProject.tasks.withType(Jar).each {
                       jarFiles += it.archivePath
                    }

                    // Other projects
                    dependantProject.plugins.withType(BasePlugin).each {
                        File bowerJson = new File(dependantProject.projectDir, BOWER_JSON)
                        File packageJson = new File(dependantProject.projectDir, PACKAGE_JSON)
                        // bower.json or package.json MUST be in the root of the project to qualify
                        if ((bower && bowerJson.exists()) || (!bower && packageJson.exists())) {
                            File componentRoot = new File(componentsDir, dependantProject.name)
                            if (componentRoot.exists()) {
                                GFileUtils.deleteDirectory(componentRoot)
                            }
                            LOGGER.info("Unpacking frontend component in $dependantProject into $componentRoot")
                            GFileUtils.copyDirectory(dependantProject.projectDir, componentRoot)
                        }
                    }
                } else {
                     jarFiles += conf.files(dependency).findAll { it.file && it.name.endsWith(JAR_EXTENSION) }
                }

                jarFiles.each { File file ->

                    Tuple2<String, String> result = findFolderAndPath(BOWER_JSON, file)
                    if (bower) {
                        if (!result && forcedBowerPackages.find { key -> file.name.startsWith(key) }) {
                            result = findFolderAndPath(PACKAGE_JSON, file)
                        }
                    } else {
                        if (result) {
                            // Bower package found, skip it
                            result = null
                        } else {
                            result = findFolderAndPath(PACKAGE_JSON, file)
                        }
                    }

                    if (result) {
                        String packageJsonFolder = result.first
                        String componentRootPackage = result.second

                        File componentRoot = new File(componentsDir, componentRootPackage)
                        if (componentRoot.exists()) {
                            GFileUtils.deleteDirectory(componentRoot)
                        }

                        LOGGER.info("Unpacking frontend component in $file.name into $componentRoot")
                        copyJarToFolder(file, packageJsonFolder, componentRoot)
                    }

                    if (resourceTargetDir && findFolder(FRONTEND_RESOURCES_META_DIR, file)) {
                        LOGGER.info("Unpacking frontend resources in $file.name into $resourceTargetDir")
                        copyJarToFolder(file, FRONTEND_RESOURCES_META_DIR, resourceTargetDir)
                    }
                }
            }
        }
    }

    /**
     * Searches the project for dependant JAR tasks
     *
     * @param project
     *      The project which dependencies should be searched
     * @return
     *      A list of jar tasks
     */
    static List<Jar> findDependantJarTasks(Project project) {
        List<Jar> jarTasks = []
        findConfigurations(project).each { Configuration conf ->
            conf.allDependencies.each { Dependency dependency ->
                if (dependency instanceof ProjectDependency) {
                    Project dependantProject = dependency.dependencyProject
                    println dependantProject.tasks
                    dependantProject.tasks.withType(Jar).each {
                        jarTasks << it
                    }
                }
            }
        }
        jarTasks
    }

    private static Tuple2<String, String> findFolderAndPath(String searchFileName, File jarFile) {
        Tuple2<String, String> result = null
        jarFile.withInputStream { InputStream stream ->
            JarInputStream jarStream = new JarInputStream(stream)
            JarEntry entry
            while ((entry = jarStream.nextJarEntry) != null) {
                if (entry.name.endsWith(searchFileName)) {
                    String packageJsonFolder = entry.name - searchFileName
                    List<String> packages = packageJsonFolder.tokenize(SLASH)
                    if (packages.empty) {
                        result = new Tuple2<>(SLASH, jarFile.name - JAR_EXTENSION)
                        break
                    }

                    String componentRootPackage = null
                    while (!componentRootPackage) {
                        String pkg = packages.removeLast()
                        if (!pkg.startsWith('.')) {
                            componentRootPackage = pkg
                        }
                    }
                    result = new Tuple2<>(packages.join(SLASH) + SLASH + componentRootPackage, componentRootPackage)
                    break
                }
            }
        }
        result
    }

    private static boolean findFolder(String searchFolder, File jarFile) {
        boolean found = false
        jarFile.withInputStream { InputStream stream ->
            JarInputStream jarStream = new JarInputStream(stream)
            JarEntry entry
            while ((entry = jarStream.nextJarEntry) != null) {
                if (entry.name == searchFolder) {
                    found = true
                    break
                }
            }
        }
        found
    }

    private static void copyJarToFolder(File file, String packageJsonFolder, File componentRoot) {
        componentRoot.mkdirs()
        file.withInputStream { InputStream stream ->
            JarInputStream jarStream = new JarInputStream(stream)
            JarEntry entry
            JarFile jarFile = new JarFile(file)
            while ((entry = jarStream.nextJarEntry) != null) {
                if (packageJsonFolder == SLASH ||
                        entry.name.startsWith(packageJsonFolder) &&
                        entry.name != packageJsonFolder) {
                    String filename = packageJsonFolder == SLASH ?
                            entry.name : entry.name[packageJsonFolder.length()..-1]
                    if (filename) {
                        File f = Paths.get(componentRoot.canonicalPath, filename.split(SLASH)).toFile()
                        if (!f.parentFile.exists()) {
                            f.parentFile.mkdirs()
                        }

                        if (entry.directory && !f.exists()) {
                            f.mkdirs()
                        } else if (!f.exists()) {
                            jarFile.getInputStream(entry).with { is ->
                                Files.copy(is, f.toPath())
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<Configuration> findConfigurations(Project project) {
        project.configurations
                .findAll { ['compile', 'implementation'].contains(it.name) }
                .collect { it.canBeResolved ? it : it.copy().with { canBeResolved = true; it } }
    }
}
