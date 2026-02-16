pipeline {
    agent any

    stages {
        stage('Get Terraform Variables') {
            steps {
                script {
                    // Get bucket name from terraform.tfvars
                    env.S3_BUCKET = sh(
                        script: 'grep s3_bucket_name /home/ubuntu/terraform/terraform.tfvars | cut -d"=" -f2',
                        returnStdout: true
                    ).trim()

                    // Get region from terraform.tfvars
                    env.AWS_REGION = sh(
                        script: 'grep aws_region /home/ubuntu/terraform/terraform.tfvars | cut -d"=" -f2',
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
                    env.BACKEND_IP = sh(
                        script: 'terraform -chdir=/home/ubuntu/terraform output -raw backend_ip',
                        returnStdout: true
                    ).trim()
                    echo "Backend IP: ${env.BACKEND_IP}"
                }
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Deploy with Ansible') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-credentials',  // ✅ Correct ID
                        usernameVariable: 'AWS_ACCESS_KEY_ID',  // ✅ Variable name
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'  // ✅ Variable name
                    )
                ]) {
                    sh """
                        ansible-playbook -i /home/ubuntu/ansible/inventory/hosts \
                            /home/ubuntu/ansible/playbooks/deploy_backend.yml \
                            --private-key=/var/lib/jenkins/.ssh/user.pem \
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