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
                           echo "Getting server IPs dynamically..."

                           # Copy terraform state to workspace
                           cp /home/ubuntu/terraform/terraform.tfstate ./terraform.tfstate 2>/dev/null || echo "State file not accessible"

                           # Extract IPs from state file
                           if [ -f "./terraform.tfstate" ]; then
                               BACKEND_IP=$(grep -o '"backend_ip": {[^}]*"value": "[^"]*"' ./terraform.tfstate | grep -o '"[^"]*"$' | head -1 | tr -d '"')
                               DATABASE_IP=$(grep -o '"database_ip": {[^}]*"value": "[^"]*"' ./terraform.tfstate | grep -o '"[^"]*"$' | head -1 | tr -d '"')
                               FRONTEND_IP=$(grep -o '"frontend_ip": {[^}]*"value": "[^"]*"' ./terraform.tfstate | grep -o '"[^"]*"$' | head -1 | tr -d '"')

                               echo "✅ Extracted IPs from terraform.tfstate"
                               echo "Backend IP: $BACKEND_IP"
                               echo "Database IP: $DATABASE_IP"
                               echo "Frontend IP: $FRONTEND_IP"
                           else
                               # Fallback if state file not accessible
                               echo "❌ Terraform state file not accessible"
                               BACKEND_IP="54.91.109.116"
                               DATABASE_IP="34.229.179.196"
                               FRONTEND_IP="34.228.82.246"
                               echo "Using fallback IPs"
                           fi
                       '''
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