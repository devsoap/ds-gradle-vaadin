package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.creators.ComponentCreator
import com.devsoap.vaadinflow.models.Composite
import com.devsoap.vaadinflow.models.ProjectType
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Creates a new Composite Component
 *
 * Usage:
 * <code>
 *     vaadinCreateWebComponent --dependency 'bower:PolymerElements/paper-slider:v2.0.5'
 * </code>
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateCompositeTask extends DefaultTask {

    static final String NAME = 'vaadinCreateComposite'

    @Option(option = 'name', description = 'Component name')
    String componentName

    @Option(option = 'package', description = 'Component package')
    String componentPackage

    @Option(option = 'baseClass', description = 'Component base class')
    String baseClass

    private final ComponentCreator compositeCreator = new ComponentCreator()

    CreateCompositeTask() {
        description = 'Creates a new Vaadin web component'
        group = 'Vaadin'
    }

    @TaskAction
    void run() {
        componentName = componentName ?: 'ExampleComposite'
        componentPackage = componentPackage ?: 'com.example.' + project.name.toLowerCase()
        baseClass = baseClass ?: 'com.vaadin.flow.component.html.Div'

        compositeCreator.generate new Composite(
                rootDirectory: project.projectDir,
                componentBaseClass: baseClass,
                componentPackage: componentPackage,
                componentName: componentName,
                projectType: ProjectType.get(project)
        )
    }
}
