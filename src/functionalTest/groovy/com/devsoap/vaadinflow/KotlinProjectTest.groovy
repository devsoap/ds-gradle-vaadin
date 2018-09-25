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
 * Tests for Kotlin projects
 *
 * @author John Ahlroos
 * @since 1.0
 */
class KotlinProjectTest extends FunctionalTest {

    void 'Create a Kotlin project'() {
        setup:
            extraPlugins = ['org.jetbrains.kotlin.jvm':'1.2.51']
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'kotlin').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File servletFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}Servlet.kt").toFile()
            File viewFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}View.kt").toFile()
            File uiFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}UI.kt").toFile()
        when:
            BuildResult result = run 'vaadinCreateProject'
        then:
            result.task(':vaadinCreateProject').outcome == SUCCESS
            servletFile.exists()
            viewFile.exists()
            uiFile.exists()
    }

    void 'Kotlin project compiles'() {
        setup:
            extraPlugins = ['org.jetbrains.kotlin.jvm':'1.2.51']
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
}
