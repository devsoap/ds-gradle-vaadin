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
package com.devsoap.vaadinflow

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Base class for functional tests
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FunctionalTest extends Specification {

    static final String PLUGIN_ID = 'com.devsoap.vaadin-flow'

    private static final String DEFAULT_TEST_VAADIN_VERSION = '11.0.0'

    @Rule
    protected TemporaryFolder testProjectDir

    protected File buildFile

    protected File settingsFile

    private long testStart

    protected Map<String, String> extraPlugins

    protected String vaadinVersion

    /**
     * Sets up the test
     */
    protected void setup() {
        extraPlugins = [:]
        vaadinVersion = DEFAULT_TEST_VAADIN_VERSION
        initBuildFile()
        initSettingsFile()
        testStart = System.currentTimeMillis()
        println "Running test in ${testProjectDir.root}"
        println "Using Yarn cache dir $offlineCachePath"
    }

    /**
     * Set additional plugins for the test.
     *
     * @param plugins
     *      the plugins to add besides the Vaadin plugin
     */
    protected void setExtraPlugins(Map<String,String> plugins) {
        extraPlugins = plugins
        buildFile.delete()
        initBuildFile()
    }

    /**
     * Set the Vaadin version used in the test
     *
     * @param version
     *      the default vaadin version to test
     */
    protected void setVaadinVersion(String version) {
        vaadinVersion = version
        buildFile.delete()
        initBuildFile()
    }

    /**
     * Cleans up the test
     */
    protected void cleanup() {
        long ms = System.currentTimeMillis() - testStart
        println "Test took ${TimeUnit.MILLISECONDS.toSeconds(ms)} seconds."
    }

    /**
     * Runs the project
     *
     * @param args
     *      the command line arguments to pass to gradle
     * @return
     *      the result of the build
     */
    protected BuildResult run(ConfigureRunner config = { }, String... args) {
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(['--stacktrace', '--info'] + (args as List))
                .withPluginClasspath()
        config.run(runner)
        println "Running gradle ${runner.arguments.join(' ')}"
        runner.build()
    }

    /**
     * Runs the project and is expected to fail
     *
     * @param args
     *      the command line arguments to pass to gradle
     * @return
     *       the result of the build
     */
    protected BuildResult runAndFail(ConfigureRunner config = { }, String... args) {
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(['--stacktrace', '--info'] + (args as List))
                .withPluginClasspath()
        config.run(runner)
        println "Running gradle ${runner.arguments.join(' ')}"
        runner.buildAndFail()
    }

    private void initBuildFile() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id '$PLUGIN_ID'
                ${extraPlugins.collect { it.value ? "id '$it.key' version '$it.value'" : "id '$it.key'" }.join('\n')}
            }

            ${ vaadinVersion ? "vaadin.version = '$vaadinVersion'" : '' }

            vaadinClientDependencies {
                offlineCachePath = "$offlineCachePath"
            }

        """.stripIndent()
    }

    private void initSettingsFile() {
        settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << '''
            enableFeaturePreview('IMPROVED_POM_SUPPORT')
        '''.stripIndent()
    }

    private String getOfflineCachePath() {
        System.getProperty('yarn.cache.dir', Paths.get(testProjectDir.root.canonicalPath, 'yarn-cache')
                .toFile().canonicalPath)
    }

    /**
     * Interface representing a GradleRunner configuration closure
     */
    @FunctionalInterface
    interface ConfigureRunner {
        void run(GradleRunner runner)
    }

}
