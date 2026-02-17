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
               withCredentials([
                   usernamePassword(
                       credentialsId: 'aws-credentials',
                       usernameVariable: 'AWS_ACCESS_KEY_ID',
                       passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                   )
               ]) {
                   sh '''
                       # Manual deployment - skip Ansible for now
                       ssh -i /var/lib/jenkins/userkey.pem -o StrictHostKeyChecking=no ubuntu@${env.BACKEND_IP} '
                           # Create app directory
                           sudo mkdir -p /opt/springboot/logs
                           sudo mkdir -p /opt/springboot

                           # Copy JAR to app directory
                           sudo cp /tmp/application.jar /opt/springboot/application.jar
                           sudo chmod +x /opt/springboot/application.jar

                           # Create application.properties
                           sudo cat > /opt/springboot/application.properties << EOF
       # Database Configuration
       spring.datasource.url=jdbc:mysql://${env.DATABASE_IP}:3306/photoupload
       spring.datasource.username=admin
       spring.datasource.password=admin
       spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
       spring.jpa.hibernate.ddl-auto=update
       spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

       # Server Configuration
       server.port=8080
       server.address=0.0.0.0

       # AWS Configuration
       aws.access-key=${AWS_ACCESS_KEY_ID}
       aws.secret-key=${AWS_SECRET_ACCESS_KEY}
       aws.s3.bucket-name=${env.S3_BUCKET}
       aws.s3.region=${env.AWS_REGION}

       # JWT Configuration
       jwt.secret=dGhpc0lzQVZlcnlMb25nU2VjcmV0S2V5Rm9ySldUVG9rZW5HZW5lcmF0aW9uMTIzNDU2Nzg5MA==
       jwt.expiration=86400000

       # File upload limits
       spring.servlet.multipart.max-file-size=5MB
       spring.servlet.multipart.max-request-size=5MB
       EOF

                           # Stop existing application
                           sudo pkill -f "application.jar" || true

                           # Start application
                           cd /opt/springboot
                           nohup sudo java -jar application.jar --spring.config.location=/opt/springboot/application.properties > logs/application.log 2>&1 &

                           # Wait for application to start
                           sleep 10

                           # Check if application is running
                           if netstat -tlnp | grep :8080; then
                               echo "✅ Application started successfully on port 8080"
                           else
                               echo "❌ Application failed to start"
                               exit 1
                           fi
                       '
                   '''
               }
           }
       }
    }

    post {
        success {
            echo "✅ Deployment successful!"
            echo "Application is running on: http://${env.BACKEND_IP}:8080"
        }
        failure {
            echo "❌ Deployment failed!"
        }
    }
}