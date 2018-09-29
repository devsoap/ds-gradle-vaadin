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
import org.gradle.api.plugins.ExtraPropertiesExtension
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

    private static final String COLON = ':'
    private static final String COMPILE = 'compile'
    private static final String BOM_ARTIFACT_NAME = 'bom'
    public static final String VAADIN_VERSION_PROPERTY = 'vaadinVersion'

    private final Property<String> version
    private final Property<Boolean> productionMode
    private final Property<Boolean> submitStatistics

    private final DependencyHandler dependencyHandler
    private final RepositoryHandler repositoryHandler
    private final Project project

    private boolean dependencyApplied = false
    private boolean statisticsApplied = false
    private boolean bomApplied = false

    VaadinFlowPluginExtension(Project project) {
        this.project = project
        dependencyHandler = project.dependencies
        repositoryHandler = project.repositories
        version = project.objects.property(String)
        productionMode = project.objects.property(Boolean)
        submitStatistics = project.objects.property(Boolean)
    }

    /**
     * The vaadin version to use. By default latest Vaadin version.
     */
    String getVersion() {
        ExtraPropertiesExtension ext = project.extensions.extraProperties
        version.getOrElse(                                              // Use vaadin.version if set
            ext.properties.containsKey(VAADIN_VERSION_PROPERTY) ?       // else see if ext.vaadinVersion is set
            ext.get(VAADIN_VERSION_PROPERTY).toString() :
            Versions.rawVersion('vaadin.default.version')          // else fallback to default vaadin version
        )
    }

    /**
     * The vaadin version to use. By default latest Vaadin version.
     */
    void setVersion(String version) {
        if (dependencyApplied) {
            throw new GradleException('Cannot set vaadin.version after dependencies have been added')
        }
        this.version.set(version)
    }

    /**
     * Has the version been set manually
     */
    boolean isVersionSet() {
        version.isPresent() || project.ext.properties.containsKey(VAADIN_VERSION_PROPERTY)
    }

    /**
     * Should the plugin support legacy browsers by transpiling the sources to ES5
     */
    boolean isProductionMode() {
        productionMode.getOrElse(Boolean.parseBoolean(System.getProperty('vaadin.productionMode',
                Boolean.FALSE.toString())))
    }

    /**
     * Should the plugin support legacy browsers by transpiling the sources to ES5
     */
    void setProductionMode(boolean enabled) {
        productionMode.set(enabled)
        project.extensions.getByType(VaadinClientDependenciesExtension).compileFromSources = enabled
    }

    /**
     * Should the plugin allow submitting statistics information to Vaadin Ltd.
     */
    boolean isSubmitStatistics() {
        submitStatistics.getOrElse(false)
    }

    /**
     * Has the submit statistics property not been set
     */
    boolean isSubmitStatisticsUnset() {
        !statisticsApplied
    }

    /**
     * Should the plugin allow submitting statistics information to Vaadin Ltd.
     */
    void setSubmitStatistics(boolean enabled) {
        statisticsApplied = true
        submitStatistics.set(enabled)
    }

    /**
     * Autoconfigures repositories and dependencies
     */
    void autoconfigure() {
        repositoryHandler.jcenter()
        repositoryHandler.add(addons())
        repositoryHandler.add(snapshots())
        dependencyHandler.add(COMPILE, bom())
        dependencyHandler.add(COMPILE, platform())

        dependencyHandler.add('compileOnly', servletApi())

        DependencyHandler dh = dependencyHandler // Because Groovy bug hiding this private field from closure

        project.plugins.withId('groovy') {
            dh.add(COMPILE, groovy())
        }

        project.plugins.withId('org.springframework.boot') {
            dh.add(COMPILE, springBoot())
        }

        project.plugins.withId('org.jetbrains.kotlin.jvm') {
            dh.add(COMPILE, 'org.jetbrains.kotlin:kotlin-stdlib')
            dh.add(COMPILE, 'org.jetbrains.kotlin:kotlin-reflect')
        }
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
        [addons(), prereleases(), snapshots()]
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
     * Add the the Spring Boot dependency
     */
    Dependency springBoot() {
        dependency('spring-boot-starter')
    }

    /**
     * Add the BOM that provides the versions of the dependencies
     */
    Dependency bom() {
        bomApplied = true
        dependency(BOM_ARTIFACT_NAME, true)
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
     */
    Dependency slf4j() {
        String version = Versions.rawVersion('slf4j.simple.version')
        dependencyHandler.create("org.slf4j:slf4j-simple:$version")
    }

    /**
     * Returns the opt-out statistics module
     */
    Dependency disableStatistics() {
        String version = Versions.rawVersion('vaadin.statistics.version')
        dependencyHandler.create("org.webjars.bowergithub.vaadin:vaadin-usage-statistics:$version")
    }

    /**
     * Returns a compatible Groovy version
     */
    Dependency groovy() {
        String version = Versions.rawVersion('groovy.version')
        dependencyHandler.create("org.codehaus.groovy:groovy-all:$version")
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
    Dependency dependency(String name, boolean useVersion=!bomApplied) {
        List<String> dependency = [GROUP]

        if (name == VAADIN) {
           dependency << name
        } else {
           dependency << "vaadin-${name}"
        }

        if (useVersion) {
            if (name != BOM_ARTIFACT_NAME && bomApplied) {
                LOGGER.warning('Forcing a Vaadin version while also using the BOM is not recommended')
            }
            dependency << getVersion()
        } else if (!bomApplied) {
            throw new GradleException('Cannot use un-versioned dependencies without using a BOM. Please apply the ' +
                    'BOM before adding un-versioned dependencies.')
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
