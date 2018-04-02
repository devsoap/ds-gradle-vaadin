package com.devsoap.vaadinflow.creators

import com.devsoap.vaadinflow.models.WebComponent
import com.devsoap.vaadinflow.util.TemplateWriter

import java.nio.file.Paths

class WebComponentCreator {

    void generate(WebComponent webComponent) {

        File root = webComponent.rootDirectory
        File javaSourceDir = Paths.get(root.canonicalPath, 'src', 'main', 'java').toFile()
        File pkgDir = Paths.get(javaSourceDir.canonicalPath, webComponent.componentPackage.split('\\.')).toFile()
        String componentClassName = TemplateWriter.makeStringJavaCompatible(webComponent.componentName)

        TemplateWriter.builder()
                .targetDir(pkgDir)
                .templateFileName('WebComponent.java')
                .targetFileName("${componentClassName}.java")
                .substitutions([
                    'componentPackage' : webComponent.componentPackage,
                    'componentTag' : webComponent.componentTag,
                    'componentDependency' : webComponent.componentDependency,
                    'componentName' : componentClassName
                ]).build().write()

    }
}
