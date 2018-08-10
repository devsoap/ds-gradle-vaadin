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
package com.devsoap.vaadinflow.util

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.variant.Variant
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import java.nio.file.Paths

/**
 * Test for VaadinYarnRunner
 *
 * @author John Ahlroos
 * @since 1.0
 */
class VaadinYarnRunnerTest extends Specification {

    private VaadinYarnRunner runner
    private File packageJson
    private File workingDir
    private Project project

    protected void setup() {
        File testProjectDir = File.createTempDir('junit', '')
        workingDir = Paths.get(testProjectDir.canonicalPath, 'build', 'frontend').toFile()
        workingDir.mkdirs()

        packageJson = new File(workingDir, 'package.json')
        packageJson.text = '{}'

        project = new ProjectBuilder().withProjectDir(testProjectDir).build()
        project.extensions.create(VaadinClientDependenciesExtension.NAME, VaadinClientDependenciesExtension, project)

        project.extensions.create(NodeExtension.NAME, NodeExtension, project).with {
            variant = new Variant()
            variant.windows = false
        }

        runner = new TestRunner(project, workingDir, Mock(ExecResult))
    }

    void 'offline cache path is set in yarn.rc'() {
        setup:
            project.extensions.getByType(VaadinClientDependenciesExtension).offlineCachePath = '/foo/bar'
        when:
            runner.init()
            File yarnRc = new File(workingDir, '.yarnrc')
        then:
            yarnRc.exists()
            yarnRc.text.contains('yarn-offline-mirror "/foo/bar"')
    }

    void 'package.json contains scripts'() {
        when:
            runner.init()
        then:
            packageJson.exists()
            packageJson.text.contains('"polymer": "./node_modules/polymer-cli/bin/polymer.js"')
            packageJson.text.contains('"polymer-bundler": "./node_modules/polymer-bundler/lib/bin/polymer-bundler.js"')
            packageJson.text.contains('"bower": "./node_modules/bower/bin/bower"')
    }

    void 'package.json contains scripts with node reference on Windows'() {
        setup:
            project.extensions.getByType(NodeExtension).variant.with {
                windows = true
                nodeExec = 'C:/path/to/windows/node.exe'
            }
        when:
            runner.init()
        then:
            packageJson.exists()
            packageJson.text.contains(
                    '"polymer": "C:/path/to/windows/node.exe ./node_modules/polymer-cli/bin/polymer.js"')
            packageJson.text.contains(
         '"polymer-bundler": "C:/path/to/windows/node.exe ./node_modules/polymer-bundler/lib/bin/polymer-bundler.js"')
            packageJson.text.contains(
                    '"bower": "C:/path/to/windows/node.exe ./node_modules/bower/bin/bower"')
    }

    private static class TestRunner extends VaadinYarnRunner {

        private final ExecResult result

        TestRunner(Project project, File workingDir, ExecResult result) {
            super(project, workingDir)
            this.result = result
        }

        @Override
        protected ExecResult doExecute() {
            result
        }
    }
}
