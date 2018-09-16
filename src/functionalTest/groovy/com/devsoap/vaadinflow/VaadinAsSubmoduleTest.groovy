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

import java.nio.file.Path
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
            String offlineCachePath = System.getProperty('yarn.cache.dir',
                Paths.get(rootProjectDir.root.canonicalPath, 'yarn-cache').toFile().canonicalPath)
            buildFile.text = """
                 plugins {
                    id '$PLUGIN_ID' apply false
                 }

                 project(':libraryProject') {
                    apply plugin: 'java'
                 }

                 project(':vaadinProject') {
                    apply plugin: '$PLUGIN_ID'

                    vaadinClientDependencies {
                        offlineCachePath = "$offlineCachePath"
                    }

                    dependencies {
                        implementation project(':libraryProject')
                    }
                 }
            """.stripMargin()
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
            BuildResult result = run('vaadinConvertStyleCssToHtml')
        then:
            result.task(':vaadinProject:vaadinConvertStyleCssToHtml').outcome == TaskOutcome.SUCCESS
            wrappedCss.exists()
    }
}
