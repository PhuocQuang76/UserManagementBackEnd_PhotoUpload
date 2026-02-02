pipeline {
    agent any

    environment {
        EC2_IP = '44.222.137.249'
        EC2_USER = 'ubuntu'
        APP_JAR = 'user-management-0.0.1-SNAPSHOT.jar'
        REPO_URL = 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
        SSH_KEY = '/var/lib/jenkins/userkey.pem'
        SSH_USER = 'ubuntu'
        MYSQL_HOST = '35.169.107.151'
        MYSQL_DB = 'photoupload'
        MYSQL_USER = 'admin'
        MYSQL_PASSWORD = 'admin'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[
                        credentialsId: '048bdac1-1fbb-4ba9-926e-cc0ff3abee5b',
                        url: REPO_URL
                    ]]
                )
            }
        }

        stage('Configure Application') {
            steps {
                script {
                    // Update application.properties with database configuration
                    sh """
                        cat > src/main/resources/application.properties << EOL
                        spring.datasource.url=jdbc:mysql://${MYSQL_HOST}:3306/${MYSQL_DB}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
                        spring.datasource.username=${MYSQL_USER}
                        spring.datasource.password=${MYSQL_PASSWORD}
                        spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
                        spring.jpa.hibernate.ddl-auto=update
                        server.port=8080
                        EOL
                    """
                }
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
                    // Install Java if not present
                    sh """
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "
                            command -v java >/dev/null 2>&1 || {
                                echo 'Java not found, installing...'
                                sudo apt update && sudo apt install -y openjdk-17-jdk
                            }
                        "
                    """

                    // Copy JAR file
                    sh """
                        scp -o StrictHostKeyChecking=no -i ${SSH_KEY} "target/${APP_JAR}" ${SSH_USER}@${EC2_IP}:/home/ubuntu/
                    """

                    // Set permissions and start application
                    sh """
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "
                            # Stop existing instance
                            pkill -f ${APP_JAR} || echo 'No existing process found'

                            # Set permissions
                            chmod +x /home/ubuntu/${APP_JAR}

                            # Start new instance
                            nohup java -jar /home/ubuntu/${APP_JAR} > /home/ubuntu/app.log 2>&1 &

                            # Wait for startup
                            sleep 10

                            # Verify
                            if pgrep -f ${APP_JAR} >/dev/null; then
                                echo 'Application started successfully!'
                                echo 'Application is running on: http://${EC2_IP}:8080'
                            else
                                echo 'Failed to start application'
                                echo 'Last 50 lines of log:'
                                tail -n 50 /home/ubuntu/app.log
                                exit 1
                            fi
                        "
                    """
                }
            }
        }
    }

    post {
        always {
            // Clean up workspace
            cleanWs()
        }
        success {
            echo 'Deployment completed successfully!'
            echo "Application is running on: http://${EC2_IP}:8080"
        }
        failure {
            echo 'Deployment failed. Check the logs for details.'
            script {
                sh """
                    ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} "
                        echo '=== Application Logs ==='
                        tail -n 100 /home/ubuntu/app.log || echo 'Could not retrieve logs'
                        echo '=== Java Process Status ==='
                        pgrep -f ${APP_JAR} || echo 'No running Java process found'
                    "
                """
            }
        }
    }
}