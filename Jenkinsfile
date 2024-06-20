pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'export MAVEN_HOME=/opt/maven'
                sh 'export PATH=$PATH:$MAVEN_HOME/bin'
                sh 'JAVA_HOME=/usr/lib/jvm/zulu-8-amd64 mvn -B clean install'
            }
        }
    }
}