pipeline {
    agent any

    stages {
        stage('Copy Terraform Files') {
            steps {
                sh '''
                    # Create terraform directory in workspace
                    mkdir -p ./terraform

                    # Copy terraform.tfvars to workspace
                    sudo cp /home/ubuntu/terraform/terraform.tfvars ./terraform/
                    sudo chown jenkins:jenkins ./terraform/terraform.tfvars

                    # Copy terraform state if needed
                    sudo cp /home/ubuntu/terraform/terraform.tfstate ./terraform/ 2>/dev/null || true
                    sudo chown jenkins:jenkins ./terraform/terraform.tfstate 2>/dev/null || true
                '''
            }
        }

        stage('Get Terraform Variables') {
            steps {
                script {
                    // Read from workspace copy
                    env.S3_BUCKET = sh(
                        script: 'grep s3_bucket_name ./terraform/terraform.tfvars | cut -d"=" -f2',
                        returnStdout: true
                    ).trim()

                    env.AWS_REGION = sh(
                        script: 'grep aws_region ./terraform/terraform.tfvars | cut -d"=" -f2',
                        returnStdout: true
                    ).trim()

                    echo "S3 Bucket: ${env.S3_BUCKET}"
                    echo "AWS Region: ${env.AWS_REGION}"
                }
            }
        }

        stage('Get Backend IP') {
            steps {
                script {
                    // Try to get from terraform state, fallback to static
                    try {
                        env.BACKEND_IP = sh(
                            script: 'terraform -chdir=./terraform output -raw backend_ip',
                            returnStdout: true
                        ).trim()
                        echo "Backend IP from Terraform: ${env.BACKEND_IP}"
                    } catch (Exception e) {
                        echo "Terraform state not available, using static IP"
                        env.BACKEND_IP = "3.87.38.119"
                        echo "Backend IP (static): ${env.BACKEND_IP}"
                    }
                }
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Copy JAR to Server') {
            steps {
                sh """
                    scp -i /home/ubuntu/.ssh/userkey.pem -o StrictHostKeyChecking=no \
                        target/*.jar ubuntu@${env.BACKEND_IP}:/tmp/application.jar
                """
            }
        }

        stage('Deploy with Ansible') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-credentials',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh """
                        ansible-playbook -i /home/ubuntu/ansible/inventory/hosts \
                            /home/ubuntu/ansible/playbooks/deploy_backend.yml \
                            --private-key=/home/ubuntu/.ssh/userkey.pem \
                            -e "aws_access_key=${AWS_ACCESS_KEY_ID}" \
                            -e "aws_secret_key=${AWS_SECRET_ACCESS_KEY}" \
                            -e "aws_s3_bucket=${env.S3_BUCKET}" \
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