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
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[
                        url: 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git',
                        credentialsId: '14ebfc46-00ee-4ac8-b8a4-841a3c5b0d50'
                    ]]
                ])
            }
        }

        stage('Build') {
            steps {
                // Add your build steps here
                sh 'mvn clean package'  // For Maven
                // or sh './gradlew build'  // For Gradle
            }
        }
    }
}