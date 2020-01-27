/*
 * Copyright 2018-2020 Devsoap Inc.
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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

import com.devsoap.vaadinflow.tasks.WrapCssTask
import com.devsoap.vaadinflow.tasks.CreateProjectTask
import org.gradle.testkit.runner.BuildResult

import java.nio.file.Paths

/**
 * Theme handling tests
 *
 * @author John Ahlroos
 * @since 1.0
 */
class ThemeTest extends FunctionalTest {

    void 'CSS styles are wrapped to HTML'() {
        setup:
            compatibilityMode = true
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
            File rootDir = testProjectDir.root
            File stylesDir = Paths.get(rootDir.canonicalPath,
                    'build', 'webapp-gen', 'frontend', 'styles').toFile()
        when:
            run CreateProjectTask.NAME
            BuildResult result = run WrapCssTask.NAME
        then:
            result.task(':' + WrapCssTask.NAME).outcome == SUCCESS
            stylesDir.listFiles().size() == 1
            stylesDir.listFiles()[0].name == "${rootDir.name}-theme.html"
    }

    void 'default legacy theme is created'() {
        setup:
            compatibilityMode = true
            File rootDir = testProjectDir.root
            File webappDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'webapp').toFile()
            File frontendDir = new File(webappDir, 'frontend')
            File stylesDir = new File(frontendDir, 'styles')
            File cssFile = new File(stylesDir, rootDir.name.toLowerCase() + '-theme.css')

            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File uiFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}UI.java").toFile()
        when:
            BuildResult result = run 'vaadinCreateProject'
        then:
            result.task(':vaadinCreateProject').outcome == SUCCESS
            cssFile.exists()
            cssFile.text.contains('This file contains the Application theme')
            uiFile.exists()
            uiFile.text.contains("@HtmlImport(\"frontend://styles/${cssFile.name - '.css'}.html\")")
            uiFile.text.contains('@Theme(Lumo.class)')
    }

    void 'default theme is created'() {
        setup:
            compatibilityMode = false
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
            File rootDir = testProjectDir.root
            File stylesheetsDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'stylesheets').toFile()
            File cssFile = new File(stylesheetsDir, 'theme.css')

            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File viewFile = Paths.get(pkg.canonicalPath,
                    "${testProjectDir.root.name.capitalize()}View.java").toFile()
        when:
            BuildResult result = run 'vaadinCreateProject'
        then:
            result.task(':vaadinCreateProject').outcome == SUCCESS
            cssFile.exists()
            cssFile.text.contains('This file contains the Application theme')
            viewFile.exists()
            viewFile.text.contains('@CssImport("./theme.css")')
            viewFile.text.contains('@Theme(Lumo.class)')
    }

    void 'CSSImports are wrapped'() {
        setup:
            compatibilityMode = false
            extraPlugins['io.freefair.jsass-java'] = '4.1.6'
            buildFile.delete()
            initBuildFile()
            buildFile << '''
                    vaadin.autoconfigure()
            '''.stripIndent()

            run 'vaadinCreateProject'

            File rootDir = testProjectDir.root
            File stylesheetsDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'stylesheets').toFile()

            new File(stylesheetsDir, 'sass-theme.scss').tap {
                it.text = '$primary-color: green; label { color: $primary_color; }'
            }

            new File(stylesheetsDir, 'test/subdir-theme.scss').tap {
                it.parentFile.mkdirs()
                it.text = '$primary-color: red; label { color: $primary_color; }'
            }

            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()

            Paths.get(pkg.canonicalPath, "${testProjectDir.root.name.capitalize()}View.java").toFile()
                    .tap {
                it.text = it.text.replace('@CssImport("./theme.css")', """
                    @CssImport("./theme.css")
                    @CssImport("sass-theme.scss")
                    @CssImport("test/subdir-theme.scss")
                """.trim().stripIndent())
            }

            File srcDir = Paths.get(rootDir.canonicalPath, 'build',  'frontend', 'src').toFile()
            File wrappedThemeCss = new File(srcDir, 'theme.js')
            File wrappedSassCss = new File(srcDir, 'sass-theme.js')
            File wrappedSubDirCss = new File(srcDir, 'test/subdir-theme.js')
        when:
            BuildResult result = run ('clean', 'vaadinWrapCss')
        then:
            result.task(':vaadinWrapCss').outcome == SUCCESS
            wrappedThemeCss.exists()
            wrappedSassCss.exists()
            wrappedSassCss.text.contains('color: green')
            wrappedSubDirCss.exists()
            wrappedSubDirCss.text.contains('color: red')
    }
}
