pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git fetch --tags --force'
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
            post {
                always {
                    junit '**/build/test-results/**/TEST-*.xml'
                }
            }
        }

        stage('Publish Snapshot') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    sh './gradlew publish -Pnexus.username="$NEXUS_USERNAME" -Pnexus.password="$NEXUS_PASSWORD"'
                }
            }
        }

        stage('Publish Release') {
            when {
                buildingTag()
            }
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    withEnv(["RELEASE_VERSION=${TAG_NAME}"]) {
                        sh './gradlew publish -Pversion="$RELEASE_VERSION" -Pnexus.username="$NEXUS_USERNAME" -Pnexus.password="$NEXUS_PASSWORD"'
                    }
                }
            }
        }
    }
}
