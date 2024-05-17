pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'export MAVEN_HOME=/opt/maven'
                sh 'export PATH=$PATH:$MAVEN_HOME/bin'
                sh 'mvn --version'
                sh 'mvn -B clean install'
            }
        }
    }
}