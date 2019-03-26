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
package com.devsoap.vaadinflow

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Tests creation of Vaadin multi-module projects
 *
 * @author John Ahlroos
 * @since 1.0
 */
class MultimoduleFunctionalTest extends Specification {

    static final String PLUGIN_ID = 'com.devsoap.vaadin-flow'

    static final String TEST_VAADIN_VERSION = '10.0.4'

    @Rule
    protected TemporaryFolder rootProjectDir
    protected File libraryProject
    protected File vaadinProject

    protected File buildFile
    protected File libraryProjectBuildFile
    protected File vaadinProjectBuildFile

    protected File settingsFile

    private long testStart

    protected String getOfflineCachePath() {
        System.getProperty('yarn.cache.dir',
                Paths.get(rootProjectDir.root.canonicalPath, 'yarn-cache').toFile().canonicalPath)
    }

    protected void setup() {
        libraryProject = new File(rootProjectDir.root, 'libraryProject')
        libraryProject.mkdirs()

        vaadinProject = new File(rootProjectDir.root, 'vaadinProject')
        vaadinProject.mkdirs()

        buildFile = rootProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id '$PLUGIN_ID' apply false
            }
        """.stripIndent()

        libraryProjectBuildFile = new File(libraryProject, 'build.gradle')
        libraryProjectBuildFile << """
            apply plugin: 'java'
        """.stripIndent()

        vaadinProjectBuildFile = new File(vaadinProject, 'build.gradle')
        vaadinProjectBuildFile << """
            apply plugin: '$PLUGIN_ID'

            vaadinClientDependencies {
                offlineCachePath = "$offlineCachePath"
            }

            dependencies {
                implementation project(':libraryProject')
            }

        """.stripIndent()

        settingsFile = rootProjectDir.newFile('settings.gradle')
        settingsFile << '''
            include 'libraryProject', 'vaadinProject'
        '''.stripIndent()

        testStart = System.currentTimeMillis()
        println "Running test in ${rootProjectDir.root}"
        println "Using Yarn cache dir $offlineCachePath"
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
    protected BuildResult run(File projectDir=rootProjectDir.root, ConfigureRunner config = { }, String... args) {
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(projectDir)
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
    protected BuildResult runAndFail(File projectDir=rootProjectDir.root,
                                     ConfigureRunner config = { }, String... args) {
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(['--stacktrace', '--info'] + (args as List))
                .withPluginClasspath()
        config.run(runner)
        println "Running gradle ${runner.arguments.join(' ')}"
        runner.buildAndFail()
    }

    /**
     * Interface representing a GradleRunner configuration closure
     */
    @FunctionalInterface
    interface ConfigureRunner {
        void run(GradleRunner runner)
    }
}
