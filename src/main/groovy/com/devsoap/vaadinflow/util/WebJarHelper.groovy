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
package com.devsoap.vaadinflow.util

import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.util.GFileUtils

import java.nio.file.Files
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

    static void unpackWebjars(File targetDir, Project project, String moduleDirName, boolean bower) {
        File componentsDir = new File(targetDir, moduleDirName)
        if (!componentsDir.exists()) {
            componentsDir.mkdirs()
        }

        List<Configuration> configs = project.configurations
                .findAll { ['compile', 'implementation'].contains(it.name) }
                .collect { it.canBeResolved ? it : it.copy().with { canBeResolved = true; it } }

        configs.each { Configuration conf ->

            Set<Dependency> artifactDependencies = conf.allDependencies.findAll { !(it instanceof ProjectDependency) }

            artifactDependencies.each { Dependency dependency ->

                Set<File> jarFiles = conf.files(dependency).findAll { it.file && it.name.endsWith('.jar') }

                jarFiles.each { File file ->

                    Tuple2<String, String> result = findFolderAndPath('bower.json', file)
                    if (!bower) {
                        if (result) {
                            // Bower package found, skip it
                            result = null
                        } else {
                            result = findFolderAndPath('package.json', file)
                        }
                    }

                    if (result) {
                        String packageJsonFolder = result.first
                        String componentRootPackage = result.second

                        File componentRoot = new File(componentsDir, componentRootPackage)
                        if (componentRoot.exists()) {
                            GFileUtils.deleteDirectory(componentRoot)
                        }

                        copyJarToFolder(file, packageJsonFolder, componentRoot)

                        WebJarHelper.LOGGER.info(
                                "Unpacked ${dependency.group}.${dependency.name} into $componentRoot")

                    }
                }
            }
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
                        File f = Paths.get(componentRoot.canonicalPath, filename.split(SLASH)).toFile()
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
