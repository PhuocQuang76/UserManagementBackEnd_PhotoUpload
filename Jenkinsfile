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
                       cp /home/ubuntu/ansible/inventory/hosts ./hosts 2>/dev/null || cat > ./hosts << EOF
[backend]
54.87.38.119 ansible_user=ubuntu ansible_ssh_private_key_file=/var/lib/jenkins/userkey.pem

[database]
34.229.93.195 ansible_user=ubuntu ansible_ssh_private_key_file=/var/lib/jenkins/userkey.pem

[frontend]
3.82.48.70 ansible_user=ubuntu ansible_ssh_private_key_file=/var/lib/jenkins/userkey.pem

[all:vars]
ansible_python_interpreter=/usr/bin/python3
EOF
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
               sh '''
                   # Simple manual deployment - no shell variable issues
                   ssh -i /var/lib/jenkins/userkey.pem -o StrictHostKeyChecking=no ubuntu@54.87.38.119 "
                       sudo pkill -f application.jar || true
                       cd /tmp
                       nohup sudo java -jar application.jar --server.address=0.0.0.0 --server.port=8080 > app.log 2>&1 &
                       sleep 10
                       echo 'Application started on http://54.87.38.119:8080'
                   "
               '''
           }
       }
    }

    post {
        success {
            echo "✅ Deployment successful!"
            echo "Application is running on: http://54.87.38.119:8080"
        }
        failure {
            echo "❌ Deployment failed!"
        }
    }
}