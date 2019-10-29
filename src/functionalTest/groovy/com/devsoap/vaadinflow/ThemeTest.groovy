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

    void 'default theme is created'() {
        setup:
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
}
