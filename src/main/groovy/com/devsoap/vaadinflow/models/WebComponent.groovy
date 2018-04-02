package com.devsoap.vaadinflow.models

import groovy.transform.builder.Builder

@Builder
class WebComponent {

    String componentName

    String componentPackage

    String componentTag

    String componentDependency

    File rootDirectory
}
