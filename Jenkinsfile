pipeline {
  agent {
    label 'linux'
  }

  parameters {
     string(name: 'buildVersion',  description: 'Build version', defaultValue: '1.0-SNAPSHOT-${BUILD_NUMBER}')
     booleanParam(name: 'publish', description: 'Should the build be published to the Plugin Portal', defaultValue: false)
     booleanParam(name: 'documentation', description: 'Should the build create API documentation', defaultValue: false)
  }

  environment {
     GRADLE_PUBLISH_KEY = credentials('GRADLE_PUBLISH_KEY')
     GRADLE_PUBLISH_SECRET = credentials('GRADLE_PUBLISH_SECRET')
  }

  stages {
    stage('Build') {
      steps {
        sh "./gradlew assemble -PBUILD_VERSION=${params.buildVersion}"
      }
    }

    stage('Publish') {
      when {                
        expression { params.publish }
      }
      steps {     
        sh "./gradlew publishPlugins -PBUILD_VERSION=${params.buildVersion} -Pgradle.publish.key=${env.GRADLE_PUBLISH_KEY} -Pgradle.publish.secret=${env.GRADLE_PUBLISH_SECRET}"
        archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
      }
    }

    stage('Build Documentation') {
      when {
        expression { params.documentation }
      }
      steps {
        sh "./gradlew groovyDoc -PBUILD_VERSION=${params.buildVersion}"
      }
    }

    stage('Publish Documentation') {
      when {
        expression { params.documentation }
      }
      steps {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'GithubID', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
          sh "rm -rf /tmp/docs; git clone --depth=1 https://github.com/devsoap/docs.git /tmp/docs"
          sh "cp -R build/docs/groovydoc /tmp/docs/docs/_vaadin_flow_gradle_plugin_api"
          dir('/tmp/docs') {
            sh "git config user.email 'jenkins@devsoap.com'"
            sh "git config user.name 'Jenkins'"
            sh "git checkout -b vaadin-flow-gradle-plugin/${params.buildVersion}"
            sh "git add docs/_vaadin_flow_gradle_plugin_api"
            sh "git commit -m 'Update API documentation for Vaadin FLow Gradle Plugin ${params.buildVersion}'"
            sh "git remote add docs https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/devsoap/docs.git"
            sh "git push docs vaadin-flow-gradle-plugin/${params.buildVersion}"
          }
        }
      }

    stage('Cleanup') {
      steps {
        sh "./gradlew clean"
        sh "rm -rf /tmp/docs"
      }
    }
  }
}
