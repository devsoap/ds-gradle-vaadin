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

import com.devsoap.vaadinflow.util.Versions
import groovy.util.logging.Log
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.provider.Property

/**
 * The main plugin extension
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinFlowPluginExtension {

    static final String NAME = VAADIN
    static final String GROUP = 'com.vaadin'
    static final String VAADIN = 'vaadin'
    public static final String COLON = ':'

    private final Property<String> version
    private final Property<Boolean> productionMode

    private final DependencyHandler dependencyHandler
    private final RepositoryHandler repositoryHandler
    private final Project project

    private boolean dependencyApplied = false

    VaadinFlowPluginExtension(Project project) {
        this.project = project
        dependencyHandler = project.dependencies
        repositoryHandler = project.repositories
        version = project.objects.property(String)
        productionMode = project.objects.property(Boolean)
    }

    /**
     * The vaadin version to use. By default latest Vaadin 7 version.
     */
    String getVersion() {
        version.orNull
    }

    /**
     * The vaadin version to use. By default latest Vaadin 7 version.
     */
    void setVersion(String version) {
        if (dependencyApplied) {
            throw new GradleException('Cannot set vaadin.version after dependencies have been added')
        }
        this.version.set(version)
    }

    /**
     * Should the plugin support legacy browsers by transpiling the sources to ES5
     */
    boolean isProductionMode() {
        productionMode.getOrElse(false)
    }

    /**
     * Should the plugin support legacy browsers by transpiling the sources to ES5
     */
    void setProductionMode(boolean enabled) {
        productionMode.set(enabled)
        project.extensions.getByType(VaadinClientDependenciesExtension).compileFromSources = enabled
    }

    /**
     * Autoconfigures repositories and dependencies
     */
    void autoconfigure() {
        repositoryHandler.jcenter()
        repositoryHandler.addAll(repositories())
        dependencyHandler.add('implementation', platform())
        // Add to compile as well for backwards compatibility with 3rd party plugins
        dependencyHandler.add('compile', platform())
        dependencyHandler.add('compileOnly', servletApi())
        dependencyHandler.add('runtime', slf4j())
    }

    /**
     * Include Vaadin snapshots in the build
     *
     * Usage:
     *    repositories {
     *        vaadin.snapshots()
     *    }
     *
     */
    ArtifactRepository snapshots() {
        repository('Vaadin Snapshots', 'http://oss.sonatype.org/content/repositories/vaadin-snapshots')
    }

    /**
     * Include Vaadin pre-releases in the build
     *
     * Usage:
     *    repositories {
     *        vaadin.prereleases()
     *    }
     *
     */
    ArtifactRepository prereleases() {
        repository('Vaadin Pre-releases', 'https://maven.vaadin.com/vaadin-prereleases')
    }

    /**
     * Include the Vaadin addons repository
     *
     * Usage:
     *    repositories {
     *        vaadin.addons()
     *    }
     */
    ArtifactRepository addons() {
        repository('Vaadin Addons', 'http://maven.vaadin.com/vaadin-addons')
    }

    /**
     * Add all stable Vaadin repositories
     */
    Collection<ArtifactRepository> repositories() {
        [prereleases(), addons() ]
    }

    /**
     * Adds the Vaadin platform dependency which contains all dependencies both commercial and open source
     */
    Dependency platform() {
        dependency(VAADIN)
    }

    /**
     * Add the core dependency with the open source components
     */
    Dependency core() {
        dependency('core')
    }

    /**
     * Add the BOM that provides the versions of the dependencies
     */
    Dependency bom() {
        dependency('bom')
    }

    /**
     * Returns the servlet API dependency
     */
    Dependency servletApi() {
        String version = Versions.rawVersion('servlet.version')
        dependencyHandler.create("javax.servlet:javax.servlet-api:$version")
    }

    /**
     * Returns the SLF4J simple implementation
     * @return
     */
    Dependency slf4j() {
        String version = Versions.rawVersion('slf4j.simple.version')
        dependencyHandler.create("org.slf4j:slf4j-simple:$version")
    }

    /**
     * Get a Vaadin dependency
     *
     * Usage:
     *    dependencies {
     *        implementation vaadin.dependency('core')
     *    }
     * @param name
     *     the name of the dependency.
     * @return
     *      the dependency
     */
    Dependency dependency(String name, boolean useVersion=true) {
        List<String> dependency = [GROUP]

        if (name == VAADIN) {
           dependency << name
        } else {
           dependency << "vaadin-${name}"
        }

        if (useVersion) {
            dependency << version.getOrElse(Versions.rawVersion('vaadin.default.version'))
        }

        dependencyApplied = true
        dependencyHandler.create(dependency.join(COLON))
    }

    /**
     * Define a Maven repository
     *
     * @param name
     *      the name for the repository
     * @param url
     *      the url of the repository
     */
    private ArtifactRepository repository(String name, String url) {
        repositoryHandler.maven { repository ->
            repository.name = name
            repository.url = url
        }
    }
}
