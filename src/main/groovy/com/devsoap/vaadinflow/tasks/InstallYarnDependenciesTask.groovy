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
package com.devsoap.vaadinflow.tasks

import static com.devsoap.vaadinflow.util.ClassIntrospectionUtils.findNpmPackages
import static com.devsoap.vaadinflow.util.ClassIntrospectionUtils.getAnnotationScan

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.models.ClientPackage
import com.devsoap.vaadinflow.util.ClientPackageUtils
import com.devsoap.vaadinflow.util.VaadinYarnRunner
import com.devsoap.vaadinflow.util.WebJarHelper
import com.devsoap.vaadinflow.util.Versions
import com.moowork.gradle.node.yarn.YarnSetupTask
import groovy.json.JsonOutput
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

    static final String NAME = 'vaadinInstallYarnDependencies'

    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    final VaadinYarnRunner yarnRunner = new VaadinYarnRunner(project, workingDir)

    @OutputFile
    final File packageJson = new File(workingDir, 'package.json')

    @OutputFile
    final File yarnrc = new File(workingDir, '.yarnrc')

    @OutputFile
    final File yarnLock = new File(workingDir, 'yarn.lock')

    @OutputDirectory
    final File nodeModules = new File(workingDir, 'node_modules')

    /**
     * Creates an installation task
     */
    InstallYarnDependenciesTask() {
        dependsOn( YarnSetupTask.NAME, 'classes' )
        onlyIf {
            VaadinClientDependenciesExtension client = project.extensions.getByType(VaadinClientDependenciesExtension)
            !client.yarnDependencies.empty ||
            !client.bowerDependencies.empty ||
            client.compileFromSources //||
            //!findNpmPackages(getAnnotationScan(project)).empty
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

        //inputs.property('npmDependencies') {
         //   findNpmPackages(getAnnotationScan(project))
        //}
    }

    /**
     * Default action. Runs the task.
     */
    @TaskAction
    void run() {
        LOGGER.info('Creating package.json ...')
        yarnRunner.init()

        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage

        // Requirement for building Vaadin 14+
        pkg.dependencies['@webcomponents/webcomponentsjs'] = Versions.rawVersion('webcomponentsjs.version')

        // Add classpath dependencies to package.json
        findNpmPackages(getAnnotationScan(project)).each { String name, String version ->
            pkg.dependencies[name] = version
        }

        // Add dependencies to package.json
        VaadinClientDependenciesExtension deps = project.extensions.getByType(VaadinClientDependenciesExtension)
        deps.yarnDependencies.each { String name, String version ->
            pkg.dependencies[name] = version
        }

        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))

        LOGGER.info('Installing yarn dependencies...')
        yarnRunner.install()

        LOGGER.info('Extracting node webjars...')
        WebJarHelper.unpackWebjars(workingDir, null, project, nodeModules.name, false)

        LOGGER.info('Validating node modules...')
        List<String> imports = ClientPackageUtils.findHTMLImportsFromComponents(
                nodeModules, null, workingDir)
        deps.yarnDependencies.keySet().each { dep ->
            if ( imports.findAll { it.contains( dep.split('/').last()) }.isEmpty()) {
                logger.error('HTML entrypoint file for {} not found, is it a Polymer 2 component?', dep)
            }
        }
    }
}
