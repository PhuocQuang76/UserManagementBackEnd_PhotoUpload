pipeline {
    agent any

    environment {
        EC2_IP = '44.222.137.249'
        EC2_USER = 'ubuntu'
        APP_JAR = 'user-management-0.0.1-SNAPSHOT.jar'  // Fixed JAR filename
        REPO_URL = 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
        SSH_KEY = '/var/lib/jenkins/userkey.pem'
        SSH_USER = 'ubuntu'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[
                        credentialsId: '048bdac1-1fbb-4ba9-926e-cc0ff3abee5b',
                        url: 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
                    ]]
                )
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
                // Verify the JAR file was created
                sh 'ls -la target/'
            }
        }

        stage('Copy Jar to EC2 instance') {
            steps {
                script {
                    sh """
                        # Show current directory and JAR file
                        echo "Current directory: \$(pwd)"
                        echo "Looking for JAR file: target/${APP_JAR}"

                        # List all JAR files in target directory
                        echo "Available JAR files in target/:"
                        ls -la target/*.jar || echo "No JAR files found in target/"

                        # Copy the JAR file
                        echo "Copying to EC2..."
                        scp -v -i ${SSH_KEY} "target/${APP_JAR}" ${SSH_USER}@${EC2_IP}:/home/ubuntu/

                        # Verify the file was copied
                        echo "Verifying file on remote server:"
                        ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "ls -la /home/ubuntu/${APP_JAR}"
                    """
                }
            }
        }
    }
}