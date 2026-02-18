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
                   # Extract IPs from terraform.tfstate
                   eval $(cat /home/ubuntu/terraform/terraform.tfstate | grep -E '"(backend|database|frontend)_ip": {[^}]*"value": "[^"]*"' | grep -o '"\1": "[^"]*"' | sed 's/": "/=/g' | sed 's/"//g')

                   echo "Backend IP: $backend_ip"
                   echo "Database IP: $database_ip"
                   echo "Frontend IP: $frontend_ip"
               '''
               script {
                   env.BACKEND_IP = sh(returnStdout: true, script: 'echo $backend_ip').trim()
                   env.DATABASE_IP = sh(returnStdout: true, script: 'echo $database_ip').trim()
                   env.FRONTEND_IP = sh(returnStdout: true, script: 'echo $frontend_ip').trim()

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