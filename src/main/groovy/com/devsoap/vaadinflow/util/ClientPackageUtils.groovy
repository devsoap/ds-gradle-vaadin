/*
 * Copyright 2018-2020 Devsoap Inc.
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0
 * International Public License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      https://creativecommons.org/licenses/by-nd/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.vaadinflow.util

import groovy.json.JsonException
import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log

import java.util.logging.Level

/**
 * Utilities for handling bower and npm modules
 *
 * @author John Ahlroos
 * @since 1.1
 */
@Log('LOGGER')
class ClientPackageUtils {

    private static final String BOWER_JSON = 'bower.json'
    private static final String PACKAGE_JSON = 'package.json'
    private static final String HTML_FILE_TYPE = '.html'
    private static final String CSS_FILE_TYPE = '.css'
    private static final String NOT_INCLUDED_WARNING = 'module will not be included in production mode bundle.'

    /**
     * Searches for HTML imports in module directories
     *
     * @param nodeModules
     *      the node module directories
     * @param bowerComponents
     *      the bower module directories
     * @param workingDir
     *      the working directory
     * @return
     *      a list of imports
     */
    static List<String> findHTMLImportsFromComponents(File nodeModules, File bowerComponents, File workingDir) {
        List<String> imports = []

        List<File> scanDirs = []
        if (nodeModules && nodeModules.exists()) {
            scanDirs.add(nodeModules)
        }
        if (bowerComponents && bowerComponents.exists()) {
            scanDirs.add(bowerComponents)
        }

        scanDirs.each {

            LOGGER.info("Searching for html imports in $it")
            it.eachDir { dir ->
                File bowerJsonFile = new File(dir, '.bower.json')
                if (!bowerJsonFile.exists()) {
                    bowerJsonFile = new File(dir, ClientPackageUtils.BOWER_JSON)
                }
                if (!bowerJsonFile.exists()) {
                    bowerJsonFile = new File(dir, ClientPackageUtils.PACKAGE_JSON)
                }
                if (bowerJsonFile.exists()) {
                    try {
                        Object bowerJson = new JsonSlurper().parse(bowerJsonFile)

                        List<String> entrypoints = []
                        if (bowerJson.main instanceof List) {
                            entrypoints.addAll(bowerJson.main as List)
                        } else {
                            entrypoints.add(bowerJson.main as String)
                        }

                        entrypoints = entrypoints.findAll {
                            it?.endsWith(ClientPackageUtils.HTML_FILE_TYPE) ||
                            it?.endsWith(ClientPackageUtils.CSS_FILE_TYPE)
                        }

                        if (entrypoints.isEmpty()) {
                            LOGGER.fine("No entry-point found for ${bowerJson.name}, $NOT_INCLUDED_WARNING")
                        }

                        entrypoints.each {
                            File resourceFile = new File(dir, it)
                            String path = (resourceFile.path - workingDir.path).substring(1)
                            imports.add(path)
                        }
                    } catch (JsonException je) {
                        LOGGER.warning("Failed to parse $bowerJsonFile")
                        LOGGER.warning(je.message)
                    }
                } else {
                    LOGGER.info("No bower.json or package.json found in $dir, $NOT_INCLUDED_WARNING")
                }
            }
        }
        imports
    }

    /**
     * Converts an object into a pretty-printed JSON string
     *
     * @since 1.3
     * @param obj
     *      the object to convert
     * @return
     *      json string
     */
    static String toJson(Object obj) {
        JsonOutput.prettyPrint( new JsonGenerator.Options().excludeNulls().build().toJson(obj))
    }

}
