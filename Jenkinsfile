pipeline {
    agent any

    environment {
        EC2_IP = '44.201.239.239'
        APP_JAR = 'user-management-0.0.1-SNAPSHOT.jar'
        REPO_URL = 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
        SSH_KEY = '/var/lib/jenkins/userkey.pem'
        SSH_USER = 'ubuntu'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: 'main']],
                    extensions: [],
                    userRemoteConfigs: [[
                        credentialsId: 'git_credetial',
                        url: 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
                    ]]
                ])
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
                sh 'ls -la target/'
            }
        }

        stage('Copy Jar to EC2 instance') {
            steps {
                script {
                    sh """
                        echo "Current directory: \$(pwd)"
                        echo "Looking for JAR file: target/${APP_JAR}"
                        echo "Available JAR files in target/:"
                        ls -la target/*.jar || echo "No JAR files found in target/"
                        echo "Copying to EC2..."
                        scp -v -i ${SSH_KEY} "target/${APP_JAR}" ${SSH_USER}@${EC2_IP}:/home/ubuntu/
                        echo "Verifying file on remote server:"
                        ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "ls -la /home/ubuntu/${APP_JAR}"
                    """
                }
            }
        }

        stage('Start Application') {
            steps {
                script {
                    sh """
                        # Kill any existing Java process running the app
                        ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "pkill -f 'java -jar /home/ubuntu/${APP_JAR}' || true"

                        # Start the application with nohup and log to a file
                        ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "nohup java -jar /home/ubuntu/${APP_JAR} > /home/ubuntu/app.log 2>&1 &"

                        # Give it a moment to start
                        sleep 5

                        # Verify the process is running
                        ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "pgrep -f 'java -jar /home/ubuntu/${APP_JAR}' || { echo 'Process failed to start'; exit 1; }"
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment completed successfully!'
            echo "Application is running on: http://${EC2_IP}:8091"
            echo "To check logs: ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} 'tail -f /home/ubuntu/app.log'"
        }
        failure {
            echo 'Deployment failed. Check the logs for details.'
            sh """
                echo "=== Application Logs ==="
                ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "tail -n 50 /home/ubuntu/app.log" || echo "Could not retrieve logs"
                echo "=== Java Process Status ==="
                ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "ps aux | grep java" || true
            """
        }
    }
}