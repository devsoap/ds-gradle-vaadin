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
package com.devsoap.vaadinflow.extensions

import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.util.RelativePathUtil

import java.nio.file.Paths

/**
 * Extension for configuring the client dependencies
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinClientDependenciesExtension {

    private static final String COLON = ':'
    private static final String LATEST = 'latest'
    private static final String HASH = '#'

    static final String NAME = 'vaadinClientDependencies'

    static final String FRONTEND_BUILD_DIR = 'build/frontend'

    static final String WEBAPP_DIR = 'src/main/webapp'

    static final String FRONTEND_DIR = "$WEBAPP_DIR/frontend"

    private final Map<String, String> yarnDependencies = [:]

    private final Map<String, String> bowerDependencies = [:]

    private final Project project

    private final Property<Boolean> compileFromSources

    private final Property<String> offlineCachePath

    /**
     * Creates the extension
     *
     * @param project
     *      the project to apply the extension to
     */
    VaadinClientDependenciesExtension(Project project) {
        this.project = project
        compileFromSources = project.objects.property(Boolean)
        offlineCachePath = project.objects.property(String)

        File mirrorFolder = Paths.get(project.rootDir.absolutePath,
                '.gradle', 'yarn', 'yarn-offline-mirror').toFile()
        File frontendDir = Paths.get(project.rootDir.absolutePath, 'build', 'frontend').toFile()
        offlineCachePath.set(RelativePathUtil.relativePath(frontendDir, mirrorFolder))
    }

    /**
     * Add a Yarn dependency using the compact notation
     *
     * @param dependencyNotation
     *      the dependency using the "<dependency-name>:<dependency-version>" notation
     */
    void yarn(String dependencyNotation) {
        if (dependencyNotation.contains(COLON)) {
            dependencyNotation.split(COLON).with {
                yarn(it.first(), it.last())
            }
        } else if (dependencyNotation.contains(HASH)) {
            dependencyNotation.split(HASH).with {
                yarn(it.first(), it.last())
            }
        } else {
            yarn(dependencyNotation, LATEST)
        }
    }

    /**
     * Add a Yarn dependency
     *
     * @param dependency
     *      the dependency name
     * @param version
     *      the dependency version
     */
    void yarn(String dependency, String version) {
        yarnDependencies[dependency] = version
    }

    /**
     * Add a Bower dependency using the compact notation
     *
     * @param dependencyNotation
     *      the dependency using the "<dependency-name>:<dependency-version>" notation
     */
    void bower(String dependencyNotation) {
        if (dependencyNotation.contains(COLON)) {
            dependencyNotation.split(COLON).with {
                bower(it.first(), it.last())
            }
        } else if (dependencyNotation.contains(HASH)) {
            dependencyNotation.split(HASH).with {
                bower(it.first(), it.last())
            }
        } else {
            bower(dependencyNotation, LATEST)
        }
    }

    /**
     * Add a Bower dependency
     *
     * @param dependency
     *      the dependency name
     * @param version
     *      the dependency version
     */
    void bower(String dependency, String version) {
        bowerDependencies[dependency] = version
    }

    /**
     * Retrieve the configured Yarn dependencies
     *
     * @return
     *      a map with the dependency name as key and version as value
     */
    Map<String,String> getYarnDependencies() {
        Collections.unmodifiableMap(yarnDependencies)
    }

    /**
     * Retrieve the configured Bower dependencies
     *
     * @return
     *      a map with the dependency name as key and version as value
     */
    Map<String,String> getBowerDependencies() {
        Collections.unmodifiableMap(bowerDependencies)
    }

    /**
     * Should the plugin compile the web components from sources.
     *
     * This property is only used to decide if the webjars should be unpacked and compiled
     * using NPM for legacy browsers. When custom component sources exist in the project
     * then this is always <code>true</code>.
     */
    boolean isCompileFromSources() {
        compileFromSources.getOrElse(false)
    }

    /**
     * Should the plugin compile the web components from sources.
     *
     * This property is only used to decide if the webjars should be unpacked and compiled
     * using NPM for legacy browsers. When custom component sources exist in the project
     * then this is always <code>true</code>.
     */
    void setCompileFromSources(boolean compile) {
        compileFromSources.set(compile)
        if (!project.extensions.getByType(VaadinFlowPluginExtension).productionMode) {
            LOGGER.warning('compileFromSources has no effect without setting vaadin.productionMode to true')
        }
    }

    /**
     * Get the cache path where Yarn should cache offline artifacts
     */
    String getOfflineCachePath() {
        offlineCachePath.get()
    }

    /**
     * Set the cache path where Yarn should cache offline artifacts
     */
    void setOfflineCachePath(String path) {
        offlineCachePath.set(path)
    }

    /**
     * Set the cache path where Yarn should cache offline artifacts
     */
    void setOfflineCachePath(File file) {
        offlineCachePath.set(file.canonicalPath)
    }
}
