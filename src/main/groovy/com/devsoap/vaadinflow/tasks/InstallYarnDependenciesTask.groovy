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
package com.devsoap.vaadinflow.tasks

import static com.devsoap.vaadinflow.util.ClassIntrospectionUtils.findNpmPackages
import static com.devsoap.vaadinflow.util.ClassIntrospectionUtils.getAnnotationScan

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.models.PackageJson
import com.devsoap.vaadinflow.util.ClientPackageUtils
import com.devsoap.vaadinflow.util.VaadinYarnRunner
import com.devsoap.vaadinflow.util.WebJarHelper
import com.moowork.gradle.node.yarn.YarnSetupTask
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Installs yarn dependencies into the webapp frontend
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
@CacheableTask
class InstallYarnDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinInstallClientDependencies'

    private static final String PACKAGE_JSON = 'package.json'
    private static final String NODE_MODULES = 'node_modules'

    protected final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)
    protected final File distDir = new File(workingDir, 'dist')

    protected final VaadinYarnRunner yarnRunner = new VaadinYarnRunner(project, workingDir)

    @OutputFile
    final File appPackageJson = new File(distDir, PACKAGE_JSON)

    @OutputFile
    final File packageJson = new File(workingDir, PACKAGE_JSON)

    @OutputFile
    final File yarnrc = new File(workingDir, '.yarnrc')

    @OutputFile
    final File yarnLock = new File(workingDir, 'yarn.lock')

    @OutputDirectory
    final File appNodeModules = new File(distDir, NODE_MODULES)

    @OutputDirectory
    final File nodeModules = new File(workingDir, NODE_MODULES)

    @OutputDirectory
    final File srcDir = new File(workingDir, 'src')

    /**
     * Creates an installation task
     */
    InstallYarnDependenciesTask() {
        dependsOn( YarnSetupTask.NAME, 'classes' )
        onlyIf {
            VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            if (vaadin.compatibilityMode) {
                (!client.yarnDependencies.empty || !client.bowerDependencies.empty || client.compileFromSources)
            } else {
                true
            }
        }

        description = 'Installs Vaadin yarn dependencies'
        group = 'Vaadin'

        inputs.property('compileFromSources') {
            project.extensions.getByType(VaadinClientDependenciesExtension).compileFromSources
        }

        inputs.property('yarnDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).yarnDependencies
        }

        inputs.property('bowerDependencies') {
            project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies
        }
    }

    /**
     * Default action. Runs the task.
     */
    @TaskAction
    void run() {
        LOGGER.info('Creating root package.json...')
        yarnRunner.init()

        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)

        if (!vaadin.compatibilityMode) {
            LOGGER.info('Create dist package.json...')
            yarnRunner.initDist()
        }

        JsonSlurper json = new JsonSlurper()
        PackageJson pkg = vaadin.compatibilityMode ?
                json.parse(packageJson) as PackageJson : json.parse(appPackageJson) as PackageJson

        pkg.vaadin = ['disableUsageStatistics': !vaadin.submitStatistics]

        pkg.dependencies = pkg.dependencies ?: [:]

        // Add classpath dependencies to package.json
        findNpmPackages(getAnnotationScan(project)).each { String name, String version ->
            pkg.dependencies[name] = version
        }

        // Add dependencies to package.json
        VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)
        deps.yarnDependencies.each { String name, String version ->
            pkg.dependencies[name] = version
        }

        if (vaadin.compatibilityMode) {
            packageJson.text = ClientPackageUtils.toJson(pkg)
        } else {

            // FIXME Vaadin RTA does not include buffer as transitive dependency. This is most likely a bug in RTA.
            pkg.dependencies.putIfAbsent 'buffer', '4.9.1'

            appPackageJson.text = ClientPackageUtils.toJson(pkg)
        }

        LOGGER.info('Installing development dependencies...')
        yarnRunner.install()

        LOGGER.info('Extracting node webjars...')
        WebJarHelper.unpackWebjars(workingDir, srcDir, project, appNodeModules.name,
                false, vaadin.compatibilityMode)

        if (vaadin.compatibilityMode) {
            LOGGER.info('Validating node modules...')
            List<String> imports = ClientPackageUtils.findHTMLImportsFromComponents(
                    appNodeModules, null, workingDir)
            deps.yarnDependencies.keySet().each { dep ->
                if (imports.findAll { it.contains(dep.split('/').last()) }.isEmpty()) {
                    logger.error('HTML entrypoint file for {} not found, is it a Polymer 2 component?', dep)
                }
            }
        } else {
            LOGGER.info('Installing application dependencies...')
            yarnRunner.distInstall()
        }
    }
}
