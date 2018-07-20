/*
 * Copyright 2018 Devsoap Inc.
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
package com.devsoap.vaadinflow.models

/**
 * TemplateModel for representing a polymer build (polymer.json)
 *
 * @author John Ahlroos
 * @since 1.0
 */
class PolymerBuild {

    String entrypoint

    List<String> sources = []

    List<String> extraDependencies = []

    List<Build> builds = []

    /**
     * Represents a single build in the build process.
     */
    interface Build { }

    static class CustomBuild implements Build {
        String name
        boolean bundle = true
        BuildConfiguration js = new BuildConfiguration()
        BuildConfiguration css = new BuildConfiguration()
        BuildConfiguration html = new BuildConfiguration()
    }

    static class BuildPreset implements Build {
        String preset
    }

    /**
     * Represents a jss/css/html build configuration.
     */
    static class BuildConfiguration {

        boolean minify = true

        boolean compile = false
    }

    static final BuildPreset ES5_BUNDLED = new BuildPreset(preset: 'es5-bundled')
    static final BuildPreset ES6_BUNDLED = new BuildPreset(preset: 'es6-bundled')
    static final BuildPreset ES6_UNBUNDLED = new BuildPreset(preset: 'es6-unbundled')
}
