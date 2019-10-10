def artifact="deployments/launcher/target/*-skinny.tar.gz"
def img_build_hook = null

pipeline {
    agent { label 'maven' }
    stages {
        stage('Prepare') {
            steps {
                sh 'printenv'
            }
        }
        stage('Build') {
            steps {
                sh 'mvn -B -V clean verify'
            }
        }
        stage('Function Test') {
            when {
                expression { env.CHANGE_ID != null } // Pull request
            }
            steps {
                sh 'mvn -B -V verify -Prun-its -Pci'
            }
        }
        stage('Deploy') {
            when { branch 'master' }
            steps {
                echo "Deploy"
                sh 'mvn help:effective-settings -B -V deploy -e'
            }
        }
        stage('Archive') {
            steps {
                echo "Archive"
                archiveArtifacts artifacts: "$artifact", fingerprint: true
            }
        }
        stage('Check Image Build Hook') {
            when {
                expression { env.IMG_BUILD_HOOKS != null }
            }
            steps {
                echo "Check Image Build Hook"
                script {
                    def jsonObj = readJSON text: env.IMG_BUILD_HOOKS
                    if (env.GIT_URL in jsonObj) {
                        echo "Build docker image"
                        if (env.BRANCH_NAME in jsonObj[env.GIT_URL]) {
                            img_build_hook = jsonObj[env.GIT_URL][env.BRANCH_NAME]
                        } else {
                            img_build_hook = jsonObj[env.GIT_URL]['default']
                        }
                    }
                }
            }
        }
        stage('Build & Push Image') {
            when {
                allOf {
                    expression { img_build_hook != null }
                    expression { env.CHANGE_ID == null } // Not pull request
                }
            }
            steps {
                script {
                    echo "Build docker image"
                    def artifact_file = sh(script: "ls $artifact", returnStdout: true)?.trim()
                    def tarball_url = "${BUILD_URL}artifact/$artifact_file"
                    sh """cat <<EOF > payload_file.yaml
env:
   - name: "tarball_url"
     value: "${tarball_url}"
EOF"""
                    sh "curl -i -H 'Content-Type: application/yaml' --data-binary @payload_file.yaml -k -X POST ${img_build_hook}"
                }
            }
        }
    }
}
