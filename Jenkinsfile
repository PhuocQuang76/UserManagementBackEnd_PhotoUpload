pipeline {
    agent any

    environment {
        EC2_IP = '100.27.198.48'  // Fixed IP address
        APP_JAR = 'user-management-0.0.1-SNAPSHOT.jar'
        REPO_URL = 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
        SSH_KEY = '/var/lib/jenkins/userkey.pem'
        SSH_USER = 'ubuntu'
        SSH_OPTS = '-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'
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

        stage('Deploy to EC2') {
            steps {
                script {
                    // Stop any running instance first
                    sh """
                        if ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "pgrep -f ${APP_JAR}"; then
                            echo "Stopping existing application..."
                            ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "pkill -f ${APP_JAR} || echo 'No running process found'"
                            sleep 5
                        fi
                    """

                    // Copy the new JAR
                    sh """
                        echo "Copying ${APP_JAR} to EC2 instance..."
                        scp ${SSH_OPTS} -i ${SSH_KEY} "target/${APP_JAR}" ${SSH_USER}@${EC2_IP}:/home/ubuntu/

                        # Set proper permissions
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "chmod +x /home/ubuntu/${APP_JAR}"
                    """
                }
            }
        }

        stage('Start Application') {
            steps {
                script {
                    sh """
                       ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "nohup java -jar /home/ubuntu/${APP_JAR} > /dev/null 2>&1 &"
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment completed successfully!'
            echo "Application is running on: http://${EC2_IP}:8091"
        }
        failure {
            echo 'Deployment failed. Check the logs for details.'
            sh """
                ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${ECV2_IP} "tail -n 50 /home/ubuntu/app.log" || echo "Could not retrieve logs"
            """
        }
    }
}