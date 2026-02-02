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
               checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[credentialsId: '048bdac1-1fbb-4ba9-926e-cc0ff3abee5b', url: 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git']])
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Copy Jar to EC2 instatnce') {
            steps {
                sh """
                    scp -i /var/lib/jenkins/userkey.pem /var/lib/jenkins/workspace/user/target/user-management-0.0.1-SNAPSHOT.jar ubuntu@44.222.137.249:/home/ubuntu //copy jar file from Jenkins 																																		       //to java server
                """
            }
        }

    }
}