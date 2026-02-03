pipeline {
    agent any

    environment {
        EC2_IP = '44.201.239.239'
        APP_JAR = 'user-management-0.0.1-SNAPSHOT.jar'
        REPO_URL = 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
        SSH_KEY = '/var/lib/jenkins/userkey.pem'
        SSH_USER = 'ubuntu'
        // Add SSH options to avoid host key verification
        SSH_OPTS = '-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[
                        credentialsId: 'gitCredential',
                        url: 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
                    ]]
                )
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
                        echo "Copying to EC2..."
                        scp ${SSH_OPTS} -i ${SSH_KEY} "target/${APP_JAR}" ${SSH_USER}@${EC2_IP}:/home/ubuntu/
                        echo "Verifying file on remote server:"
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "ls -la /home/ubuntu/${APP_JAR}"
                    """
                }
            }
        }

        stage('Start Application') {
            steps {
                script {
                    try {
                        // Kill any existing Java process
                        sh "ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} 'pkill -f \"java -jar /home/ubuntu/${APP_JAR}\" || true'"

                        // Start the application in the background and get the PID
                        sh """
                            ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "\
                                cd /home/ubuntu && \
                                nohup java -jar ${APP_JAR} > /dev/null 2>&1 & \
                                echo \\\$! > app.pid"
                        """

                        // Give it time to start
                        sleep 10

                        // Verify if the application is running
                        def status = sh(
                            script: "ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} 'if [ -f /home/ubuntu/app.pid ] && ps -p \\$(cat /home/ubuntu/app.pid) > /dev/null 2>&1; then echo RUNNING; else echo NOT_RUNNING; fi'",
                            returnStdout: true
                        ).trim()

                        if (status != "RUNNING") {
                            error "Application failed to start. Check the EC2 instance for details."
                        }

                        echo "Application started successfully"

                    } catch (Exception e) {
                        error "Failed to start application: ${e.message}"
                    }
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
                echo "=== Application Logs ==="
                ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "tail -n 50 /home/ubuntu/app.log" || echo "Could not retrieve logs"
                echo "=== Java Process Status ==="
                ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "ps aux | grep java" || true
            """
        }
    }
}