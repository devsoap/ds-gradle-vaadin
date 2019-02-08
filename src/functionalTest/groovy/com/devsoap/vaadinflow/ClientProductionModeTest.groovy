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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Paths

/**
 * Test for testing production mode behavior
 *
 * @author John Ahlroos
 * @since 1.0
 */
class ClientProductionModeTest extends FunctionalTest {

    void 'transpile when no client dependencies in production mode'() {
        setup:
            buildFile << '''
                vaadin.productionMode = true
                vaadin.autoconfigure()
            '''.stripIndent()
            run 'vaadinCreateProject'
        when:
            BuildResult result = run('jar')
        then:
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS

            File webappGen = Paths.get(buildFile.parentFile.canonicalPath, 'build', 'webapp-gen').toFile()
            File webapp = Paths.get(buildFile.parentFile.canonicalPath, 'src', 'main', 'webapp').toFile()

            File frontend5 = new File(webappGen, 'frontend-es5')
            frontend5.exists()

            File frontend6 = new File(webappGen, 'frontend-es6')
            frontend6.exists()

            File frontend = new File(webapp, 'frontend')
            frontend.exists()

            File styles = new File(frontend, 'styles')
            styles.exists()

            File cssFile = new File(styles, testProjectDir.root.name.toLowerCase() + '-theme.css')
            cssFile.exists()
    }

    void 'default yarn offline mirror is in project folder'() {
        setup:
            buildFile.text = """
                plugins {
                    id '$PLUGIN_ID'
                }

                vaadinClientDependencies {
                    yarn '@polymer/paper-slider:0.0.3'
                }

                vaadin.autoconfigure()
            """.stripIndent()
        when:
            run '--info', 'vaadinInstallYarnDependencies'
            File mirror = Paths.get(testProjectDir.root.canonicalPath, '.gradle', 'yarn',
                    'yarn-offline-mirror').toFile()
        then:
            mirror.exists()
            mirror.list().length > 0
    }

    void 'template subfolders are transpiled'() {
        setup:
            run 'vaadinCreateProject'
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

            File webapp = Paths.get(buildFile.parentFile.canonicalPath, 'src', 'main', 'webapp').toFile()
            File templates = Paths.get(webapp.absolutePath, 'frontend', 'templates').toFile()

            File htmlImport = Paths.get(templates.absolutePath, 'hello.html').toFile()
            htmlImport.parentFile.mkdirs()
            htmlImport.createNewFile()

            File deepHTMLImport = Paths.get(templates.absolutePath, 'foo', 'bar', 'baz.html').toFile()
            deepHTMLImport.parentFile.mkdirs()
            deepHTMLImport.createNewFile()
        when:
            run('vaadinTranspileDependencies')
        then:
            File frontend = Paths.get(buildFile.parentFile.canonicalPath, 'build', 'frontend').toFile()
            File polymerJson = new File(frontend, 'polymer.json')
            polymerJson.text.contains('templates/foo/bar/baz.html')
            polymerJson.text.contains('templates/hello.html')
    }

    void 'inter-template import paths are validated correctly'() {
        setup:
            run 'vaadinCreateProject'
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

            File webapp = Paths.get(buildFile.parentFile.canonicalPath, 'src', 'main', 'webapp').toFile()
            File templates = Paths.get(webapp.absolutePath, 'frontend', 'templates').toFile()

            File viewsFolder = new File(templates, 'views')
            viewsFolder.mkdirs()

            File componentsFolder = new File(templates, 'components')
            componentsFolder.mkdirs()

            File componentTemplate = new File(componentsFolder, 'component.html')
            componentTemplate.text = '''
                    <link rel="import" href="../../bower_components/polymer/polymer-element.html">
                '''.stripIndent()

            File viewTemplate = new File(viewsFolder, 'view.html')
            viewTemplate.text = """
                    <link rel="import" href="../../bower_components/polymer/polymer-element.html">
                    <link rel="import" href="../components/$componentTemplate.name">
                """.stripIndent()

            File rootTemplate = new File(templates, 'root.html')
            rootTemplate.text = """
                    <link rel="import" href="../bower_components/polymer/polymer-element.html">
                    <link rel="import" href="./components/$componentTemplate.name">
                    <link rel="import" href="./views/$viewTemplate.name">
                """.stripIndent()
        when:
            BuildResult result = run('vaadinTranspileDependencies')
        then:
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SUCCESS
    }

    void 'bundle caches are cleaned up'() {
        setup:
            buildFile << '''
                   vaadin.productionMode = true
                   vaadin.autoconfigure()
            '''.stripIndent()

            run 'vaadinCreateProject'
            run 'vaadinAssembleClient'
            buildFile << '''
                vaadinClientDependencies {
                    yarn '@polymer/paper-slider:0.0.3\'
                }
            '''.stripIndent()
        when:
            run 'vaadinAssembleClient'
        then:
            File frontend = Paths.get(buildFile.parentFile.canonicalPath, 'build', 'frontend').toFile()
            File[] files = frontend.listFiles({ dir, name -> name.endsWith '.cache.html' } as FilenameFilter)
            files.size() == 1
    }
}
