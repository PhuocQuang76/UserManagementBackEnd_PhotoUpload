pipeline {
    agent any

    environment {
        // These will be loaded from the JSON file
        EC2_IP = ''
        EC2_USER = 'ec2-user'  // or 'ubuntu' for Ubuntu
        APP_JAR = 'user-management-backend-0.0.1-SNAPSHOT.jar'
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                script {
                    checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[credentialsId: 'webhookToken', url: 'https://github.com/PhuocQuang76/Test_SpringBootDevOps.git']])
                }
            }
        }

        stage('SCM') {
            steps {
                git branch: 'main', url: 'https://github.com/PhuocQuang76/Test_SpringBootDevOps.git'
            }
        }
    }
}