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
                sh '''
                    # Copy inventory to workspace
                    cp /home/ubuntu/ansible/inventory/hosts ./hosts 2>/dev/null || echo "Backend server: 54.87.38.119" > ./hosts

                    # Read from workspace copy
                    BACKEND_IP=$(grep -A 1 "\\[backend\\]" ./hosts | grep -o "[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+" | head -1)
                    DATABASE_IP=$(grep -A 1 "\\[database\\]" ./hosts | grep -o "[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+" | head -1)

                    echo "Backend IP: $BACKEND_IP"
                    echo "Database IP: $DATABASE_IP"
                '''
            }
        }

        stage('Copy JAR to Server') {
            steps {
                sh """
                    scp -i /var/lib/jenkins/userkey.pem -o StrictHostKeyChecking=no \
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