/*
 * Copyright 2018-2019 Devsoap Inc.
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

import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.variant.Variant
import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher

/**
 *  A modified version of com.moowork.gradle.node.task.SetupTask
 *
 * <b>Note:</b> This class is internal and might be removed if the upstream plugin is updated. Do not rely on this
 * class to exist.
 *
 *  @author John Ahlroos
 *  @since 1.1
 */
@Internal
class NodeSetupTask extends DefaultTask {

    final static String NAME = 'nodeSetup'

    private static final String IVY_ARTIFACT_PATTERN = 'v[revision]/[artifact](-v[revision]-[classifier]).[ext]'
    private static final String IVY_XML_PATH = 'v[revision]/ivy.xml'

    private NodeExtension config

    protected Variant variant

    private IvyArtifactRepository repo

    private List<ArtifactRepository> allRepos

    NodeSetupTask() {
        this.group = 'Node'
        this.description = 'Download and install a local node/npm version.'
        this.enabled = false
    }

    @Input
    Set<String> getInput() {
        configureIfNeeded()
        Set<String> set =[]
        set.add( Boolean.toString(config.download) )
        set.add( variant.archiveDependency )
        set.add( variant.exeDependency )
        set
    }

    @OutputDirectory
    File getNodeDir() {
        configureIfNeeded()
        this.variant.nodeDir
    }

    @TaskAction
    void run() {
        configureIfNeeded()
        addRepository()

        if ( this.variant.exeDependency ) {
            copyNodeExe()
        }

        deleteExistingNode()
        unpackNodeArchive()
        setExecutableFlag()
        restoreRepositories()
    }

    private void configureIfNeeded() {
        if ( config != null ) {
            return
        }
        config = NodeExtension.get( this.project )
        variant = config.variant
    }

    private void copyNodeExe() {
        project.copy {
            from nodeExeFile
            into variant.nodeBinDir
            rename 'node.+\\.exe', 'node.exe'
        }
    }

    private void deleteExistingNode() {
        project.delete( nodeDir.parent )
    }

    private void unpackNodeArchive() {
        if ( nodeArchiveFile.name.endsWith( 'zip' ) ) {
            project.copy {
                from project.zipTree( nodeArchiveFile )
                into nodeDir.parent
            }
        } else if ( variant.exeDependency )  {
            // Remap lib/node_modules to node_modules (the same directory as node.exe) because
            // that's how the zip dist does it
            project.copy {
                from project.tarTree( nodeArchiveFile )
                into variant.nodeBinDir
                eachFile {
                    Matcher m = it.path =~ /^.*?[\\/]lib[\\/](node_modules.*$)/
                    if ( m.matches() ) {
                        // remap the file to the root
                        it.path = m.group( 1 )
                    } else {
                        it.exclude()
                    }
                }
                includeEmptyDirs = false
            }
        }
        else {
            project.copy {
                from project.tarTree( nodeArchiveFile )
                into nodeDir.parent
            }
            // Fix broken symlink
            Path npm = Paths.get( variant.nodeBinDir.path, 'npm' )
            if ( Files.deleteIfExists(npm) ) {
                Files.createSymbolicLink(
                        npm,
                        variant.nodeBinDir.toPath().relativize(Paths.get(variant.npmScriptFile)))
            }
        }
    }

    private void setExecutableFlag() {
        if (!this.variant.windows) {
            new File( this.variant.nodeExec ).setExecutable( true )
        }
    }

    protected File getNodeExeFile() {
        resolveSingle( variant.exeDependency )
    }

    protected File getNodeArchiveFile() {
        resolveSingle( variant.archiveDependency )
    }

    private File resolveSingle( String name ) {
        Dependency dep = project.dependencies.create( name )
        Configuration conf = project.configurations.detachedConfiguration( dep )
        conf.transitive = false
        conf.resolve().iterator().next()
    }

    private void addRepository() {
        allRepos = []
        allRepos.addAll( project.repositories )
        project.repositories.clear()

        String distUrl = config.distBaseUrl
        repo = project.repositories.ivy {
            url distUrl
            if (IvyArtifactRepository.metaClass.respondsTo(this, 'patternLayout', Closure)) {
                //Gradle 5.3 ->
                patternLayout {
                    artifact NodeSetupTask.IVY_ARTIFACT_PATTERN
                    ivy NodeSetupTask.IVY_XML_PATH
                }
            } else {
                layout 'pattern', {
                    artifact NodeSetupTask.IVY_ARTIFACT_PATTERN
                    ivy NodeSetupTask.IVY_XML_PATH
                }
            }
        }
    }

    private void restoreRepositories() {
        project.repositories.clear()
        project.repositories.addAll(allRepos )
    }
}
