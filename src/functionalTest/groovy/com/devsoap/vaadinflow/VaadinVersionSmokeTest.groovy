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

import com.devsoap.spock.Smoke
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

/**
 * Smoke tests for supported Vaadin versions
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Smoke
class VaadinVersionSmokeTest extends FunctionalTest {

    private static final List<String> VERSIONS = ['10.0.4', '11.0.0', '12.0.0', '13.0.0.alpha2']

    @Unroll
    void 'Test development mode with Vaadin #version'(String version) {
        setup:
            vaadinVersion = version
            buildFile << '''
                    repositories {
                        vaadin.prereleases()
                    }
                    vaadin.autoconfigure()
                '''.stripIndent()
            run 'vaadinCreateProject'
        when:
            BuildResult result = run 'jar'
            BuildResult depInsightResult = run('dependencyInsight', '--dependency', 'com.vaadin:flow-server')
        then:
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SKIPPED
            result.task(':jar').outcome == TaskOutcome.SUCCESS
            depInsightResult.task(':dependencyInsight').outcome == TaskOutcome.SUCCESS
            depInsightResult.output.contains("com.vaadin:vaadin-core:$version")
        where:
            version << VERSIONS
    }

    @Unroll
    void 'Test production mode with Vaadin #version'(String version) {
        setup:
            vaadinVersion = version
            buildFile << '''
                    repositories {
                        vaadin.prereleases()
                    }
                    vaadin.productionMode = true
                    vaadin.autoconfigure()
                '''.stripIndent()
            run 'vaadinCreateProject'
        when:
            BuildResult result = run 'vaadinAssembleClient'
        then:
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS
        where:
            version << VERSIONS
    }

}
