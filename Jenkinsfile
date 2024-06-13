pipeline {
    agent {
        docker {
            image 'svenruppert/maven-3.1.1-zulu:1.8.232'
            label 'meta-agent_docker'
            args '-v /tmp:/tmp'
        }
    }
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