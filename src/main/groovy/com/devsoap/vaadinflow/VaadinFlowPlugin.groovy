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
package com.devsoap.vaadinflow

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.devsoap.vaadinflow.actions.GrettyDeprecatedPluginAction
import com.devsoap.vaadinflow.actions.GrettyPluginAction
import com.devsoap.vaadinflow.actions.JavaPluginAction
import com.devsoap.vaadinflow.actions.NodePluginAction
import com.devsoap.vaadinflow.actions.PluginAction
import com.devsoap.vaadinflow.actions.SassJavaPluginAction
import com.devsoap.vaadinflow.actions.SassWarPluginAction
import com.devsoap.vaadinflow.actions.SpringBootAction
import com.devsoap.vaadinflow.actions.VaadinFlowPluginAction
import com.devsoap.vaadinflow.actions.WarPluginAction
import com.devsoap.vaadinflow.extensions.DevsoapExtension
import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import com.devsoap.vaadinflow.tasks.ConvertGroovyTemplatesToHTML
import com.devsoap.vaadinflow.tasks.CreateComponentTask
import com.devsoap.vaadinflow.tasks.CreateCompositeTask
import com.devsoap.vaadinflow.tasks.CreateProjectTask
import com.devsoap.vaadinflow.tasks.CreateWebComponentTask
import com.devsoap.vaadinflow.tasks.CreateWebTemplateTask
import com.devsoap.vaadinflow.tasks.InstallBowerDependenciesTask
import com.devsoap.vaadinflow.tasks.InstallYarnDependenciesTask
import com.devsoap.vaadinflow.tasks.TranspileDependenciesTask
import com.devsoap.vaadinflow.tasks.VersionCheckTask
import com.devsoap.vaadinflow.tasks.WrapCssTask
import com.devsoap.vaadinflow.util.Versions
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.PackageScope
import groovy.util.logging.Log
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.api.invocation.Gradle
import org.gradle.internal.reflect.Instantiator
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.VersionNumber

import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.CertPathBuilderException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

