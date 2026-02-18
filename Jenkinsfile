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
                      script {
                          // Try terraform first
                          try {
                              env.BACKEND_IP = sh(
                                  script: 'terraform -chdir=/home/ubuntu/terraform output -raw backend_ip',
                                  returnStdout: true
                              ).trim()

                              env.DATABASE_IP = sh(
                                  script: 'terraform -chdir=/home/ubuntu/terraform output -raw database_ip',
                                  returnStdout: true
                              ).trim()

                              echo "✅ Backend IP: ${env.BACKEND_IP}"
                              echo "✅ Database IP: ${env.DATABASE_IP}"
                          } catch (Exception e) {
                              echo "❌ Terraform failed: ${e.getMessage()}"
                              // Fallback to AWS CLI
                              env.BACKEND_IP = sh(
                                  script: 'aws ec2 describe-instances --filters "Name=tag:Name,Values=backend-server" --query "Reservations[*].Instances[*].PublicIpAddress" --output text | head -1',
                                  returnStdout: true
                              ).trim()

                              env.DATABASE_IP = sh(
                                  script: 'aws ec2 describe-instances --filters "Name=tag:Name,Values=database-server" --query "Reservations[*].Instances[*].PublicIpAddress" --output text | head -1',
                                  returnStdout: true
                              ).trim()

                              echo "✅ Backend IP (from AWS): ${env.BACKEND_IP}"
                              echo "✅ Database IP (from AWS): ${env.DATABASE_IP}"
                          }
                      }
                  }
              }

               stage('Copy JAR to Server') {
                   steps {
                       sh '''
                           echo "Copying JAR to ${env.BACKEND_IP}..."
                           scp -i /var/lib/jenkins/.ssh/userkey.pem -o StrictHostKeyChecking=no \
                               target/*.jar ubuntu@${env.BACKEND_IP}:/tmp/application.jar
                           echo "✅ JAR copied successfully"
                       '''
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