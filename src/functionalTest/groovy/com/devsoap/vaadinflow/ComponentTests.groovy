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
    void '#compositeName composite is created in #compositePackage package'(String compositeName,
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

    void 'default component is created'() {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File componentFile = Paths.get(pkg.canonicalPath, 'ExampleTextField.java').toFile()
        when:
            BuildResult result = run 'vaadinCreateComponent'
        then:
            result.task(':vaadinCreateComponent').outcome == TaskOutcome.SUCCESS
            componentFile.exists()
            componentFile.text.contains('@Tag("input")')
            componentFile.text.contains('public class ExampleTextField extends Component')
    }

    void 'default component compiles'() {
        setup:
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
        when:
            run 'vaadinCreateComponent'
            BuildResult result = run 'jar'
        then:
            result.task(':jar').outcome == TaskOutcome.SUCCESS
    }

    @Unroll
    void '#componentName component is created in #componentPackage package'(String componentName,
                                                                            String componentPackage) {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, componentPackage.split('\\.')).toFile()
            File componentFile = Paths.get(pkg.canonicalPath, 'MyComponent.java').toFile()
            String tag = 'textarea'
        when:
            BuildResult result = run 'vaadinCreateComponent',
                    "--name=$componentName",
                    "--package=$componentPackage",
                    "--tag=$tag"
        then:
            result.task(':vaadinCreateComponent').outcome == TaskOutcome.SUCCESS
            componentFile.exists()
            componentFile.text.contains('@Tag("textarea")')
            componentFile.text.contains('public class MyComponent extends Component')
        where:
            componentName = 'MyComponent'
            componentPackage = 'com.hello.world'
    }

    void 'default web template compiles'() {
        setup:
            buildFile << '''
                    vaadin.autoconfigure()
                '''.stripIndent()
        when:
            run 'vaadinCreateWebTemplate'
            BuildResult result = run 'jar'
        then:
            result.task(':jar').outcome == TaskOutcome.SUCCESS
    }

    void 'default web template is created'() {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File templatesDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'webapp',
                    'frontend', 'templates').toFile()
            File pkg = Paths.get(javaSourceDir.canonicalPath, 'com', 'example',
                    testProjectDir.root.name.toLowerCase()).toFile()
            File javaFile = Paths.get(pkg.canonicalPath, 'ExampleWebTemplate.java').toFile()
            File templateFile = Paths.get(templatesDir.canonicalPath, 'ExampleWebTemplate.html').toFile()
        when:
            BuildResult result = run 'vaadinCreateWebTemplate'
        then:
            result.task(':vaadinCreateWebTemplate').outcome == TaskOutcome.SUCCESS

            javaFile.exists()
            javaFile.text.contains('@Tag("example-web-template")')
            javaFile.text.contains('@HtmlImport("templates/ExampleWebTemplate.html")')
            javaFile.text.contains(
               'public class ExampleWebTemplate extends PolymerTemplate<ExampleWebTemplate.ExampleWebTemplateModel>')

            templateFile.exists()
            templateFile.text.contains('<dom-module id="example-web-template">')
            templateFile.text.contains('class ExampleWebTemplate extends Polymer.Element')
            templateFile.text.contains('customElements.define(ExampleWebTemplate.is, ExampleWebTemplate);')
    }

}
