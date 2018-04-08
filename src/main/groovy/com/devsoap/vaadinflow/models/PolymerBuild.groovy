package com.devsoap.vaadinflow.models

import groovy.transform.builder.Builder

@Builder
class PolymerBuild {

    String entrypoint

    List<String> sources = []

    List<String> extraDependencies = []

    List<Build> builds = []

    static class Build {

        String name

        BuildConfiguration js = new BuildConfiguration()

        BuildConfiguration css = new BuildConfiguration()

        BuildConfiguration html = new BuildConfiguration()
    }

    static class BuildConfiguration {

        boolean minify = true

        boolean compile = false
    }
}
