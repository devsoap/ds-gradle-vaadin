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
import spock.lang.Unroll

import java.nio.file.Paths

/**
 * Tests creation of Vaadin components
 *
 * @author John Ahlroos
 * @since 1.0
 */
class ComponentTests extends FunctionalTest {

    void 'default composite is created'() {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File compositeFile = Paths.get(pkg.canonicalPath, 'ExampleComposite.java').toFile()
        when:
            BuildResult result = run 'vaadinCreateComposite'
        then:
            result.task(':vaadinCreateComposite').outcome == TaskOutcome.SUCCESS
            compositeFile.exists()
            compositeFile.text.contains('import com.vaadin.flow.component.html.Div;')
            compositeFile.text.contains('public class ExampleComposite extends Composite<Div>')
    }

    void 'default composite compiles'() {
        setup:
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
        when:
            run 'vaadinCreateComposite'
            BuildResult result = run 'jar'
        then:
            result.task(':jar').outcome == TaskOutcome.SUCCESS
    }

    @Unroll
    void '#compositeName project is created in #compositePackage package'(String compositeName,
                                                                          String compositePackage) {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, compositePackage.split('\\.')).toFile()
            File compositeFile = Paths.get(pkg.canonicalPath, 'MyComposite.java').toFile()
            String baseClass = 'com.vaadin.flow.component.html.Label'
        when:
            BuildResult result = run 'vaadinCreateComposite',
                    "--name=$compositeName",
                    "--package=$compositePackage",
                    "--baseClass=$baseClass"
        then:
            result.task(':vaadinCreateComposite').outcome == TaskOutcome.SUCCESS
            compositeFile.exists()
            compositeFile.text.contains("import $baseClass;")
            compositeFile.text.contains('public class MyComposite extends Composite<Label>')
        where:
            compositeName = 'MyComposite'
            compositePackage = 'com.hello.world'
    }
}
