/*
 * Copyright 2018-2019 Devsoap Inc.
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0
 * International Public License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      https://creativecommons.org/licenses/by-nd/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.vaadinflow

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

import spock.lang.Unroll

import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.BuildResult

import java.nio.file.Paths

/**
 * Tests creation of Vaadin projects
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateProjectTest extends FunctionalTest {

    void 'default project is created'() {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File servletFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}Servlet.java").toFile()
            File viewFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}View.java").toFile()
            File uiFile = Paths.get(pkg.canonicalPath,
                "${testProjectDir.root.name.capitalize()}UI.java").toFile()
        when:
            BuildResult result = run'vaadinCreateProject'
        then:
            result.task(':vaadinCreateProject').outcome == SUCCESS
            servletFile.exists()
            viewFile.exists()
            uiFile.exists()
    }

    void 'default project compiles'() {
        setup:
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
        when:
            run'vaadinCreateProject'
            BuildResult result = run'jar'
        then:
            result.task(':jar').outcome == TaskOutcome.SUCCESS
    }

    @Unroll
    void '#applicationName project is created in #applicationPackage package'(String applicationName,
                                                                              String applicationPackage) {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, applicationPackage.split('\\.')).toFile()
            File servletFile = Paths.get(pkg.canonicalPath, "${applicationName}Servlet.java").toFile()
            File viewFile = Paths.get(pkg.canonicalPath, "${applicationName}View.java").toFile()
            File uiFile = Paths.get(pkg.canonicalPath, "${applicationName}UI.java").toFile()
        when:
            BuildResult result = run'vaadinCreateProject',
                    "--name=$applicationName", "--package=$applicationPackage"
        then:
            result.task(':vaadinCreateProject').outcome == SUCCESS
            servletFile.exists()
            viewFile.exists()
            uiFile.exists()
        where:
            applicationName = 'Foo'
            applicationPackage = 'bar.baz'
    }

    void '*123-foo bar is converted to a valid application name'() {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                testProjectDir.root.name.toLowerCase()).toFile()
            File servletFile = Paths.get(pkg.canonicalPath, '123FooBarServlet.java').toFile()
            File viewFile = Paths.get(pkg.canonicalPath, '123FooBarView.java').toFile()
            File uiFile = Paths.get(pkg.canonicalPath, '123FooBarUI.java').toFile()
        when:
            BuildResult result = run'vaadinCreateProject', '--name="*123-Foo bar"'
        then:
            result.task(':vaadinCreateProject').outcome == SUCCESS
            servletFile.exists()
            viewFile.exists()
            uiFile.exists()
    }

    void 'Material base theme is used'() {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File uiFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}UI.java").toFile()
        when:
            BuildResult result = run 'vaadinCreateProject', '--baseTheme=Material'
        then:
            result.task(':vaadinCreateProject').outcome == SUCCESS
            uiFile.exists()
            uiFile.text.contains('@Theme(Material.class)')
    }

    @Unroll
    void 'setting base theme to #baseTheme fails'(String baseTheme) {
        when:
            BuildResult result = runAndFail( 'vaadinCreateProject', "--baseTheme=$baseTheme")
        then:
            result.task(':vaadinCreateProject').outcome == FAILED
        where:
            baseTheme = 'foobar'
    }

    void 'spring boot project is created and compiles'() {
        setup:
            extraPlugins = ['org.springframework.boot':'2.0.5.RELEASE']
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()

            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File applicationFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}Application.java").toFile()
            File jarArchiveFile = Paths.get(rootDir.canonicalPath, 'build', 'libs',
                    testProjectDir.root.name.toLowerCase() + '.jar').toFile()
        when:
            BuildResult createResult = run'vaadinCreateProject'
            BuildResult buildResult = run'bootJar'
        then:
            createResult.task(':vaadinCreateProject').outcome == SUCCESS
            applicationFile.exists()
            buildResult.task(':bootJar').outcome == SUCCESS
            jarArchiveFile.exists()
    }
}
