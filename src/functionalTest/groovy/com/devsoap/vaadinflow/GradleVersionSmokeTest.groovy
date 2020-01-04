/*
 * Copyright 2018-2020 Devsoap Inc.
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0
 * International Public License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      https://creativecommons.org/licenses/by-nd/4.0/
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
 * Smoke test for Gradle versions
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Smoke
class GradleVersionSmokeTest extends FunctionalTest {

    private static final List<String> VERSIONS = ['5.6', '6.0-rc-2']

    @Unroll
    void 'Test Gradle #version'(String version) {
        setup:
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
            run( { it.withGradleVersion(version) }, 'vaadinCreateProject')
        when:
            BuildResult result = run( {
                it.withGradleVersion(version)
                it.arguments = ['--warning-mode', 'all'] + it.arguments
            }, 'jar')
        then:
            result.task(':jar').outcome == TaskOutcome.SUCCESS
            !result.output.contains('Deprecated Gradle features')

        where:
            version << VERSIONS
    }
}
