pipeline {
  agent any

  environment {
    IMAGE_TAG = "${BUILD_NUMBER}"
  }

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

              // Get ECR registry from Terraform
              env.ECR_REGISTRY = sh(script: 'terraform -chdir=/home/ubuntu/terraform output -raw ecr_repository_url', returnStdout: true).trim()

              // Parse terraform.tfvars directly
              env.AWS_S3_BUCKET = sh(
                script: '''grep "^s3_bucket_name" /home/ubuntu/terraform/terraform.tfvars | cut -d= -f2 | sed "s/ //g" | sed "s/\\"//g"''',
                returnStdout: true
              ).trim()

              env.AWS_REGION = sh(
                script: '''grep "^aws_region" /home/ubuntu/terraform/terraform.tfvars | cut -d= -f2 | sed "s/ //g" | sed "s/\\"//g"''',
                returnStdout: true
              ).trim()

              env.IMAGE_NAME = sh(
                script: '''grep "^image_name" /home/ubuntu/terraform/terraform.tfvars | cut -d= -f2 | sed "s/ //g" | sed "s/\\"//g"''',
                returnStdout: true
              ).trim()

              echo "Backend: ${env.BACKEND_IP}"
              echo "Database: ${env.DATABASE_IP}"
              echo "S3 Bucket: ${env.AWS_S3_BUCKET}"
              echo "Image: ${env.IMAGE_NAME}"
              echo "Region: ${env.AWS_REGION}"
              echo "ECR Registry: ${env.ECR_REGISTRY}"
           }
        }
    }

    stage('Build Docker Image') {
      steps {
        script {
          def ecrRegistry = sh(
            script: 'grep ecr_registry /home/ubuntu/ansible/inventory/hosts | cut -d= -f2',
            returnStdout: true
          ).trim()

          sh """
            docker build -t ${ecrRegistry}/${IMAGE_NAME}:${IMAGE_TAG} .
            docker tag ${ecrRegistry}/${IMAGE_NAME}:${IMAGE_TAG} \
                      ${ecrRegistry}/${IMAGE_NAME}:latest
          """
        }
      }
    }

    stage('Push to ECR') {
      steps {
        script {
          def ecrRegistry = sh(
            script: 'grep ecr_registry /home/ubuntu/ansible/inventory/hosts | cut -d= -f2',
            returnStdout: true
          ).trim()

          sh """
            aws ecr get-login-password --region us-east-1 | \
              docker login --username AWS --password-stdin ${ecrRegistry}

            docker push ${ecrRegistry}/${IMAGE_NAME}:${IMAGE_TAG}
            docker push ${ecrRegistry}/${IMAGE_NAME}:latest
          """
        }
      }
    }

    stage('Deploy to Backend EC2') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'awsCredential',
          usernameVariable: 'AWS_ACCESS_KEY_ID',
          passwordVariable: 'AWS_SECRET_ACCESS_KEY'
        )]) {
          sh """
            ansible-playbook -i /home/ubuntu/ansible/inventory/hosts \
              /home/ubuntu/ansible/playbooks/deploy_backend_docker.yml \
              --private-key=/var/lib/jenkins/.ssh/userkey.pem \
              -e "aws_access_key=${AWS_ACCESS_KEY_ID}" \
              -e "aws_secret_key=${AWS_SECRET_ACCESS_KEY}" \
              -e "aws_s3_bucket=user-management-s3-bucket-syn" \
              -e "aws_s3_region=eu-north-1" \
              -e "image_tag=${env.IMAGE_TAG}" \
              -e "ecr_registry=${ecrRegistry}" \
              -e "image_name=${env.IMAGE_NAME}"
          """
        }
      }
    }
  }

  post {
    success {
      echo "✅ Docker container deployed!"
    }
    failure {
      echo "❌ Deployment failed!"
    }
    always {
      sh 'docker system prune -f'
    }
  }
}