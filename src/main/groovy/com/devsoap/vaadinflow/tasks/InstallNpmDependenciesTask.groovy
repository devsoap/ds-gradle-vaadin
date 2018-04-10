package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.models.ClientPackage
import com.moowork.gradle.node.npm.NpmExecRunner
import com.moowork.gradle.node.npm.NpmSetupTask
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@Log('LOGGER')
class InstallNpmDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinInstallNpmDependencies'

    final NpmExecRunner npmExecRunner = new NpmExecRunner(project)

    @OutputDirectory
    final File workingDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    @OutputFile
    @InputFile
    final File packageJson = new File(workingDir, 'package.json')

    InstallNpmDependenciesTask() {
        dependsOn( NpmSetupTask.NAME )
        onlyIf {
            !project.extensions.getByType(VaadinClientDependenciesExtension).bowerDependencies.empty ||
            !project.extensions.getByType(VaadinClientDependenciesExtension).yarnDependencies.empty
        }

        description = 'Installs Vaadin npm client dependencies'
        group = 'Vaadin'

        npmExecRunner.workingDir = workingDir
    }

    @TaskAction
    void run() {

        // Create package.json
        npmExecRunner.arguments = ['init', '-y', '-f']
        npmExecRunner.execute().assertNormalExitValue()

        // Set proper defaults for package.json
        ClientPackage pkg = new JsonSlurper().parse(packageJson) as ClientPackage
        pkg.main = ''
        pkg.version = '1.0.0'
        pkg.name = project.name + '-frontend'

        // Install bower
        LOGGER.info('Installing Bower ...')
        npmExecRunner.arguments = ['install', 'bower', '--save-dev']
        npmExecRunner.execute().assertNormalExitValue()
        pkg.scripts['bower'] = 'bower'

        // Install polymer-build
        LOGGER.info('Installing polymer-build ...')
        npmExecRunner.arguments = ['install', 'polymer-cli@1.5.2', '--save-dev']
        npmExecRunner.execute().assertNormalExitValue()
        pkg.scripts['polymer'] = 'polymer'

        // Generate package.json
        LOGGER.info('Generating package.json...')
        packageJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))
    }
}
