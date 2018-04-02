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
            BuildResult result = run '--info', '--stacktrace', 'vaadinInstallClientDependencies'
        then:
            File frontend = Paths.get(buildFile.parentFile.canonicalPath,
                    'src', 'main', 'webapp', 'frontend').toFile()
            File nodeModules = new File(frontend, 'node_modules')
            File sliderComponent = Paths.get(nodeModules.canonicalPath, '@polymer', 'paper-slider').toFile()
            sliderComponent.exists()
    }

    void 'create paper-slider web component'() {
        setup:
        buildFile << """               
                vaadin.autoconfigure()
            """.stripMargin()
            run 'vaadinCreateProject'
        when:
            BuildResult result = run '--info', '--stacktrace', 'vaadinCreateWebComponent',
                    '--dependency', '@polymer/paper-slider:0.0.3'
        then:
            println result.output

            // Validate that the component was created
            File javaSourceDir = Paths.get(buildFile.parentFile.canonicalPath,
                    'src', 'main', 'java').toFile()
            File componentClass = Paths.get(javaSourceDir.canonicalPath,
                    'com', 'example', testProjectDir.root.name.toLowerCase(),
                            'ExampleWebComponent.java').toFile()
            componentClass.exists()
            componentClass.text.contains('@Tag("example-web-component")')
            componentClass.text.contains(
                    '@HtmlImport("frontend://node_modules/@polymer/paper-slider/paper-slider.html")')
            componentClass.text.contains('public class ExampleWebComponent')

            // Validate that the dependency got downloaded and installed
            File frontend = Paths.get(buildFile.parentFile.canonicalPath,
                    'src', 'main', 'webapp', 'frontend').toFile()
            File nodeModules = new File(frontend, 'node_modules')
            File sliderComponent = Paths.get(nodeModules.canonicalPath, '@polymer', 'paper-slider').toFile()
            sliderComponent.exists()
    }

}
