pipeline {
    agent any

    options {
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B -Dmaven.test.failure.ignore=true test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B -DskipTests package'
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'docs-web/target/*.war', fingerprint: true, allowEmptyArchive: false
            }
        }

        stage('Site') {
            steps {
                sh 'mvn -B -DskipTests site site:stage'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/staging/**', allowEmptyArchive: true
                    publishHTML(target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/staging',
                        reportFiles: 'index.html',
                        reportName: 'Site Documentation'
                    ])
                }
            }
        }
    }
}
