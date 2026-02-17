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
                   // Copy inventory to workspace first with ALL groups
                   sh '''
                       cp /home/ubuntu/ansible/inventory/hosts ./hosts 2>/dev/null || echo "[backend]\\n54.87.38.119 ansible_user=ubuntu ansible_ssh_private_key_file=/var/lib/jenkins/userkey.pem\\n\\n[database]\\n34.229.93.195 ansible_user=ubuntu ansible_ssh_private_key_file=/var/lib/jenkins/userkey.pem\\n\\n[frontend]\\n3.82.48.70 ansible_user=ubuntu ansible_ssh_private_key_file=/var/lib/jenkins/userkey.pem\\n\\n[all:vars]\\nansible_python_interpreter=/usr/bin/python3" > ./hosts
                   '''

                   // Read backend IP from workspace copy
                   env.BACKEND_IP = sh(
                       script: 'grep -A 1 "\\[backend\\]" ./hosts | grep -o "[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+"',
                       returnStdout: true
                   ).trim()
                   echo "Backend IP: ${env.BACKEND_IP}"

                   // Also get database IP for verification
                   env.DATABASE_IP = sh(
                       script: 'grep -A 1 "\\[database\\]" ./hosts | grep -o "[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+"',
                       returnStdout: true
                   ).trim()
                   echo "Database IP: ${env.DATABASE_IP}"
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
                    scp -i /var/lib/jenkins/userkey.pem -o StrictHostKeyChecking=no \
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
                   sh '''
                       # Copy playbook and fix issues on the fly
                       cp /home/ubuntu/ansible/playbook/deploy_backend.yml ./deploy_backend.yml 2>/dev/null || \
                       scp -i /var/lib/jenkins/userkey.pem -o StrictHostKeyChecking=no ubuntu@ip-10-0-1-59:/home/ubuntu/ansible/playbook/deploy_backend.yml ./deploy_backend.yml



                       # Run Ansible
                       ansible-playbook -i ./hosts ./deploy_backend.yml \
                           --private-key=/var/lib/jenkins/userkey.pem \
                           -e "aws_access_key=${AWS_ACCESS_KEY_ID}" \
                           -e "aws_secret_key=${AWS_SECRET_ACCESS_KEY}" \
                           -e "aws_s3_bucket=${env.S3_BUCKET}" \
                           -e "aws_s3_region=${env.AWS_REGION}"
                   '''
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