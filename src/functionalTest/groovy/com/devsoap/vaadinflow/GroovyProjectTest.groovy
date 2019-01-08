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
import java.nio.file.Paths

/**
 * Tests for Groovy projects
 *
 * @author John Ahlroos
 * @since 1.0
 */
class GroovyProjectTest extends FunctionalTest {

    void 'Create a Groovy project'() {
        setup:
            extraPlugins = ['groovy':'']
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'groovy').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File servletFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}Servlet.groovy").toFile()
            File viewFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}View.groovy").toFile()
            File uiFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}UI.groovy").toFile()
        when:
            BuildResult result = run 'vaadinCreateProject'
        then:
            result.task(':vaadinCreateProject').outcome == SUCCESS
            servletFile.exists()
            viewFile.exists()
            uiFile.exists()
    }

    void 'Groovy project compiles'() {
        setup:
            extraPlugins = ['groovy':'']
            buildFile << '''
                    vaadin.autoconfigure()
                '''.stripIndent()
        when:
            run'vaadinCreateProject'
            run 'vaadinCreateComponent'
            run 'vaadinCreateComposite'
            run 'vaadinCreateWebTemplate'
            run 'vaadinCreateWebComponent', '--dependency', 'bower:PolymerElements/paper-slider:v2.0.5'

            BuildResult result = run'jar'
        then:
            result.task(':jar').outcome == SUCCESS
    }

    void 'Groovy template is converted'() {
        setup:
            extraPlugins = ['groovy':'']

            run'vaadinCreateProject'
            run 'vaadinCreateWebTemplate', '--name', 'groovytemplate'

            File gen = Paths.get(testProjectDir.root.canonicalPath,
                'build', 'webapp-gen', 'frontend', 'templates').toFile()
            File htmlFile = new File(gen, 'groovytemplate.html')
        when:
            run 'vaadinConvertGroovyTemplatesToHtml'
        then:
            htmlFile.exists()
            htmlFile.text.contains("<link rel='import' href='../bower_components/polymer/polymer-element.html'/>")
            htmlFile.text.contains("<dom-module id='groovytemplate'>")
            htmlFile.text.contains("<dom-module id='groovytemplate'>")
    }

    void 'Groovy templates are included in transpilation'() {
        setup:
            extraPlugins = ['groovy':'']

            run 'vaadinCreateProject'
            run 'vaadinCreateWebTemplate', '--name', 'groovytemplate'

            buildFile << '''
                vaadin.productionMode = true

                repositories {
                    vaadin.repositories()
                }

                dependencies {
                  implementation vaadin.bom()
                  implementation vaadin.core()
                  implementation vaadin.servletApi()
                }
            '''.stripIndent()
        when:
            run('vaadinTranspileDependencies')
        then:
            File frontend = Paths.get(buildFile.parentFile.canonicalPath, 'build', 'frontend').toFile()
            File polymerJson = new File(frontend, 'polymer.json')
            polymerJson.text.contains('templates/groovytemplate.html')
    }

    void 'Groovy templates in custom webapp dir'() {
        setup:
            extraPlugins = ['groovy':'']

            buildFile << '''
                vaadinAssembleClient {
                    webappDir 'src'
                }
            '''.stripIndent()

            run 'vaadinCreateProject'
            run 'vaadinCreateWebTemplate', '--name', 'groovytemplate'

            buildFile << '''
                vaadin.productionMode = true

                repositories {
                    vaadin.repositories()
                }

                dependencies {
                  implementation vaadin.bom()
                  implementation vaadin.core()
                  implementation vaadin.servletApi()
                }
            '''.stripIndent()
        when:
            run('vaadinTranspileDependencies')
        then:
            File frontend = Paths.get(buildFile.parentFile.canonicalPath, 'build', 'frontend').toFile()
            File polymerJson = new File(frontend, 'polymer.json')
            polymerJson.text.contains('templates/groovytemplate.html')
    }
}
