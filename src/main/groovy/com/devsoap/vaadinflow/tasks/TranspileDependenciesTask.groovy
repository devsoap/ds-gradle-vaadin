package com.devsoap.vaadinflow.tasks

import static com.devsoap.vaadinflow.models.PolymerBuild.Build
import static com.devsoap.vaadinflow.models.PolymerBuild.BuildConfiguration

import com.devsoap.vaadinflow.models.ClientPackage
import com.devsoap.vaadinflow.models.PolymerBuild
import com.moowork.gradle.node.npm.NpmExecRunner
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@Log("LOGGER")
class TranspileDependenciesTask extends DefaultTask {

    static final String NAME = 'vaadinTranspileDependencies'

    final NpmExecRunner npmExecRunner

    TranspileDependenciesTask() {
        dependsOn(InstallBowerDependenciesTask.NAME)
        description = 'Compiles client modules to support legacy browsers'
        group = 'Vaadin'

        npmExecRunner = new NpmExecRunner(project)

        this.project.afterEvaluate {
            npmExecRunner.workingDir = npmExecRunner.workingDir ?: project.node.nodeModulesDir
            if (!npmExecRunner.workingDir.exists()) {
                npmExecRunner.workingDir.mkdirs()
            }
            outputs.dir(npmExecRunner.workingDir)
        }
    }

    @TaskAction
    void run() {



        // Add polymer-cli as a script
        File pkgjson = new File(npmExecRunner.workingDir, 'package.json')
        ClientPackage pkg = new JsonSlurper().parse(pkgjson) as ClientPackage
        pkg.scripts['polymer'] = 'polymer'
        pkgjson.text = JsonOutput.prettyPrint(JsonOutput.toJson(pkg))

        // Create polymer.json
        PolymerBuild buildModel = PolymerBuild.builder()
            .entrypoint("index.html")
            .sources(['bower.json'])
            .extraDependencies(['bower_components/webcomponentsjs/webcomponents-lite.js'])
            .builds([
                new Build(name:'frontend-es5').with { js.compile = true; it },
                new Build(name:'frontend-es6')
            ])
            .build()
        File polymerjson = new File(npmExecRunner.workingDir, 'polymer.json')
        polymerjson.text = JsonOutput.prettyPrint(JsonOutput.toJson(buildModel))

        // Create index.html
        File index = new File(npmExecRunner.workingDir, 'index.html')
        if(!index.exists()){
            index.createNewFile()
        }

        // Run polymer build
        npmExecRunner.arguments = ['run', 'polymer', 'build']
        npmExecRunner.execute().assertNormalExitValue()
    }

}
