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

import org.gradle.testkit.runner.BuildResult

import java.nio.file.Paths

/**
 * Test for testing client dependencies added to the project
 *
 * @author John Ahlroos
 * @since 1.3
 */
class ClientDependenciesTest extends FunctionalTest {

    void 'bundle NPM dependencies with Webpack'() {
        setup:
            compatibilityMode = false
            buildFile << '''
            vaadin.autoconfigure()
            '''.stripIndent()
        when:
            BuildResult createResult = run 'vaadinCreateProject'
            BuildResult jarResult = run('jar')
            File build = Paths.get(testProjectDir.root.canonicalPath, 'build').toFile()
            File frontend = Paths.get(build.canonicalPath, 'frontend').toFile()
            File dist = new File(frontend,  'dist')
            File scripts = new File(frontend, 'scripts')
            File genVaadin = Paths.get(build.canonicalPath, 'webapp-gen', 'VAADIN').toFile()
            File genBuild = new File(genVaadin, 'build')
        then:
            createResult.output.contains('Licensed to ')
            !createResult.output.contains('UNLICENSED')
            !jarResult.output.contains('Vaadin 13 (Flow 1) compatibility mode')

            new File(frontend, 'package.json').exists()
            new File(frontend, 'webpack.config.js').exists()
            new File(frontend, '.npmrc').exists()
            new File(frontend, '.yarnrc').exists()
            new File(scripts, 'build-dist.js').exists()
            new File(dist, 'package.json').exists()
            new File(dist, 'yarn.lock').exists()
            new File(genBuild, 'webcomponentsjs').exists()
            genVaadin.list { dir, name -> name.startsWith('vaadin-main.es5') }.length > 0
            genVaadin.list { dir, name -> name.startsWith('vaadin-main-') }.length > 0
    }

}
