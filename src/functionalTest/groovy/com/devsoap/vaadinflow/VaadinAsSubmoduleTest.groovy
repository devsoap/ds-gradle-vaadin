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

import com.devsoap.vaadinflow.tasks.WrapCssTask
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Paths

/**
 * Tests creation of Vaadin project as submodule
 *
 * @author John Ahlroos
 * @since 1.0
 */
class VaadinAsSubmoduleTest extends MultimoduleFunctionalTest {

    void 'submodule project creates project files in correct place'() {
        when:
            BuildResult result = run('vaadinCreateProject')
        then:
            result.output.contains('/vaadinProject/src/main/java/com/example/vaadinproject/VaadinProjectServlet.java')
            result.task(':vaadinProject:vaadinCreateProject').outcome == TaskOutcome.SUCCESS
    }

    void 'submodule project creates component files in correct place'() {
        when:
            BuildResult result = run('vaadinCreateComponent')
        then:
            result.output.contains('/vaadinProject/src/main/java/com/example/vaadinproject/ExampleTextField.java')
            result.task(':vaadinProject:vaadinCreateComponent').outcome == TaskOutcome.SUCCESS
    }

    void 'submodule project with single build file'() {
        setup:
            vaadinProjectBuildFile.delete()
            libraryProjectBuildFile.delete()
            buildFile.text = """
                 plugins {
                    id '$PLUGIN_ID' apply false
                 }

                 project(':libraryProject') {
                    apply plugin: 'java'
                 }

                 project(':vaadinProject') {
                    apply plugin: '$PLUGIN_ID'

                    vaadin.version = '$vaadinVersion'

                    vaadinClientDependencies {
                        offlineCachePath = "$offlineCachePath"
                    }

                    dependencies {
                        implementation project(':libraryProject')
                    }
                 }
            """.stripIndent()
        when:
            BuildResult result = run('vaadinCreateProject')
        then:
            result.output.contains('/vaadinProject/src/main/java/com/example/vaadinproject/VaadinProjectServlet.java')
            result.task(':vaadinProject:vaadinCreateProject').outcome == TaskOutcome.SUCCESS
    }

    void 'css wrappers are copied into the correct folder in sub-module'() {
        setup:
            File genStyles = Paths.get(vaadinProject.canonicalPath,
                    'build', 'webapp-gen', 'frontend', 'styles').toFile()
            File wrappedCss = new File(genStyles, "${vaadinProject.name.toLowerCase()}-theme.html")
            run('vaadinCreateProject')
        when:
            BuildResult result = run(WrapCssTask.NAME)
        then:
            result.task(':vaadinProject:' + WrapCssTask.NAME).outcome == TaskOutcome.SUCCESS
            wrappedCss.exists()
    }

    void 'groovy templates are compiled into the correct folder in sub-module'() {
        setup:
            vaadinProjectBuildFile.text = """
                apply plugin: '$PLUGIN_ID'
                apply plugin: 'groovy'

                vaadinClientDependencies {
                    offlineCachePath = "$offlineCachePath"
                }

                dependencies {
                    implementation project(':libraryProject')
                }
            """.stripIndent()

            File templates = Paths.get(vaadinProject.canonicalPath,
                    'build', 'webapp-gen', 'frontend', 'templates').toFile()
            File compiledTemplate = new File(templates, 'groovytemplate.html')
            run('vaadinCreateProject')
            run 'vaadinCreateWebTemplate', '--name', 'groovytemplate'
        when:
            BuildResult result = run('vaadinConvertGroovyTemplatesToHtml')
        then:
            result.task(':vaadinProject:vaadinConvertGroovyTemplatesToHtml').outcome == TaskOutcome.SUCCESS
            compiledTemplate.exists()
    }

    void 'include dependant Java projects in transpilation'() {
        setup:
            vaadinProjectBuildFile << 'vaadin.productionMode = true'
            File bowerJson = Paths.get(libraryProject.canonicalPath,
                    'src', 'main', 'resources', 'bower.json').toFile()
            bowerJson.parentFile.mkdirs()
            bowerJson.createNewFile()
            File unpackedJar = Paths.get(vaadinProject.canonicalPath,
                    'build', 'frontend', 'bower_components', 'libraryProject').toFile()
            File unpackedBowerJson = new File(unpackedJar, 'bower.json')
        when:
            run(':vaadinProject:vaadinCreateProject')
            BuildResult result = run(':vaadinProject:vaadinInstallBowerDependencies')
        then:
            result.output.contains('Unpacking frontend component in libraryProject.jar')
            unpackedBowerJson.exists()
    }

    void 'include dependant resource projects in transpilation'() {
        setup:
            vaadinProjectBuildFile << 'vaadin.productionMode = true'
            libraryProjectBuildFile.text = "apply plugin: 'base'"
            File bowerJson = Paths.get(libraryProject.canonicalPath, 'bower.json').toFile()
            bowerJson.createNewFile()
            File unpackedProject = Paths.get(vaadinProject.canonicalPath,
                    'build', 'frontend', 'bower_components', 'libraryProject').toFile()
            File unpackedBowerJson = new File(unpackedProject, 'bower.json')
        when:
            run(':vaadinProject:vaadinCreateProject')
            BuildResult result = run(':vaadinProject:vaadinInstallBowerDependencies')
        then:
            result.output.contains("Unpacking frontend component in project ':libraryProject'")
            unpackedBowerJson.exists()
    }
}
