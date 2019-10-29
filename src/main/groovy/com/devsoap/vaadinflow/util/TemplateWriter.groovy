/*
 * Copyright 2018-2019 Devsoap Inc.
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

import groovy.text.SimpleTemplateEngine
import groovy.text.TemplateEngine
import groovy.transform.Memoized
import groovy.transform.builder.Builder
import groovy.util.logging.Log

/**
 * Builder for creating writers for writing a template to the file system
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Builder
@Log('LOGGER')
class TemplateWriter {

    String templateFileName

    File targetDir

    String targetFileName

    Map substitutions

    /**
     * Writes the template to the file-system
     *
     * @return
     *     the file of the generated template
     */
    File write() {
        Objects.nonNull(templateFileName)
        Objects.nonNull(targetDir)

        targetFileName = targetFileName ?: templateFileName
        substitutions = substitutions ?: [:]

        writeTemplateFromString(generateTemplate())
    }

    /**
     * Get template as String
     *
     * @return
     *      the template result as a string
     */
    String toString() {
        Objects.nonNull(templateFileName)
        Objects.nonNull(targetDir)

        targetFileName = targetFileName ?: templateFileName
        substitutions = substitutions ?: [:]

        generateTemplate()
    }

    private String generateTemplate() {
        URL templateUrl = TemplateWriter.classLoader.getResource("templates/${templateFileName}.template")
        if ( !templateUrl ) {
            throw new FileNotFoundException("Could not find template 'templates/${templateFileName}.template'")
        }

        TemplateEngine engine = new SimpleTemplateEngine()
        engine.createTemplate(templateUrl).make(substitutions.withDefault { null })
    }

    private File writeTemplateFromString(String templateContent) {
        File targetFile = new File(targetDir, targetFileName)
        if ( !targetFile.exists() ) {
            targetFile.parentFile.mkdirs()
            targetFile.createNewFile()
        }

        if ( !targetFile.canWrite() ) {
            throw new FileNotFoundException("Could not write to target file $targetFile.canonicalPath")
        }

        LOGGER.info "Writing template $templateFileName to ${targetFile.canonicalPath}"
        targetFile.write(templateContent)
        targetFile
    }

    /**
     * Ensures that the string can be used as a Java Class Name
     */
    static String makeStringJavaCompatible(String string) {
        if (!string) {
            return null
        }
        List<Character> collector = []
        List<String> collector2 = []
        string.chars.collect(collector) { ch ->
            if (collector.empty) {
                Character.isJavaIdentifierStart(ch) ? ch : ''
            } else {
                Character.isJavaIdentifierPart(ch) ? ch : ' '
            }
        }.join('').tokenize().collect(collector2) { t ->
            collector2.empty ? t : t.capitalize()
        }.join('')
    }
}
