pipeline {
    agent any

    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
        IS_RELEASE = "${TAG_NAME != null && TAG_NAME.startsWith('v')}"
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew :plugin-jar-core:test :plugin-jar-gradle-plugin:functionalTest'
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
                    sh './gradlew publish -Pnexus.username=$NEXUS_USERNAME -Pnexus.password=$NEXUS_PASSWORD'
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
                    script {
                        def version = TAG_NAME.replaceFirst(/^v/, '')
                        sh './gradlew publish -Pversion=' + version + ' -Pnexus.username=$NEXUS_USERNAME -Pnexus.password=$NEXUS_PASSWORD'
                    }
                }
            }
        }
    }
}
