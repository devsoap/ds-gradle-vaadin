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
            result.output.contains("Your gradle version (${version}.0) is too old.")
        where:
            version = '4.10'
    }

    @Unroll
    void '#version is a valid gradle version'(String version) {
        when:
            BuildResult result = run({ it.withGradleVersion(version) }, 'jar')
        then:
            result.task(':jar').outcome == SUCCESS
        where:
            version = '5.6'
    }

    void 'server dependencies are applied to project'() {
        setup:
            buildFile << '''
                repositories {
                    mavenCentral()
                    vaadin.prereleases()
                }
                dependencies {
                    implementation vaadin.bom()
                    implementation vaadin.core()
                    implementation vaadin.lumoTheme()
                    compileOnly vaadin.servletApi()
                }
            '''.stripIndent()
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
                        implementation vaadin.lumoTheme()
                        implementation vaadin.dependency('ordered-layout-flow', false)
                        implementation vaadin.dependency('button-flow', false)
                        implementation vaadin.dependency('core', false)
                        compileOnly vaadin.servletApi()
                    }
                '''.stripIndent()
             run  'vaadinCreateProject'
        when:
            BuildResult result = run'jar'
        then:
            result.task(':jar').outcome == SUCCESS
    }

    void 'all repositories are added to project'() {
        setup:
        buildFile << '''
                repositories {
                    mavenCentral()
                    vaadin.repositories()
                }
                dependencies {
                    implementation vaadin.core()
                    compileOnly vaadin.servletApi()
                }
            '''.stripIndent()
        when:
            BuildResult result = run('dependencyInsight', '--dependency', 'com.vaadin:flow-server')
        then:
            result.task(':dependencyInsight').outcome == SUCCESS
            result.output.contains("com.vaadin:vaadin-core:$vaadinVersion")
    }

    void 'autoconfigure project'() {
        setup:
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
            run  'vaadinCreateProject'
        when:
            BuildResult result = run'jar'
        then:
            result.task(':jar').outcome == SUCCESS
    }

    void 'no vaadin version is set, fall back to latest version'() {
        setup:
            vaadinVersion = null
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
        when:
            BuildResult result = run'jar'
        then:
            result.task(':jar').outcome == SUCCESS
            result.output.contains('vaadin.version is not set, falling back to latest Vaadin version')
    }

    void 'not able to set version after dependency has been added'() {
        setup:
            vaadinVersion = null
            buildFile << '''
                dependencies {
                    implementation vaadin.core()
                }
                vaadin {
                    version 'x.x.x'
                }
            '''.stripIndent()
        when:
             BuildResult result = runAndFail'jar'
        then:
            result.output.contains 'Cannot set vaadin.version after dependencies have been added'
    }

    void 'statistics are not submitted by default'() {
        setup:
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
        when:
            BuildResult result = run('dependencyInsight', '--dependency',
                    'org.webjars.bowergithub.vaadin:vaadin-usage-statistics')
        then:
            result.task(':dependencyInsight').outcome == SUCCESS
            result.output.contains('org.webjars.bowergithub.vaadin:vaadin-usage-statistics')
            result.output.contains('-> 1.0.0-optout')
            result.output.contains('Allow Vaadin to gather usage statistics')
    }

    void 'statistics are submitted when turned on'() {
        setup:
            buildFile << '''
                    vaadin.submitStatistics = true
                    vaadin.autoconfigure()
                '''.stripIndent()
        when:
            BuildResult result = run('dependencyInsight', '--dependency',
                    'org.webjars.bowergithub.vaadin:vaadin-usage-statistics')
        then:
            result.task(':dependencyInsight').outcome == SUCCESS
            result.output.contains('org.webjars.bowergithub.vaadin:vaadin-usage-statistics')
            !result.output.contains('-> 1.0.0-optout')
            !result.output.contains('Allow Vaadin to gather usage statistics')
    }

    void 'statistics message turned off when set to false'() {
        setup:
            buildFile << '''
                    vaadin.submitStatistics = false
                    vaadin.autoconfigure()
                '''.stripIndent()
        when:
            BuildResult result = run('dependencyInsight', '--dependency',
                'org.webjars.bowergithub.vaadin:vaadin-usage-statistics')
        then:
            !result.output.contains('Allow Vaadin to gather usage statistics')
    }

    void 'fail if un-versioned dependency is applied before BOM'() {
        setup:
            buildFile << '''
                repositories {
                    mavenCentral()
                }
                dependencies {
                    implementation vaadin.dependency('core', false)
                    implementation vaadin.bom()
                }
            '''.stripIndent()
        when:
            BuildResult result = runAndFail('jar')
        then:
            result.output.contains('Cannot use un-versioned dependencies without using a BOM')
    }

    void 'warn if using both BOM and defined version'() {
        setup:
            buildFile << '''
                repositories {
                    mavenCentral()
                }
                dependencies {
                    implementation vaadin.bom()
                    implementation vaadin.dependency('core', true)
                }
            '''.stripIndent()
        when:
            BuildResult result = run('jar')
        then:
            result.output.contains('Forcing a Vaadin version while also using the BOM is not recommended')
    }

    void 'version check test'() {
        setup:
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
        when:
            BuildResult result = run('vaadinPluginVersionCheck')
        then:
            result.task(':vaadinPluginVersionCheck').outcome == SUCCESS
            result.output.contains('You are using the latest plugin. Excellent!')
    }
}
