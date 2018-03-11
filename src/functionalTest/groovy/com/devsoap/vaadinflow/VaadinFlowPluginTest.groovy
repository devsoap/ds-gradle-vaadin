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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

/**
 * Test the basic vaadin plugin features defined in the VaadinFlowPlugin
 *
 * @author John Ahlroos
 * @since 1.0
 */
class VaadinFlowPluginTest extends FunctionalTest {

    void 'can successfully be applied to project'() {
        when:
            BuildResult result = run'jar'
        then:
            result.task(':jar').outcome == SUCCESS
    }

    @Unroll
    void '#version is an invalid gradle version'(String version) {
        when:
            BuildResult result = runAndFail({ it.withGradleVersion(version) }, '--info', 'jar')
        then:
            result.output.contains("Your gradle version ($version.0) is too old")
        where:
            version = '4.5'
    }

    @Unroll
    void '#version is a valid gradle version'(String version) {
        when:
            BuildResult result = run({ it.withGradleVersion(version) }, 'jar')
        then:
            result.task(':jar').outcome == SUCCESS
        where:
            version = '4.6'
    }

    void 'server dependencies are applied to project'() {
        setup:
            buildFile << """
                repositories {
                    mavenCentral()
                    vaadin.prereleases()
                }
                dependencies {
                    implementation vaadin.dependency('core')
                }
            """.stripMargin()
            run  'vaadinCreateProject'
        when:
            BuildResult result = run'jar'
        then:
            result.task(':jar').outcome == SUCCESS
    }
}