/**
 * Main plugin class
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinFlowPlugin implements Plugin<Project> {

    static final String PLUGIN_ID = 'com.devsoap.vaadin-flow'

    private static final String COMPILE_CONFIGURATION = 'compile'
    private static final String LICENSE_SERVER_URL = 'https://fns.devsoap.com/t/license-server/check'
    private static final int CONNECTION_TIMEOUT = 30000
    private static final String PLUGIN_NAME = 'gradle-vaadin-flow'

    private final List<PluginAction> actions = []

    @PackageScope
    final File licenseFile

    @Inject
    VaadinFlowPlugin(Gradle gradle, Instantiator instantiator) {
        validateGradleVersion(gradle)

        actions << instantiator.newInstance(JavaPluginAction)
        actions << instantiator.newInstance(VaadinFlowPluginAction)
        actions << instantiator.newInstance(NodePluginAction)
        actions << instantiator.newInstance(WarPluginAction)
        actions << instantiator.newInstance(GrettyDeprecatedPluginAction)
        actions << instantiator.newInstance(GrettyPluginAction)
        actions << instantiator.newInstance(SpringBootAction)
        actions << instantiator.newInstance(SassJavaPluginAction)
        actions << instantiator.newInstance(SassWarPluginAction)

        licenseFile = new File(gradle.gradleUserHomeDir, '.gradle-vaadin-flow.license')
    }

    @Override
    void apply(Project project) {
        project.with {

            actions.each { action ->
                action.apply(project)
            }

            extensions.with {
                create(VaadinFlowPluginExtension.NAME, VaadinFlowPluginExtension, project)
                create(VaadinClientDependenciesExtension.NAME, VaadinClientDependenciesExtension, project)
                create(DevsoapExtension.NAME, DevsoapExtension, project)
            }

            tasks.with {
                register(CreateProjectTask.NAME, CreateProjectTask)
                register(CreateWebComponentTask.NAME, CreateWebComponentTask)
                register(InstallYarnDependenciesTask.NAME, InstallYarnDependenciesTask)
                register(InstallBowerDependenciesTask.NAME, InstallBowerDependenciesTask)
                register(TranspileDependenciesTask.NAME, TranspileDependenciesTask)
                register(AssembleClientDependenciesTask.NAME, AssembleClientDependenciesTask)
                register(WrapCssTask.NAME, WrapCssTask)
                register(CreateCompositeTask.NAME, CreateCompositeTask)
                register(CreateComponentTask.NAME, CreateComponentTask)
                register(CreateWebTemplateTask.NAME, CreateWebTemplateTask)
                register(ConvertGroovyTemplatesToHTML.NAME, ConvertGroovyTemplatesToHTML)
                register(VersionCheckTask.NAME, VersionCheckTask)
            }

            afterEvaluate {
                validateLicense(project)
                disableStatistics(project)
                enableProductionMode(project)
                validateVaadinVersion(project)
            }
        }
    }

    final boolean isValidLicense(Project project) {
        DevsoapExtension devsoap = project.extensions.getByType(DevsoapExtension)
        if (!devsoap.email || !devsoap.key) {
            return false
        }
        licenseFile.exists() && verifySignature(licenseFile.text, devsoap.key)
    }

    private static void disableStatistics(Project project) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        if (!vaadin.submitStatistics) {
            Dependency statistics = vaadin.disableStatistics()
            project.configurations[COMPILE_CONFIGURATION].dependencies.add(statistics)
            project.configurations.all { DefaultConfiguration config ->
                config.resolutionStrategy.force("${statistics.group}:${statistics.name}:${statistics.version}")
            }
        }
    }

    private static void enableProductionMode(Project project) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        if (vaadin.productionMode) {
            Dependency productionMode = vaadin.enableProductionMode()
            project.configurations[COMPILE_CONFIGURATION].dependencies.add(productionMode)
        }
    }

    private static void validateGradleVersion(Gradle gradle) {
        VersionNumber version = VersionNumber.parse(gradle.gradleVersion)
        VersionNumber requiredVersion = Versions.version('vaadin.plugin.gradle.version')
        if ( version.baseVersion < requiredVersion ) {
            throw new UnsupportedVersionException("Your gradle version ($version) is too old. " +
                    "Plugin requires Gradle $requiredVersion+")
        }
    }

    private static void validateVaadinVersion(Project project) {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)
        if (vaadin.unSupportedVersion) {
            LOGGER.severe(
                    "The Vaadin version ($vaadin.version) you have selected is not supported by the plugin. " +
                            'Since vaadin.unsupportedVersion is set to True, continuing anyway. You are on your own.')

        } else if (!vaadin.isSupportedVersion()) {
            String[] supportedVersions = Versions.rawVersion('vaadin.supported.versions').split(',')
            throw new UnsupportedVersionException(
                    "The Vaadin version ($vaadin.version) you have selected is not supported by the plugin. " +
                        "Please pick one of the following supported Vaadin versions $supportedVersions. " +
                        'Alternatively you can add vaadin.unsupportedVersion=true to your build.gradle to override ' +
                        'this check but there is no guarantee it will work or that the build will be stable.')
        }
    }

    private static void validateLicense(Project project) {
        DevsoapExtension devsoap = project.extensions.getByType(DevsoapExtension)
        if (!devsoap.email || !devsoap.key) {
            LOGGER.info('No license email or key defined, skipping license check.')
            return
        }

        VaadinFlowPlugin plugin = project.plugins.getPlugin(VaadinFlowPlugin)
        if (plugin.isValidLicense(project)) {
            LOGGER.info('Offline license key verified.')
            return
        }

        LOGGER.info('Fetching license information...')

        try {
            Map<String,String> payload = [
                    'product': PLUGIN_NAME,
                    'email': devsoap.email.toString(),
                    'key': devsoap.key.toString()
            ]
            InputStream stream = callLicenseServer(payload)
            Object response = new JsonSlurper().parse(stream)
            if (response?.result == 'OK') {
                String token = response.data.signature
                if (verifySignature(token, devsoap.key)) {
                    plugin.licenseFile.text = token
                } else {
                    LOGGER.info('License signature verification failed. Are you using a valid license?')
                }
            } else {
              LOGGER.info('Failed response from license server, response: ' + response)
            }
        } catch (SocketTimeoutException | SSLHandshakeException | CertPathBuilderException e) {
            LOGGER.warning('Validating license failed, failed to contact license server.')
            LOGGER.warning(e.message)
        }
    }

    private static boolean verifySignature(String jwtToken, String productKey) {
        String signature = Versions.rawVersion('vaadin.plugin.signature')
        Algorithm algorithm = Algorithm.HMAC256("${PLUGIN_NAME}.${productKey}.${signature}")
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer('Devsoap Inc.')
                .withClaim('key', productKey)
                .withClaim('product', PLUGIN_NAME)
                .acceptLeeway(TimeUnit.HOURS.toSeconds(12))
                .build()
        try {
            verifier.verify(jwtToken)
            return true
        } catch (JWTVerificationException e) {
            LOGGER.warning(e.message)
        }
        false
    }

    private static InputStream callLicenseServer(Map payload) {
        String body = JsonOutput.toJson(payload)
        HttpsURLConnection connection = (HttpsURLConnection) LICENSE_SERVER_URL.toURL().openConnection()
        connection.with {
            it.SSLSocketFactory = trustAllSslSocketFactory()
            it.doOutput = true
            it.requestMethod = 'POST'
            it.connectTimeout = CONNECTION_TIMEOUT
            it.readTimeout = CONNECTION_TIMEOUT
            it.outputStream.withWriter { writer -> writer.write(body) }
            it
        }.inputStream
    }

    // FIXME Replace with real certificate
    @SuppressWarnings('UnusedMethodParameter')
    private static SSLSocketFactory trustAllSslSocketFactory() throws Exception {
        TrustManager[] allTM = [ new X509TrustManager() {
            @Override X509Certificate[] getAcceptedIssuers() { new X509Certificate[0] }
            @Override void checkClientTrusted(X509Certificate[] chain, String authType) { }
            @Override void checkServerTrusted(X509Certificate[] chain, String authType) { }
        }]
        SSLContext sslContext = SSLContext.getInstance('TLS')
        sslContext.init(null, allTM , new SecureRandom())
        sslContext.socketFactory
    }

}
