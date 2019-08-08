pipeline {
   agent { label 'maven' }
   stages {
      stage('Build') {
         steps {
            sh 'mvn -B -V clean verify'
         }
      }
   }
}
