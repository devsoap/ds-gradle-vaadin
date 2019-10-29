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

import com.devsoap.vaadinflow.tasks.CreateProjectTask
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * War packaging tess
 *
 * @author John Ahlroos
 * @since 1.0
 */
class WarArchiveTest extends FunctionalTest {

    void 'Wrapped CSS styles are packaged into WAR'() {
        setup:
            extraPlugins = ['war':'']
            buildFile << '''
                vaadin.autoconfigure()
            '''.stripIndent()
        when:
            run CreateProjectTask.NAME
            BuildResult result = run 'war'
            List<ZipEntry> files = getFilesInFolder('frontend/styles')
        then:
            result.task(':war').outcome == TaskOutcome.SUCCESS
            files.size() == 2
            files[0].name == "frontend/styles/${testProjectDir.root.name}-theme.css"
            files[1].name == "frontend/styles/${testProjectDir.root.name}-theme.html"
    }

    void 'Styles are included when using custom webapp directory'() {
        setup:
            extraPlugins = ['war':'']
            buildFile << '''
                vaadinAssembleClient {
                    webappDir 'src'
                }
                vaadin.autoconfigure()
            '''.stripIndent()
        when:
            run CreateProjectTask.NAME
            BuildResult result = run 'war'
            List<ZipEntry> files = getFilesInFolder('frontend/styles')
        then:
            result.task(':war').outcome == TaskOutcome.SUCCESS
            files.size() == 2
            files[0].name == "frontend/styles/${testProjectDir.root.name}-theme.css"
            files[1].name == "frontend/styles/${testProjectDir.root.name}-theme.html"
    }

    private ZipFile getWarFile() {
        File libsDir = Paths.get(testProjectDir.root.canonicalPath, 'build', 'libs').toFile()
        new ZipFile(new File(libsDir, testProjectDir.root.name + '.war'))
    }

    private List<ZipEntry> getFilesInFolder(String folder) {
        warFile.entries()
                .findAll { ZipEntry entry -> !entry.directory && entry.name.startsWith(folder) }
                .sort { ZipEntry a, ZipEntry b -> a.name <=> b.name }
    }
}
