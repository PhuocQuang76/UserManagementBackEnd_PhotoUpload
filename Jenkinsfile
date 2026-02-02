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

        stage('Copy to EC2') {
            steps {
                script {
                    // Store the JAR file path in a variable
                    def jarFile = findFiles(glob: 'target/*.jar')[0].path
                    echo "JAR file found at: ${jarFile}"

                    // Copy the JAR file to the EC2 instance
                    sh "scp -i /var/lib/jenkins/userkey.pem ${jarFile} ubuntu@44.222.137.249:/home/ubuntu/"

                    // Verify the file was copied
                    sh "ssh -i /var/lib/jenkins/userkey.pem ubuntu@44.222.137.249 'ls -la /home/ubuntu/'"
                }
            }
        }

    }
}