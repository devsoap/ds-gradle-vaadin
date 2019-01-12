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

import com.devsoap.spock.Client
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Paths

/**
 * Test for testing client dependencies added to the project
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Client
class ClientDependenciesTest extends FunctionalTest {

    void 'add paper-slider to project as bower dependency'() {
        setup:
        buildFile << """
                vaadinClientDependencies {
                    bower 'PolymerElements/paper-slider:v2.0.5'
                }

                vaadin.autoconfigure()

            """.stripIndent()
        run 'vaadinCreateProject'
        when:
        BuildResult result = run 'vaadinAssembleClient'
        then:
        result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SUCCESS
        result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SUCCESS
        result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SKIPPED
        result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS
        File frontend = Paths.get(buildFile.parentFile.canonicalPath,
                'build', 'webapp-gen', 'frontend').toFile()
        bowerComponentExists(frontend, 'paper-slider')
    }

    void 'add paper-slider to project as yarn dependency'() {
        setup:
            buildFile << """
                vaadinClientDependencies {
                    yarn '@polymer/paper-slider:0.0.3'
                }

                vaadin.autoconfigure()

            """.stripIndent()
            run 'vaadinCreateProject'
        when:
            BuildResult result = run  'vaadinAssembleClient'
        then:
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SKIPPED
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS
            File frontend = Paths.get(buildFile.parentFile.canonicalPath,
                    'build', 'webapp-gen', 'frontend').toFile()
            yarnComponentExists(frontend, '@polymer/paper-slider')
    }

    void 'add paper-slider web component via task'() {
        setup:
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
            run 'vaadinCreateProject'
        when:
            run 'vaadinCreateWebComponent', '--dependency', 'bower:PolymerElements/paper-slider:v2.0.5'
            BuildResult result = run 'jar'
        then:
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.UP_TO_DATE
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.UP_TO_DATE
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
                'build', 'webapp-gen', 'frontend').toFile()
            bowerComponentExists(frontend, 'paper-slider')
    }

    void 'transpile dependencies in production mode'() {
        setup:
            buildFile << '''
                    vaadin.productionMode = true
                    vaadin.autoconfigure()
                '''.stripIndent()
            run 'vaadinCreateProject'
        when:
            run 'vaadinCreateWebComponent', '--dependency', 'bower:PolymerElements/paper-slider:v2.0.5'
            run('jar')
        then:

            File webappGen = Paths.get(buildFile.parentFile.canonicalPath, 'build', 'webapp-gen').toFile()

            File frontend5 = new File(webappGen, 'frontend-es5')
            frontend5.exists()

            File bundle5 = new File(frontend5, 'vaadin-flow-bundle-manifest.json')
            bundle5.exists()

            File styles5 = new File(frontend5, 'styles')
            styles5.exists()
            File cssFile5 = new File(styles5, testProjectDir.root.name.toLowerCase() + '-theme.css')
            cssFile5.exists()
            File cssFile5HTML = new File(styles5, testProjectDir.root.name.toLowerCase() + '-theme.html')
            cssFile5HTML.exists()

            File frontend6 = new File(webappGen, 'frontend-es6')
            frontend6.exists()

            File bundle6 = new File(frontend6, 'vaadin-flow-bundle-manifest.json')
            bundle6.exists()

            File styles6 = new File(frontend6, 'styles')
            styles6.exists()
            File cssFile6 = new File(styles6, testProjectDir.root.name.toLowerCase() + '-theme.css')
            cssFile6.exists()
            File cssFile6HTML = new File(styles6, testProjectDir.root.name.toLowerCase() + '-theme.html')
            cssFile6HTML.exists()
    }

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

    void 'transpile with manually configured dependencies'() {
        setup:
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
            run 'vaadinCreateProject'
        when:
            BuildResult result = run('build')
        then:
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SUCCESS
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

    void 'exclude specific imports from bundle'() {
        setup:
            buildFile << '''
                   vaadinTranspileDependencies {
                       bundleExcludes = ['https://www.gstatic.com/charts/loader.js']
                   }

                   vaadin.productionMode = true
                   vaadin.autoconfigure()
            '''.stripIndent()

            run 'vaadinCreateProject'
        when:
            BuildResult result = run 'vaadinAssembleClient'
        then:
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS
    }

    void 'assemble from custom webapp directory'() {
        setup:
            buildFile << '''
                   vaadinAssembleClient {
                        webappDir 'src'
                   }

                   vaadin.productionMode = true
                   vaadin.autoconfigure()
            '''.stripIndent()

            run 'vaadinCreateProject'

            File webapp = Paths.get(buildFile.parentFile.canonicalPath, 'src').toFile()
            File templates = Paths.get(webapp.absolutePath, 'frontend', 'templates').toFile()

            File htmlImport = Paths.get(templates.absolutePath, 'hello.html').toFile()
            htmlImport.parentFile.mkdirs()
            htmlImport.createNewFile()
        when:
            BuildResult result = run 'vaadinAssembleClient'
        then:
            result.task(':vaadinInstallYarnDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinInstallBowerDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinTranspileDependencies').outcome == TaskOutcome.SUCCESS
            result.task(':vaadinAssembleClient').outcome == TaskOutcome.SUCCESS

            File frontend = Paths.get(buildFile.parentFile.canonicalPath, 'build', 'frontend').toFile()
            File polymerJson = new File(frontend, 'polymer.json')
            polymerJson.text.contains('templates/hello.html')
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

    private static boolean bowerComponentExists(File frontend, String component) {
        File componentFile = Paths.get(frontend.canonicalPath, 'bower_components', component).toFile()
        File componentHTMLFile = new File(componentFile, "${component}.html")
        componentFile.exists() && componentHTMLFile.exists()
    }

    private static boolean yarnComponentExists(File frontend, String component) {
        File componentFile = new File(Paths.get(frontend.canonicalPath, 'node_modules').toFile(), component)
        File componentHTMLFile = new File(componentFile, "${component.split('/').last()}.html")
        componentFile.exists() && componentHTMLFile.exists()
    }
}
