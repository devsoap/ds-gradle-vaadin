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
package com.devsoap.vaadinflow.extensions

import org.gradle.api.Project

/**
 * Extension for configuring the client dependencies
 *
 * @author John Ahlroos
 * @since 1.0
 */
class VaadinClientDependenciesExtension {

    static final String NAME = 'vaadinClientDependencies'

    static final String FRONTEND_DIR = 'src/main/webapp/frontend'

    private final Map<String, String> yarnDependencies = [:]

    private final Project project

    /**
     * Creates the extension
     *
     * @param project
     *      the project to apply the extension to
     */
    VaadinClientDependenciesExtension(Project project) {
        this.project = project
    }

    /**
     * Add a Yarn dependency using the compact notation
     *
     * @param dependencyNotation
     *      the dependency using the "<dependency-name>:<dependency-version>" notation
     */
    void yarn(String dependencyNotation) {
        dependencyNotation.split(':').with {
            yarn(it.first(), it.last())
        }
    }

    /**
     * Add a Yarn dependency
     *
     * @param dependency
     *      the dependency name
     * @param version
     *      the dependency version
     */
    void yarn(String dependency, String version) {
        yarnDependencies[dependency] = version
    }

    /**
     * Retrieve the configured Yarn dependencies
     *
     * @return
     *      a map with the dependency name as key and version as value
     */
    Map<String,String> getYarnDependencies() {
        Collections.unmodifiableMap(yarnDependencies)
    }

}
