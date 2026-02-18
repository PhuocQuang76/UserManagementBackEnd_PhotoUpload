pipeline {
    agent any

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

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

       stage('Get Server IPs') {
           steps {
               sh '''
                   # Copy state file with ubuntu user, then read as jenkins
                   sudo cp /home/ubuntu/terraform/terraform.tfstate ./terraform.tfstate
                   sudo chown jenkins:jenkins ./terraform.tfstate
                   sudo chmod 644 ./terraform.tfstate

                   # Read IPs from copied file
                   BACKEND_IP=$(cat ./terraform.tfstate | grep -o '"backend_ip": {[^}]*"value": "[^"]*"' | grep -o '"[^"]*"$' | head -1 | tr -d '"')
                   DATABASE_IP=$(cat ./terraform.tfstate | grep -o '"database_ip": {[^}]*"value": "[^"]*"' | grep -o '"[^"]*"$' | head -1 | tr -d '"')
                   FRONTEND_IP=$(cat ./terraform.tfstate | grep -o '"frontend_ip": {[^}]*"value": "[^"]*"' | grep -o '"[^"]*"$' | head -1 | tr -d '"')

                   echo "Backend IP: $BACKEND_IP"
                   echo "Database IP: $DATABASE_IP"
                   echo "Frontend IP: $FRONTEND_IP"
               '''
               script {
                   env.BACKEND_IP = sh(returnStdout: true, script: 'echo $BACKEND_IP').trim()
                   env.DATABASE_IP = sh(returnStdout: true, script: 'echo $DATABASE_IP').trim()
                   env.FRONTEND_IP = sh(returnStdout: true, script: 'echo $FRONTEND_IP').trim()

                   echo "Final Backend IP: ${env.BACKEND_IP}"
                   echo "Final Database IP: ${env.DATABASE_IP}"
                   echo "Final Frontend IP: ${env.FRONTEND_IP}"
               }
           }
       }

        stage('Copy JAR to Server') {
            steps {
                sh """
                    scp -i /var/lib/jenkins/.ssh/userkey.pem -o StrictHostKeyChecking=no \
                        target/*.jar ubuntu@${env.BACKEND_IP}:/tmp/application.jar
                """
            }
        }

        stage('Deploy with Ansible') {
            steps {
                sh """
                    ansible-playbook -i /home/ubuntu/ansible/inventory/hosts \
                        /home/ubuntu/ansible/playbooks/deploy_backend.yml \
                        --private-key=/var/lib/jenkins/.ssh/userkey.pem \
                        -e "aws_access_key=${AWS_ACCESS_KEY}" \
                        -e "aws_secret_key=${AWS_SECRET_KEY}" \
                        -e "aws_s3_bucket=${AWS_S3_BUCKET}" \
                        -e "aws_s3_region=${AWS_REGION}"
                """
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