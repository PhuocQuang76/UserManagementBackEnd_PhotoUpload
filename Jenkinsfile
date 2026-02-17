pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[
                        credentialsId: 'gitCredentials',
                        url: 'https://github.com/PhuocQuang76/UserManagementBackEnd_PhotoUpload.git'
                    ]]
                )
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Get Server IPs') {
            steps {
                script {
                    // Ensure inventory is accessible
                    sh 'ls -la /home/ubuntu/ansible/inventory/hosts'

                    // Copy to workspace for safe processing
                    sh 'cp /home/ubuntu/ansible/inventory/hosts ./hosts'

                    // Extract IPs using grep
                    env.BACKEND_IP = sh(
                        script: 'grep -A 1 "\\[backend\\]" ./hosts | grep -o "[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+" | head -1',
                        returnStdout: true
                    ).trim()

                    env.DATABASE_IP = sh(
                        script: 'grep -A 1 "\\[database\\]" ./hosts | grep -o "[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+" | head -1',
                        returnStdout: true
                    ).trim()

                    echo "🔍 Server IPs Found:"
                    echo "Backend IP: ${env.BACKEND_IP}"
                    echo "Database IP: ${env.DATABASE_IP}"
                }
            }
        }

        stage('Copy JAR to Server') {
            steps {
                sh """
                    scp -i /var/lib/jenkins/.ssh/user.pem -o StrictHostKeyChecking=no \
                        target/*.jar ubuntu@${env.BACKEND_IP}:/tmp/application.jar
                """
            }
        }

        stage('Deploy with Ansible') {
            steps {
                sh '''
                    # Copy Ansible files to workspace first
                    cp /home/ubuntu/ansible/inventory/hosts ./hosts
                    cp /home/ubuntu/ansible/playbooks/deploy_backend.yml ./deploy_backend.yml

                    # Use workspace copies
                    ansible-playbook -i ./hosts ./deploy_backend.yml \
                        --private-key=/var/lib/jenkins/userkey.pem \
                        -e "aws_access_key=${AWS_ACCESS_KEY}" \
                        -e "aws_secret_key=${AWS_SECRET_KEY}" \
                        -e "aws_s3_bucket=${AWS_S3_BUCKET}" \
                        -e "aws_s3_region=${AWS_REGION}"
                '''
            }
        }
    }

    post {
        success {
            echo "✅ Deployment successful!"
        }
        failure {
            echo "❌ Deployment failed!"
        }
    }
}