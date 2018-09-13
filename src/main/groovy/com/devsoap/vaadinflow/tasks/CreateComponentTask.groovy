package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.creators.ComponentCreator
import com.devsoap.vaadinflow.models.Component
import com.devsoap.vaadinflow.models.Composite
import com.devsoap.vaadinflow.models.ProjectType
import com.devsoap.vaadinflow.util.PathUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Creates a new Vaadin Component
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateComponentTask extends DefaultTask {

    static final String NAME = 'vaadinCreateComponent'

    @Option(option = 'name', description = 'Component name')
    String componentName

    @Option(option = 'package', description = 'Component package')
    String componentPackage

    @Option(option = 'tag', description = 'Component tag')
    String componentTag

    private final ComponentCreator componentCreator = new ComponentCreator()

    CreateComponentTask() {
        description = 'Creates a new Vaadin component'
        group = 'Vaadin'
    }

    @TaskAction
    void run() {
        componentName = componentName ?: 'ExampleTextField'
        componentPackage = componentPackage ?: 'com.example.' + project.name.toLowerCase()
        componentTag = componentTag ?: 'input'

        componentCreator.generate new Component(
            rootDirectory : PathUtils.getSubmoduleSensitiveProjectRootDir(project),
            componentPackage : this.componentPackage,
            componentName : this.componentName,
            componentTag : componentTag,
            projectType: ProjectType.get(project)
        )
    }
}
