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



        stage('Get All Config') {
            steps {
                script {
                    // Get dynamic IPs from Terraform outputs
                    env.BACKEND_IP = sh(script: 'terraform -chdir=/home/ubuntu/terraform output -raw backend_ip', returnStdout: true).trim()
                    env.DATABASE_IP = sh(script: 'terraform -chdir=/home/ubuntu/terraform output -raw database_ip', returnStdout: true).trim()

                    // Parse terraform.tfvars directly (no hardcode)
                    env.AWS_S3_BUCKET = sh(
                        script: '''grep "^s3_bucket_name" /home/ubuntu/terraform/terraform.tfvars | cut -d= -f2 | sed "s/ //g" | sed "s/\\"//g"''',
                        returnStdout: true
                    ).trim()

                    env.AWS_REGION = sh(
                        script: '''grep "^aws_region" /home/ubuntu/terraform/terraform.tfvars | cut -d= -f2 | sed "s/ //g" | sed "s/\\"//g"''',
                        returnStdout: true
                    ).trim()

                    echo "Backend: ${env.BACKEND_IP}"
                    echo "Database: ${env.DATABASE_IP}"
                    echo "S3 Bucket: ${env.AWS_S3_BUCKET}"
                    echo "Region: ${env.AWS_REGION}"
                }
            }
        }

        stage('Copy JAR to Server') {
            steps {
                sh """#!/bin/bash -e
                    echo "Copying JAR to ${env.BACKEND_IP}..."
                    scp -i /var/lib/jenkins/.ssh/userkey.pem -o StrictHostKeyChecking=no \
                        target/*.jar ubuntu@${env.BACKEND_IP}:/tmp/application.jar
                    echo "✅ JAR copied successfully"
                """
            }
        }


      stage('Deploy with Ansible') {
          steps {
              withCredentials([usernamePassword(
                  credentialsId: 'awsCredential',
                  usernameVariable: 'AWS_ACCESS_KEY',
                  passwordVariable: 'AWS_SECRET_KEY'
              )]) {
                  sh """
                      ansible-playbook -i /home/ubuntu/ansible/inventory/hosts \
                          /home/ubuntu/ansible/playbooks/deploy_backend.yml \
                          --private-key=/var/lib/jenkins/.ssh/userkey.pem \
                          -e "aws_access_key=${env.AWS_ACCESS_KEY}" \
                          -e "aws_secret_key=${env.AWS_SECRET_KEY}" \
                          -e "aws_s3_bucket=${env.AWS_S3_BUCKET}" \
                          -e "aws_s3_region=${env.AWS_REGION}"
                  """
              }
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