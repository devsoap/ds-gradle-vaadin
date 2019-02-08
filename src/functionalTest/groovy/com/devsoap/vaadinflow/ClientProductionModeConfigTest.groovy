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
 * Test for testing production mode custom configurations
 *
 * @author John Ahlroos
 * @since 1.0
 */
class ClientProductionModeConfigTest extends FunctionalTest {

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

    void 'custom parameters in yarnrc and npmrc'() {
        setup:
            buildFile << '''
               vaadin.productionMode = true
               vaadin.autoconfigure()

               vaadinClientDependencies {
                    customYarnProperties = ['strict-ssl' : false ]
                    customNpmProperties = ['strict-ssl' : false ]
               }
            '''.stripIndent()

            run 'vaadinCreateProject'
        when:
            run 'vaadinInstallYarnDependencies'
        then:
            File frontend = Paths.get(buildFile.parentFile.canonicalPath, 'build', 'frontend').toFile()
            File yarnrc = new File(frontend, '.yarnrc')
            yarnrc.text.contains('strict-ssl false')
            File npmrc = new File(frontend, '.npmrc')
            npmrc.text.contains('strict-ssl=false')
    }

    void 'custom filter html imports'() {
        setup:
            buildFile << '''
              vaadin.productionMode = true
              vaadin.autoconfigure()

              vaadinTranspileDependencies {
                importExcludes = [
                    '.*/theme/material/.*',
                    '.*/vaadin-material-styles/.*'
                ]
              }
            '''.stripIndent()

            run 'vaadinCreateProject'
        when:
            run 'vaadinTranspileDependencies'
        then:
            File frontend = Paths.get(buildFile.parentFile.canonicalPath, 'build', 'frontend').toFile()
            File polymerjson = new File(frontend, 'polymer.json')
            !polymerjson.text.contains('/theme/material/')
            !polymerjson.text.contains('/vaadin-material-styles/')
    }
}
