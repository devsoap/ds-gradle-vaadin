package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class AssembleClientDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinAssembleClient'

    @InputDirectory
    final File sourceDir = project.file(VaadinClientDependenciesExtension.FRONTEND_BUILD_DIR)

    @OutputDirectory
    final File targetDir = project.file(VaadinClientDependenciesExtension.WEBAPP_DIR)

    AssembleClientDependenciesTask() {
        dependsOn(TranspileDependenciesTask.NAME)
        group = 'Vaadin'
        description = 'Copies built client dependencies into the right target directory'
    }

    @TaskAction
    void run() {

        List<String> excludes = [
             '**/LICENSE*',
             '**/demo/**',
             '**/docs/**',
             '**/test*/**',
             '**/.*',
             '**/*.md',
             '**/bower.json',
             '**/package.json',
             '**/package-lock.json'
        ]

        project.copy { spec -> spec
                .from(sourceDir)
                .include('index.html', 'bower_components/**')
                .exclude(excludes)
                .into(new File(targetDir, 'frontend'))
        }

        project.copy { spec -> spec
                .from(new File(sourceDir, 'build'))
                .include('frontend*/**')
                .exclude(excludes)
                .into(targetDir)
        }
    }


}
