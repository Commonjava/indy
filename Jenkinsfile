def artifact_glob="deployments/launcher/target/*.tar.gz"
def artifact="deployments/launcher/target/*-skinny.tar.gz"
def data_artifact="deployments/launcher/target/*-data.tar.gz"

def ocp_map = '/mnt/ocp/jenkins-openshift-mappings.json'
def bc_section = 'build-configs'

def my_bc = null

pipeline {
    agent {
        kubernetes {
            cloud params.JENKINS_AGENT_CLOUD_NAME
            label "maven-36-jdk11-${UUID.randomUUID().toString()}"
            serviceAccount "jenkins"
            defaultContainer 'jnlp'
            yaml """
            apiVersion: v1
            kind: Pod
            metadata:
                labels:
                  app: "jenkins-${env.JOB_BASE_NAME}"
                  indy-pipeline-build-number: "${env.BUILD_NUMBER}"
            spec:
                containers:
                - name: jnlp
                  image: quay.io/factory2/jenkins-agent:maven-36-rhel7-latest
                  imagePullPolicy: Always
                  tty: true
                  env:
                  - name: NPMREGISTRY
                    value: 'https://repository.engineering.redhat.com/nexus/repository/registry.npmjs.org'
                  - name: JAVA_TOOL_OPTIONS
                    value: '-XX:+UnlockExperimentalVMOptions -Dsun.zip.disableMemoryMapping=true -Xms1g -Xmx4g'
                  - name: MAVEN_OPTS
                    value: '-Xmx8g -Xms1g -XX:MaxPermSize=512m -Xss8m'
                  - name: USER
                    value: 'jenkins-k8s-config'
                  - name: IMG_BUILD_HOOKS
                    valueFrom:
                      secretKeyRef:
                        key: img-build-hooks.json
                        name: img-build-hooks-secrets
                  - name: JAVA_HOME
                    value: /usr/lib/jvm/java-11-openjdk
                  - name: HOME
                    value: /home/jenkins
                  resources:
                    requests:
                      memory: 4Gi
                      cpu: 2000m
                    limits:
                      memory: 8Gi
                      cpu: 4000m
                  volumeMounts:
                  - mountPath: /home/jenkins/sonatype
                    name: volume-0
                  - mountPath: /home/jenkins/.m2
                    name: volume-1
                  - mountPath: /mnt/ocp
                    name: volume-2
                  workingDir: /home/jenkins
                volumes:
                - name: volume-0
                  secret:
                    defaultMode: 420
                    secretName: sonatype-secrets
                - name: volume-1
                  secret:
                    defaultMode: 420
                    secretName: maven-secrets
                - name: volume-2
                  configMap:
                    defaultMode: 420
                    name: jenkins-openshift-mappings
            """
        }
    }
    stages {
        stage('Prepare') {
            steps {
                sh 'printenv'
            }
        }
        stage('Build') {
            steps {
                withEnv(['JAVA_HOME=/usr/lib/jvm/java-11-openjdk']){
                    sh 'mvn -B -V clean verify -DskipNpmConfig=false --global-toolchains toolchains.xml'
                }
            }
        }
        stage('Function Test') {
            when {
                expression { env.CHANGE_ID != null } // Pull request
            }
            steps {
                withEnv(['JAVA_HOME=/usr/lib/jvm/java-11-openjdk', 'JAVA_11_HOME=/usr/lib/jvm/java-11-openjdk']){
                    sh 'mvn -B -V verify -Prun-its -Pci -DskipNpmConfig=false --global-toolchains toolchains.xml'
                }
            }
        }
        stage('Load OCP Mappings') {
            when {
                allOf {
                    expression { env.CHANGE_ID == null } // Not pull request
                }
            }
            steps {
                echo "Load OCP Mapping document"
                script {
                    def exists = fileExists ocp_map
                    if (exists){
                        def jsonObj = readJSON file: ocp_map
                        if (bc_section in jsonObj){
                            if (env.GIT_URL in jsonObj[bc_section]) {
                                echo "Found BC for Git repo: ${env.GIT_URL}"
                                if (env.BRANCH_NAME in jsonObj[bc_section][env.GIT_URL]) {
                                    my_bc = jsonObj[bc_section][env.GIT_URL][env.BRANCH_NAME]
                                } else {
                                    my_bc = jsonObj[bc_section][env.GIT_URL]['default']
                                }

                                echo "Using BuildConfig: ${my_bc}"
                            }
                            else {
                                echo "Git URL: ${env.GIT_URL} not found in BC mapping."
                            }
                        }
                        else {
                            "BC mapping is invalid! No ${bc_section} sub-object found!"
                        }
                    }
                    else {
                        echo "JSON configuration file not found: ${ocp_map}"
                    }

                    // if ( my_bc == null ) {
                    //     error("No valid BuildConfig reference found for Git URL: ${env.GIT_URL} with branch: ${env.BRANCH_NAME}")
                    // }
                }
            }
        }
        stage('Deploy') {
            when {
                allOf {
                    expression { my_bc != null }
                    expression { env.CHANGE_ID == null } // Not pull request
                    branch 'master'
                }
            }
            steps {
                echo "Deploy"
                withEnv(['JAVA_HOME=/usr/lib/jvm/java-11-openjdk', 'JAVA_11_HOME=/usr/lib/jvm/java-11-openjdk']){
                    sh 'mvn help:effective-settings -B -V -DskipTests=true -DskipNpmConfig=false deploy -e --global-toolchains toolchains.xml'
                }
            }
        }
        stage('Archive') {
            steps {
                echo "Archive"
                archiveArtifacts artifacts: "$artifact_glob", fingerprint: true
            }
        }
        stage('Build & Push Image') {
            when {
                allOf {
                    expression { my_bc != null }
                    expression { env.CHANGE_ID == null } // Not pull request
                }
            }
            steps {
                script {
                    def artifact_file = sh(script: "ls $artifact", returnStdout: true)?.trim()
                    def tarball_url = "${BUILD_URL}artifact/$artifact_file"
                    openshift.withCluster() {
                        openshift.withProject() {
                            echo "Starting image build: ${openshift.project()}:${my_bc}"
                            def bc = openshift.selector("bc", my_bc)

                            def data_artifact_file = sh(script: "ls $data_artifact", returnStdout: true)?.trim()
                            def data_tarball_url = "${BUILD_URL}artifact/$data_artifact_file"
                            
                            def buildSel = bc.startBuild("-e tarball_url=${tarball_url} -e data_tarball_url=${data_tarball_url}")
                            buildSel.logs("-f")
                        }
                    }
                }
            }
        }
    }
}
