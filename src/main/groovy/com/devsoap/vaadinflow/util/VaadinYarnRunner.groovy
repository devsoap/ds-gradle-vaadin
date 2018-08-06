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
import com.devsoap.vaadinflow.models.ClientPackage
import com.moowork.gradle.node.yarn.YarnExecRunner
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.process.ExecSpec

import java.util.logging.Level

/**
 * Yarn runner with Vaadin plugin specific funcationality
 *
 * @author John Ahlroos
 * @since 1.0
 */
class VaadinYarnRunner extends YarnExecRunner {

    private static final String YARN_RC_FILENAME = '.yarnrc'
    private static final String PREFER_OFFLINE = '--prefer-offline'
    private static final String INSTALL_COMMAND = 'install'
    private static final String POLYMER_COMMAND = 'polymer'
    private static final String BOWER_COMMAND = 'bower'
    private static final String POLYMER_BUNDLER_COMMAND = 'polymer-bundler'
    private static final String RUN_COMMAND = 'run'

    /**
     * Creates a new Yarn runner
     *
     * @param project
     *      the project to run Yarn agains
     * @param workingDir
     *      the frontend working directory
     * @param standardOutput
     *      output stream for messages (stdout)
     */
    VaadinYarnRunner(Project project, File workingDir,
                     OutputStream standardOutput = LogUtils.getLogOutputStream(Level.FINE)) {
        super(project)
        this.workingDir = workingDir
        execOverrides = { ExecSpec spec ->
            spec.standardOutput = standardOutput
            spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
        }
    }

    /**
     * Yarn install
     *
     * https://yarnpkg.com/lang/en/docs/cli/install/
     */
    void install() {
        generateYarnRc()
        arguments = [PREFER_OFFLINE, '--no-bin-links', INSTALL_COMMAND]
        execute().assertNormalExitValue()
    }

    /**
     * Yarn init
     *
     * https://yarnpkg.com/en/docs/cli/init
     */
    void init() {

        // Generator package.json
        arguments = [PREFER_OFFLINE, 'init', '-y']
        execute().assertNormalExitValue()

        // Set proper defaults for package.json
        File packageJson = new File(workingDir as File, 'package.json')
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.main = ''
        pkg.version = '1.0.0'
        pkg.name = 'frontend'

        pkg.devDependencies['polymer-cli'] = Versions.rawVersion('polymer.cli.version')
        pkg.scripts[POLYMER_COMMAND] = 'node_modules/polymer-cli/bin/polymer.js'

        pkg.devDependencies[POLYMER_BUNDLER_COMMAND] = Versions.rawVersion('polymer.bundler.version')
        pkg.scripts[POLYMER_BUNDLER_COMMAND] = 'node_modules/polymer-bundler/lib/bin/polymer-bundler.js'

        pkg.devDependencies[BOWER_COMMAND] = 'latest'
        pkg.scripts[BOWER_COMMAND] = 'node_modules/bower/bin/bower'

        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))
    }

    /**
     * Bower install
     *
     * https://bower.io/docs/api/#install
     */
    void bowerInstall() {
        generateYarnRc()
        arguments = [PREFER_OFFLINE, RUN_COMMAND, BOWER_COMMAND, INSTALL_COMMAND, '--config.interactive=false' ]
        execute().assertNormalExitValue()
    }

    /**
     * Polymer bundle
     *
     * @param manifestJson
     *      the manifest json file
     *
     * @param htmlManifest
     *      the html manifest file
     */
    void polymerBundle(File manifestJson, File htmlManifest) {
        generateYarnRc()
        arguments = [PREFER_OFFLINE, RUN_COMMAND, POLYMER_BUNDLER_COMMAND,
                    '--inline-scripts',
                    "--manifest-out=${manifestJson.canonicalPath}",
                     htmlManifest.name]
        execute().assertNormalExitValue()
    }

    /**
     * Polymer build
     */
    void transpile() {
        arguments = [PREFER_OFFLINE, RUN_COMMAND, POLYMER_COMMAND, 'build', '--npm', '--module-resolution=node']
        execute().assertNormalExitValue()
    }

    private void generateYarnRc() {
        VaadinClientDependenciesExtension vaadinClient = project.extensions.getByType(VaadinClientDependenciesExtension)
        File yarnrc = new File(workingDir as File, YARN_RC_FILENAME)
        if (!yarnrc.exists()) {
            TemplateWriter.builder()
                    .targetDir(workingDir as File)
                    .templateFileName(YARN_RC_FILENAME)
                    .substitutions([
                        'offlineCachePath': vaadinClient.offlineCachePath,
                        'cacheFolder': new File(workingDir as File, 'yarn-cache').canonicalPath
                    ])
        }
    }
}
