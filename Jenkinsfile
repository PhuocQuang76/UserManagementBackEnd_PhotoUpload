pipeline {
    agent any

    environment {
        // These will be loaded from the JSON file
        EC2_IP = ''
        EC2_USER = 'ec2-user'  // or 'ubuntu' for Ubuntu
        APP_JAR = 'user-management-backend-0.0.1-SNAPSHOT.jar'
        REPO_URL = 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
         BRANCH = 'main'
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                script {
                   checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[credentialsId: '14ebfc46-00ee-4ac8-b8a4-841a3c5b0d50', url: 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git']])
                }
            }
        }

        stage('SCM') {
            steps {
                git branch: 'main', url: 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
            }
        }
    }
}