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

import static org.gradle.testkit.runner.TaskOutcome.SKIPPED

import org.gradle.testkit.runner.BuildResult

/**
 * Test for testing VaadinCompile task
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CompileTest extends FunctionalTest {

    void 'compile is not run if Webjars are used'() {
        when:
            BuildResult result = run'vaadinCompile'
        then:
            result.task(':vaadinCompile').outcome == SKIPPED
    }

    void 'cannot set resolver after dependencies have been added'() {
        setup:
            buildFile << '''
                    dependencies {
                        implementation vaadin.core()
                    }
                    vaadinCompile {
                        clientDependencyResolver 'FOO'
                    }
                '''.stripMargin()
        when:
            BuildResult result = runAndFail'jar'
        then:
            result.output.contains 'vaadin.clientDependencyResolver cannot be set after dependencies have been added'
    }

    void 'set client resolver to invalid value'() {
        setup:
            buildFile << '''
                vaadinCompile {
                    clientDependencyResolver 'FOO'
                }
            '''.stripMargin()
        when:
            BuildResult result = runAndFail'jar'
        then:
            result.output.contains '\'FOO\' is not a valid dependency resolver'
    }

}
