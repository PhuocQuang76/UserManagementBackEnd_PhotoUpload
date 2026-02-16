pipeline {
    agent any

    stages {
        stage('Get Terraform Variables') {
            steps {
                script {
                    // Read terraform.tfvars content as string
                    def tfvarsContent = sh(
                        script: 'cat /home/ubuntu/terraform/terraform.tfvars 2>/dev/null || echo "s3_bucket_name = \\"aileenpics\\"\naws_region = \\"us-east-1\\""',
                        returnStdout: true
                    )

                    // Parse string line by line
                    def lines = tfvarsContent.split('\n')
                    lines.each { line ->
                        if (line.contains('s3_bucket_name')) {
                            env.S3_BUCKET = line.split('=')[1].trim().replaceAll('"', '')
                        }
                        if (line.contains('aws_region')) {
                            env.AWS_REGION = line.split('=')[1].trim().replaceAll('"', '')
                        }
                    }

                    echo "S3 Bucket: ${env.S3_BUCKET}"
                    echo "AWS Region: ${env.AWS_REGION}"
                }
            }
        }

        stage('Get Backend IP') {
            steps {
                script {
                    try {
                        env.BACKEND_IP = sh(
                            script: 'terraform -chdir=/home/ubuntu/terraform output -raw backend_ip 2>/dev/null || echo "3.87.38.119"',
                            returnStdout: true
                        ).trim()
                        echo "Backend IP: ${env.BACKEND_IP}"
                    } catch (Exception e) {
                        env.BACKEND_IP = "3.87.38.119"
                        echo "Backend IP (fallback): ${env.BACKEND_IP}"
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