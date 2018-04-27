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
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Paths

/**
 * Test for testing client dependencies added to the project
 *
 * @author John Ahlroos
 * @since 1.0
 */
class ClientDependenciesTest extends FunctionalTest {

    void 'add paper-slider to project as yarn dependency'() {
        setup:
            buildFile << """
                vaadinClientDependencies {
                    yarn '@polymer/paper-slider:0.0.3'
                }

                vaadin.autoconfigure()

            """.stripMargin()
            run 'vaadinCreateProject'
        when:
            BuildResult result = run  'vaadinAssembleClient'
        then:
            result.task(':vaadinInstallNpmDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS
            File frontend = Paths.get(buildFile.parentFile.canonicalPath,
                    'src', 'main', 'webapp', 'frontend').toFile()
            bowerComponentExists(frontend, 'paper-slider')
    }

    void 'add paper-slider to project as bower dependency'() {
        setup:
            buildFile << """
                vaadinClientDependencies {
                    bower 'PolymerElements/paper-slider'
                }

                vaadin.autoconfigure()

            """.stripMargin()
             run 'vaadinCreateProject'
        when:
            BuildResult result = run 'vaadinAssembleClient'
        then:
            result.task(':vaadinInstallNpmDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS
            File frontend = Paths.get(buildFile.parentFile.canonicalPath,
                    'src', 'main', 'webapp', 'frontend').toFile()
            bowerComponentExists(frontend, 'paper-slider')
    }

    void 'add paper-slider web component via task'() {
        setup:
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripMargin()
            run 'vaadinCreateProject'
        when:
            run 'vaadinCreateWebComponent', '--dependency', 'bower:PolymerElements/paper-slider'
            BuildResult result = run 'jar'
        then:
            result.task(':vaadinInstallNpmDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS

            // Validate that the component was created
            File javaSourceDir = Paths.get(buildFile.parentFile.canonicalPath,
                    'src', 'main', 'java').toFile()
            File componentClass = Paths.get(javaSourceDir.canonicalPath,
                    'com', 'example', testProjectDir.root.name.toLowerCase(),
                            'ExampleWebComponent.java').toFile()
            componentClass.exists()
            componentClass.text.contains('@Tag("example-web-component")')
            componentClass.text.contains(
                    '@HtmlImport("frontend://bower_components/paper-slider/paper-slider.html")')
            componentClass.text.contains('public class ExampleWebComponent')

            // Validate that the dependency got downloaded and installed
            File frontend = Paths.get(buildFile.parentFile.canonicalPath,
                    'src', 'main', 'webapp', 'frontend').toFile()
            bowerComponentExists(frontend, 'paper-slider')
    }

    void 'transpile dependencies in production mode'() {
        setup:
            buildFile << '''
                    vaadin.productionMode = true
                    vaadin.autoconfigure()
                '''.stripMargin()
            run 'vaadinCreateProject'
        when:
            run 'vaadinCreateWebComponent', '--dependency', 'bower:PolymerElements/paper-slider'
            BuildResult result = run('jar')
        then:
            result.task(':vaadinInstallNpmDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS

            File webapp = Paths.get(buildFile.parentFile.canonicalPath, 'src', 'main', 'webapp').toFile()

            File frontend5 = new File(webapp, 'frontend-es5')
            frontend5.exists()
            bowerComponentExists(frontend5, 'paper-slider')
            bowerComponentExists(frontend5, 'vaadin-button')

            File frontend6 = new File(webapp, 'frontend-es6')
            frontend6.exists()
            bowerComponentExists(frontend6, 'paper-slider')
            bowerComponentExists(frontend6, 'vaadin-button')

            File frontend = new File(webapp, 'frontend')
            frontend.exists()
            File cssFile = new File(frontend, testProjectDir.root.name.toLowerCase() + '-theme.css')
            cssFile.exists()
            !bowerComponentExists(frontend, 'paper-slider')
            !bowerComponentExists(frontend, 'vaadin-button')
    }

    void 'no transpile when no client dependencies'() {
        setup:
        buildFile << '''
                    vaadin.productionMode = true
                    vaadin.autoconfigure()
                '''.stripMargin()
            run 'vaadinCreateProject'
        when:
            BuildResult result = run('jar')
        then:
            result.task(':vaadinInstallNpmDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SKIPPED

            File webapp = Paths.get(buildFile.parentFile.canonicalPath, 'src', 'main', 'webapp').toFile()

            File frontend5 = new File(webapp, 'frontend-es5')
            !frontend5.exists()

            File frontend6 = new File(webapp, 'frontend-es6')
            !frontend6.exists()

            File frontend = new File(webapp, 'frontend')
            frontend.exists()
            frontend.listFiles().length == 1 // Theme file remains here
    }

    void 'force transpile when no client dependencies'() {
        setup:
            buildFile << '''
                        vaadin.productionMode = true
                        vaadinClientDependencies.compileFromSources = true
                        vaadin.autoconfigure()
                    '''.stripMargin()
            run 'vaadinCreateProject'
        when:
            BuildResult result = run('jar')
        then:
            result.task(':vaadinInstallNpmDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS

            File webapp = Paths.get(buildFile.parentFile.canonicalPath, 'src', 'main', 'webapp').toFile()

            File frontend5 = new File(webapp, 'frontend-es5')
            frontend5.exists()
            bowerComponentExists(frontend5, 'vaadin-button')

            File frontend6 = new File(webapp, 'frontend-es6')
            frontend6.exists()
            bowerComponentExists(frontend6, 'vaadin-button')

            File frontend = new File(webapp, 'frontend')
            frontend.exists()
            !bowerComponentExists(frontend, 'vaadin-button')
    }

    private static boolean bowerComponentExists(File frontend, String component) {
        File componentFile = Paths.get(frontend.canonicalPath, 'bower_components', component).toFile()
        File componentHTMLFile = new File(componentFile, "${component}.html")
        componentFile.exists() && componentHTMLFile.exists()
    }
}
