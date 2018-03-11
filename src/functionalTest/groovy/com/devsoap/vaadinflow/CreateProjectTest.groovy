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
        when:
            BuildResult result = run'--info', 'vaadinCreateProject'
        then:
            result.task(':vaadinCreateProject').outcome == SUCCESS
            servletFile.exists()
            viewFile.exists()
    }
}
