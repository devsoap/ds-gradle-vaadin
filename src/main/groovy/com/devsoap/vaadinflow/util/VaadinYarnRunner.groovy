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

import com.devsoap.vaadinflow.actions.SpringBootAction
import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.models.ClientPackage
import com.moowork.gradle.node.yarn.YarnExecRunner
import groovy.json.JsonOutput
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import groovy.transform.PackageScope
import groovy.util.logging.Log
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec

import java.nio.file.Paths
import java.util.logging.Level

/**
 * Yarn runner with Vaadin plugin specific functionality
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
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
    private static final String BUILD_DISTRIBUTION_COMMAND = 'build-dist'
    private static final String NO_BIN_LINKS = '--no-bin-links'
    private static final String DIST_DIR = 'dist'
    private static final String INIT_COMMAND = 'init'
    private static final String YES_PARAM = '-y'
    private static final String PACKAGE_JSON = 'package.json'
    private static final String SPACE = ' '
    private static final String NODE_PTY_PREBUILT = 'node-pty-prebuilt'
    private static final String NODE_PTY = 'node-pty'
    private static final String DONE = 'Done'

    private final boolean isOffline
    private boolean captureOutput
    private OutputStream capturedOutputStream

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
    VaadinYarnRunner(Project project, File workingDir, boolean captureOutput=false) {
        super(project)
        this.workingDir = workingDir
        this.isOffline = project.gradle.startParameter.offline
        this.captureOutput = captureOutput
    }

    @Override
    protected ExecResult doExecute() {
        resetStreams()
        super.doExecute()
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

        // Install dev dependencies
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, NO_BIN_LINKS, WORK_DIR_OPTION, workingDir,
                     INSTALL_COMMAND]

        LogUtils.measureTime('Installing development packages done.') {
            execute().assertNormalExitValue()
        }
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
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, WORK_DIR_OPTION, workingDir, INIT_COMMAND, YES_PARAM]
        execute().assertNormalExitValue()

        // Set proper defaults for package.json
        File packageJson = new File(frontendDir, PACKAGE_JSON)
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.name = FRONTEND
        pkg.devDependencies = [:]
        pkg.scripts = [:]

        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)

        if (vaadin.compatibilityMode) {
            pkg.devDependencies['polymer-cli'] = Versions.rawVersion('polymer.cli.version')
            pkg.scripts[POLYMER_COMMAND] = './node_modules/polymer-cli/bin/polymer.js'

            pkg.devDependencies[POLYMER_BUNDLER_COMMAND] = Versions.rawVersion('polymer.bundler.version')
            pkg.scripts[POLYMER_BUNDLER_COMMAND] = './node_modules/polymer-bundler/lib/bin/polymer-bundler.js'

            pkg.devDependencies[BOWER_COMMAND] = Versions.rawVersion('bower.version')
            pkg.scripts[BOWER_COMMAND] = './node_modules/bower/bin/bower'

        } else {
            if (this.variant.windows) {
                pkg.devDependencies[NODE_PTY_PREBUILT] = Versions.rawVersion('node.pty.prebuilt.version')
            } else {
                pkg.devDependencies[NODE_PTY] = Versions.rawVersion('node.pty.version')
            }

            pkg.devDependencies[WEBPACK_COMMAND] = Versions.rawVersion('webpack.version')
            pkg.devDependencies[WEBPACK_CLI_COMMAND] = Versions.rawVersion('webpack.cli.version')
            pkg.devDependencies['webpack-babel-multi-target-plugin'] = Versions.rawVersion(
                    'bable.multitarget.plugin.version')
            pkg.scripts[WEBPACK_COMMAND] = './node_modules/webpack/bin/webpack.js'
        }

        if (this.variant.windows) {
            pkg.scripts.replaceAll { key, value -> this.variant.nodeExec + SPACE + value }
        }

        packageJson.text = ClientPackageUtils.toJson(pkg)
    }

    void distInstall() {
        generateYarnRc()
        File distDir = Paths.get(((File)workingDir).canonicalPath, DIST_DIR).toFile()
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, NO_BIN_LINKS, WORK_DIR_OPTION, distDir,
                     BUILD_DISTRIBUTION_COMMAND]
        LogUtils.measureTime('Installing application packages done.') {
            execute().assertNormalExitValue()
        }
    }

    void initDist() {
        File distDir = new File((File)workingDir, DIST_DIR)

        // Generate package.json
        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, WORK_DIR_OPTION, distDir, INIT_COMMAND, YES_PARAM]
        execute().assertNormalExitValue()

        // Set proper defaults for package.json
        File packageJson = new File(distDir, PACKAGE_JSON)
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.name = FRONTEND
        pkg.dependencies = [:]
        pkg.scripts = [:]

        pkg.dependencies['@babel/runtime'] = Versions.rawVersion('babel.runtime.version')
        pkg.dependencies['@webcomponents/webcomponentsjs'] = Versions.rawVersion('webcomponentsjs.version')

        addFlattenScript(pkg, '..')

        if (this.variant.windows) {
            pkg.scripts.replaceAll { key, value -> this.variant.nodeExec + SPACE + value }
        }

        packageJson.text = ClientPackageUtils.toJson(pkg)
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
    void webpackBundle(Project project, File statsFile, File infoFile) {

        generateWebpackConfig(project)

        generateBuildInfo(infoFile)

        arguments = [isOffline ? OFFLINE : PREFER_OFFLINE, WORK_DIR_OPTION, workingDir, RUN_COMMAND, WEBPACK_COMMAND,
                     '--json']

        boolean oldCapture = captureOutput
        captureOutput = true
        try {
            execute().assertNormalExitValue()
        } finally {
            captureOutput = oldCapture
        }

        LOGGER.info('Writing stats.json...')
        LogUtils.measureTime(DONE) {
            byte[] output = ((ByteArrayOutputStream) capturedOutputStream).toByteArray()
            new ByteArrayInputStream(output).with { stream ->
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

        LOGGER.info('Sanitizing stats.json...')
        LogUtils.measureTime(DONE) {
            Object stats = new JsonSlurper(type: JsonParserType.CHARACTER_SOURCE).parse(statsFile)
            Map out = [assetsByChunkName: stats.assetsByChunkName]
            out.chunks = []
            stats.chunks.each { Map c ->
                Map chunk = [modules: []]
                recursivelyAddModules(c, chunk)
                out.chunks << chunk
            }
            statsFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(out))
        }
    }

    private static void recursivelyAddModules(Map inModule, Map outModule) {
        if (!inModule.modules) {
           outModule.remove('modules')
           return
        }
        inModule.modules.each { m ->
            Map module = [name : frontendAlias(m.name), source: m.source, modules:[]]
            outModule.modules << module
            recursivelyAddModules(m, module)
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

    private void generateWebpackConfig(Project project) {
        String targetPath = SpringBootAction.isActive(project) ?
                '../webapp-gen/META-INF/resources/VAADIN' :
                '../webapp-gen/VAADIN'

        LOGGER.info("Bundling Javascript resources to $targetPath")

        TemplateWriter.builder()
                .targetDir(workingDir as File)
                .templateFileName('webpack.config.js')
                .substitutions([
                    'targetPath' : targetPath,
                    'moduleDirs': [
                            'src',
                            'dist/node_modules'
                    ]
                ])
                .build().write()
    }

    private void generateBuildInfo(File buildInfoFile) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        Map info = [
                'compatibilityMode': vaadin.compatibilityMode,
                'productionMode': vaadin.productionMode,
                'enableDevServer': false
        ]
        buildInfoFile.text = JsonOutput.toJson(info)
    }

    private void addFlattenScript(ClientPackage pkg, String rootPath='.') {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        File yarnInstallScript = Paths.get(project.buildDir.canonicalPath, FRONTEND, 'scripts',
                'build-dist.js').toFile()
        File distDir = new File(yarnInstallScript.parentFile.parentFile, DIST_DIR)
        yarnInstallScript.parentFile.mkdirs()
        yarnInstallScript.text = TemplateWriter.builder()
                .templateFileName('yarn-flat.js')
                .substitutions([
                    'productionMode': vaadin.productionMode ? '--production' : '',
                    'distDir': distDir.canonicalPath.replace( '\\', '\\\\'),
                    'ptyPackage': this.variant.windows ? NODE_PTY_PREBUILT : NODE_PTY
                ])
                .build().toString()

        yarnInstallScript.executable = true
        pkg.scripts[BUILD_DISTRIBUTION_COMMAND] = "$rootPath/scripts/$yarnInstallScript.name".toString()
    }

    private void resetStreams() {
        capturedOutputStream = captureOutput ? new ByteArrayOutputStream() : LogUtils.getLogOutputStream(Level.INFO)
        execOverrides = { ExecSpec spec ->
            spec.standardInput = new ByteArrayInputStream()
            spec.standardOutput = capturedOutputStream
            spec.errorOutput = LogUtils.getLogOutputStream(Level.INFO)
        }
    }

    @PackageScope
    static String frontendAlias(String path) {
        path.replaceFirst(/^\.\/src/, 'Frontend')
    }
}
