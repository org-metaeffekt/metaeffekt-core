pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'export MAVEN_HOME=/opt/maven'
                sn 'export PATH=$PATH:$MAVEN_HOME/bin'
                sh 'mvn --version'
                sh 'mvn -B clean install'
            }
        }
    }
}