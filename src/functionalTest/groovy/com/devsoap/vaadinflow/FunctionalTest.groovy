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

/**
 * Base class for functional tests
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FunctionalTest extends Specification {

    private static final String PLUGIN_ID = 'com.devsoap.vaadin-flow'

    @Rule
    protected TemporaryFolder testProjectDir

    protected File buildFile

    protected File settingsFile

    /**
     * Sets up the test
     */
    protected void setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id '$PLUGIN_ID'
            }
        """.stripMargin()

        settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << '''
            enableFeaturePreview('IMPROVED_POM_SUPPORT')
        '''.stripMargin()
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
                .withArguments(args)
                .withPluginClasspath()
        config.run(runner)
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
                .withArguments(args)
                .withPluginClasspath()
        config.run(runner)
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
