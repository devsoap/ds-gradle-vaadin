package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.creators.WebComponentCreator
import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.models.WebComponent
import com.moowork.gradle.node.yarn.YarnExecRunner
import com.moowork.gradle.node.yarn.YarnSetupTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

class CreateWebComponentTask extends DefaultTask {

    static final String NAME = 'vaadinCreateWebComponent'

    @Option(option = 'name', description = 'Component name')
    String componentName

    @Option(option = 'package', description = 'Component package')
    String componentPackage

    @Option(option = 'tag', description = 'Component tag')
    String componentTag

    @Option(option = 'dependency', description = 'Component dependency')
    String componentDependency


    /**
     * Runner to run yarn tasks
     */
    final YarnExecRunner yarnRunner

    private final WebComponentCreator webComponentCreator = new WebComponentCreator()


    CreateWebComponentTask() {
        dependsOn( YarnSetupTask.NAME )

        description = 'Creates a new Vaadin web component'
        group = 'Vaadin'

        yarnRunner = new YarnExecRunner(project)

        this.project.afterEvaluate {
            yarnRunner.workingDir = yarnRunner.workingDir ?: project.node.nodeModulesDir
            if (!yarnRunner.workingDir.exists()) {
                yarnRunner.workingDir.mkdirs()
            }
        }
    }

    @TaskAction
    void run() {

        componentName = componentName ?: 'ExampleWebComponent'
        componentPackage = componentPackage ?: 'com.example.' + project.name.toLowerCase()
        componentTag = componentTag ?: componentName.replaceAll(/\B[A-Z]/) { '-' + it }.toLowerCase()
        componentDependency = componentDependency ?: componentTag

        WebComponent component = WebComponent
                .builder()
                .componentName(componentName)
                .componentPackage(componentPackage)
                .componentTag(componentTag)
                .componentDependency(componentDependency)
                .rootDirectory(project.rootDir)
                .build()

        webComponentCreator.generate(component)

        File buildFile = project.file('build.gradle')
        buildFile << """
            ${VaadinClientDependenciesExtension.NAME}.yarn('${componentDependency}')
        """.stripMargin()

        yarnRunner.arguments = ['add', componentDependency.replace(':', '@')]
        yarnRunner.execute().assertNormalExitValue()
    }

}
