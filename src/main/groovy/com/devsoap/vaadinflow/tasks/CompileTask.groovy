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
package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.models.VaadinCompileResolver
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction

/**
 * Task for compiling client side Javascript
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class CompileTask extends DefaultTask {

    static final String NAME = 'vaadinCompile'

    private final Property<VaadinCompileResolver> resolver = project.objects.property(VaadinCompileResolver)

    CompileTask() {
        description = 'Compiles the client Javascript if needed'
        resolver.set(VaadinCompileResolver.WEBJAR)
        onlyIf { VaadinCompileResolver.WEBJAR.name().toLowerCase() != clientDependencyResolver }
        mustRunAfter('processResources')
    }

    @TaskAction
    void run() {
        // FIXME in #11
        LOGGER.warning('Compile task has not been implemented!')
    }

    /**
     * Get the client dependency resolver
     */
    String getClientDependencyResolver() {
        resolver.get().name().toLowerCase()
    }

    /**
     * Set the client dependency resolver
     *
     * @param resolver
     *      the resolver to use, see VaadinCompileResolver for possible values
     */
    void setClientDependencyResolver(String resolver) {
        Objects.requireNonNull(resolver, 'Resolver cannot be null')
        if (project.extensions.getByType(VaadinFlowPluginExtension).dependenciesApplied) {
            throw new GradleException('vaadin.clientDependencyResolver cannot be ' +
                    'set after dependencies have been added')
        }

        try {
            this.resolver.set(VaadinCompileResolver.valueOf(resolver.toUpperCase()))
        } catch (IllegalArgumentException e) {
            throw new GradleException("'$resolver' is not a valid dependency resolver")
        }

    }
}
