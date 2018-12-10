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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecSpec

import java.nio.file.Paths
import java.util.logging.Level

/**
 * Yarn runner with Vaadin plugin specific functionality
 *
 * @author John Ahlroos
 * @since 1.0
 */
class VaadinYarnRunner extends YarnExecRunner {

    private static final String YARN_RC_FILENAME = '.yarnrc'
    private static final String OFFLINE = '--offline'
    private static final String PREFER_OFFLINE = '--prefer-offline'
    private static final String INSTALL_COMMAND = 'install'
    private static final String POLYMER_COMMAND = 'polymer'
    private static final String BOWER_COMMAND = 'bower'
    private static final String POLYMER_BUNDLER_COMMAND = 'polymer-bundler'
    private static final String RUN_COMMAND = 'run'

    private boolean isOffline = false

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
        this.isOffline = project.gradle.startParameter.offline
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
        if(isOffline) {
            // The old lock file need to remain for offline install to work
            File yarnLockFile = Paths.get(project.buildDir.canonicalPath, 'frontend','yarn.lock').toFile()
            if(!yarnLockFile.exists()) {
                throw new GradleException(
                    "Cannot perform offline Yarn install without existing yarn.lock file in $yarnLockFile.parentFile")
            }
        }

        generateYarnRc()
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, '--no-bin-links', INSTALL_COMMAND]
        execute().assertNormalExitValue()
    }

    /**
     * Yarn init
     *
     * https://yarnpkg.com/en/docs/cli/init
     */
    void init() {
        generateYarnRc()

        // Generator package.json
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, 'init', '-y']
        execute().assertNormalExitValue()

        // Set proper defaults for package.json
        File packageJson = new File(workingDir as File, 'package.json')
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.main = ''
        pkg.version = '1.0.0'
        pkg.name = 'frontend'

        pkg.devDependencies['polymer-cli'] = Versions.rawVersion('polymer.cli.version')
        pkg.scripts[POLYMER_COMMAND] = './node_modules/polymer-cli/bin/polymer.js'

        pkg.devDependencies[POLYMER_BUNDLER_COMMAND] = Versions.rawVersion('polymer.bundler.version')
        pkg.scripts[POLYMER_BUNDLER_COMMAND] = './node_modules/polymer-bundler/lib/bin/polymer-bundler.js'

        pkg.devDependencies[BOWER_COMMAND] = Versions.rawVersion('bower.version')
        pkg.scripts[BOWER_COMMAND] = './node_modules/bower/bin/bower'

        if (this.variant.windows) {
            pkg.scripts.replaceAll { key, value -> this.variant.nodeExec + ' ' + value }
        }

        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))
    }

    /**
     * Bower install
     *
     * https://bower.io/docs/api/#install
     */
    void bowerInstall() {
        generateYarnRc()
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, RUN_COMMAND, BOWER_COMMAND, INSTALL_COMMAND,
                     '--config.interactive=false' ]
        if(isOffline) {
            arguments << OFFLINE
        }
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
    void polymerBundle(File manifestJson, File htmlManifest, List<String> excludes=[]) {
        generateYarnRc()
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, RUN_COMMAND, POLYMER_BUNDLER_COMMAND,
                    '--inline-scripts',
                    "--manifest-out=${manifestJson.canonicalPath}"]
        arguments.addAll(excludes.collect { "--exclude \"$it\"" })
        arguments.add(htmlManifest.name)
        execute().assertNormalExitValue()
    }

    /**
     * Polymer build
     */
    void transpile() {
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, RUN_COMMAND, POLYMER_COMMAND,
                     'build', '--npm', '--module-resolution=node']
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
                        'cacheFolder': './build/frontend/yarn-cache'
                    ]).build().write()
        }
    }
}
