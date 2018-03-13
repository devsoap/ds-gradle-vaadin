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
        setup:
            settingsFile.text = ''
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

    @Unroll
    void 'use vaadin version #version'(String version) {
        setup:
            buildFile << """
                vaadin {
                    version '$version'
                }
                repositories {
                    mavenCentral()
                    vaadin.prereleases()
                }
                dependencies {
                    implementation vaadin.core()
                }
            """.stripMargin()
        when:
            BuildResult result = run('dependencyInsight', '--dependency', 'com.vaadin:flow-server')
        then:
            result.task(':dependencyInsight').outcome == SUCCESS
            result.output.contains("com.vaadin:vaadin-core:$version")
        where:
            version = '10.0.0.beta1'
    }

    void 'server dependencies are applied to project'() {
        setup:
            buildFile << '''
                repositories {
                    mavenCentral()
                    vaadin.prereleases()
                }
                dependencies {
                    implementation vaadin.core()
                }
            '''.stripMargin()
            run  'vaadinCreateProject'
        when:
            BuildResult result = run'jar'
        then:
            result.task(':jar').outcome == SUCCESS
    }

    void 'individual dependencies are applied to project using BOM versions'() {
        setup:
            buildFile << '''
                    repositories {
                        mavenCentral()
                        vaadin.prereleases()
                    }
                    dependencies {
                        implementation vaadin.bom()
                        implementation vaadin.dependency('lumo-theme', false)
                        implementation vaadin.dependency('ordered-layout-flow', false)
                        implementation vaadin.dependency('button-flow', false)
                    }
                '''.stripMargin()
             run  'vaadinCreateProject'
        when:
            BuildResult result = run'jar'
        then:
            result.task(':jar').outcome == SUCCESS
    }

    void 'all repositories are added to project'() {
        setup:
        buildFile << '''
                vaadin {
                    version '10.0.0.beta1'
                }
                repositories {
                    mavenCentral()
                    vaadin.repositories()
                }
                dependencies {
                    implementation vaadin.core()
                }
            '''.stripMargin()
        when:
            BuildResult result = run('dependencyInsight', '--dependency', 'com.vaadin:flow-server')
        then:
            result.task(':dependencyInsight').outcome == SUCCESS
            result.output.contains('com.vaadin:vaadin-core:10.0.0.beta1')
    }
}
