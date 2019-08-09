pipeline {
    agent { label 'maven' }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -V clean verify -Prun-its -Pci'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'deployments/launcher/target/*-complete.tar.gz', fingerprint: true
        }
    }
}
