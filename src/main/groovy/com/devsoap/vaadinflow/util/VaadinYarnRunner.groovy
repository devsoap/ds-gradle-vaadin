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
    private static final String WEBPACK_COMMAND = 'webpack'
    private static final String WEBPACK_CLI_COMMAND = 'webpack-cli'
    private static final String RUN_COMMAND = 'run'
    private static final String FRONTEND = 'frontend'
    private static final String WORK_DIR_OPTION = '--cwd'

    private final boolean isOffline

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
        if (isOffline) {
            // The old lock file need to remain for offline install to work
            File yarnLockFile = Paths.get(project.buildDir.canonicalPath, FRONTEND, 'yarn.lock').toFile()
            if (!yarnLockFile.exists()) {
                throw new GradleException(
                    "Cannot perform offline Yarn install without existing yarn.lock file in $yarnLockFile.parentFile")
            }
        }

        generateYarnRc()
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, '--no-bin-links', WORK_DIR_OPTION, workingDir,
                     INSTALL_COMMAND]
        execute().assertNormalExitValue()
    }

    /**
     * Yarn init
     *
     * https://yarnpkg.com/en/docs/cli/init
     */
    void init() {

        File frontendDir = workingDir as File
        if (!frontendDir.exists()) {
            frontendDir.mkdirs()
        }

        generateYarnRc()

        // Generate package.json
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, WORK_DIR_OPTION, workingDir, 'init', '-y']
        execute().assertNormalExitValue()

        // Set proper defaults for package.json
        File packageJson = new File(frontendDir, 'package.json')
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.main = ''
        pkg.version = project.version
        pkg.name = FRONTEND

        pkg.devDependencies['polymer-cli'] = Versions.rawVersion('polymer.cli.version')
        pkg.scripts[POLYMER_COMMAND] = './node_modules/polymer-cli/bin/polymer.js'

        pkg.devDependencies[POLYMER_BUNDLER_COMMAND] = Versions.rawVersion('polymer.bundler.version')
        pkg.scripts[POLYMER_BUNDLER_COMMAND] = './node_modules/polymer-bundler/lib/bin/polymer-bundler.js'

        pkg.devDependencies[BOWER_COMMAND] = Versions.rawVersion('bower.version')
        pkg.scripts[BOWER_COMMAND] = './node_modules/bower/bin/bower'

        pkg.devDependencies[WEBPACK_COMMAND] = Versions.rawVersion('webpack.version')
        pkg.devDependencies[WEBPACK_CLI_COMMAND] = Versions.rawVersion('webpack.cli.version')
        pkg.scripts[WEBPACK_COMMAND] = './node_modules/webpack/bin/webpack.js'

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
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, WORK_DIR_OPTION, workingDir,
                     RUN_COMMAND, BOWER_COMMAND, INSTALL_COMMAND,
                     '--config.interactive=false' ]
        if (isOffline) {
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
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, WORK_DIR_OPTION, workingDir, RUN_COMMAND,
                     POLYMER_BUNDLER_COMMAND]
        arguments.addAll(excludes.collect { "--exclude \"$it\"" })
        arguments.addAll( ['--inline-scripts', "--manifest-out=${manifestJson.canonicalPath}"])
        arguments.add(htmlManifest.name)
        execute().assertNormalExitValue()
        manifestJson.text = JsonOutput.prettyPrint(manifestJson.text)
    }

    /**
     * Webpack bundle
     *
     * @since 1.3
     */
    void webpackBundle(File statsFile, File bundleFile) {

        generateWebpackConfig(bundleFile)

        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, WORK_DIR_OPTION, workingDir, RUN_COMMAND, WEBPACK_COMMAND,
                     '--profile', '--json']

        ByteArrayOutputStream statsStream = new ByteArrayOutputStream()

        Closure oldExecOverrides = execOverrides
        execOverrides = { ExecSpec spec ->
            spec.standardOutput = statsStream
            spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
        }

        execute().assertNormalExitValue()

        execOverrides = oldExecOverrides

        new ByteArrayInputStream(statsStream.toByteArray()).with { stream ->
            boolean jsonStartFound = false
            boolean jsonEndFound = false
            statsFile.withWriter { writer ->
                stream.filterLine(writer) { line ->
                    if (!jsonStartFound && line.startsWith('{')) {
                        jsonStartFound = true
                        return true
                    } else if (!jsonEndFound && line.startsWith('}')) {
                        jsonEndFound = true
                        return true
                    }
                    jsonStartFound && !jsonEndFound
                }
            }
        }
    }

    /**
     * Polymer build
     */
    void transpile(List<String> excludes=[]) {
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, WORK_DIR_OPTION, workingDir, RUN_COMMAND, POLYMER_COMMAND]
        arguments.addAll(excludes.collect { "--exclude \"$it\"" })
        arguments.addAll(['build', '--npm', '--module-resolution=node'])
        execute().assertNormalExitValue()
    }

    private void generateYarnRc() {
        VaadinClientDependenciesExtension vaadinClient = project.extensions.getByType(VaadinClientDependenciesExtension)
        File yarnrc = new File(workingDir as File, YARN_RC_FILENAME)
        if (!yarnrc.exists()) {

            Map<String, Object> params = [:]
            params['lastUpdateCheck'] = 0
            params['cache-folder'] = './build/frontend/yarn-cache'
            params['yarn-offline-mirror'] = vaadinClient.offlineCachePath
            params['yarn-offline-mirror-pruning'] = true
            params['disable-self-update-check'] = true

            if (HttpUtils.httpsProxy) {
                params['https-proxy'] = HttpUtils.httpsProxy
            }

            if (HttpUtils.httpProxy) {
                params['proxy'] = HttpUtils.httpProxy
            }

            params.putAll(vaadinClient.customYarnProperties)

            TemplateWriter.builder()
                    .targetDir(workingDir as File)
                    .templateFileName(YARN_RC_FILENAME)
                    .substitutions(['parameters' : params])
                    .build().write()
        }
    }

    private void generateWebpackConfig(File bundleFile) {
        TemplateWriter.builder()
                .targetDir(workingDir as File)
                .templateFileName('webpack.config.js')
                .substitutions(['targetPath' :  bundleFile.parentFile.canonicalPath ])
                .build().write()
    }
}
