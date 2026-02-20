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
          env.BACKEND_IPS = sh(script: 'terraform -chdir=/home/ubuntu/terraform output backend_ips | tr -d "[]", | tr " " ","', returnStdout: true).trim()
          env.DATABASE_IP = sh(script: 'terraform -chdir=/home/ubuntu/terraform output -raw database_ip', returnStdout: true).trim()
          env.ECR_REGISTRY = sh(script: 'terraform -chdir=/home/ubuntu/terraform output -raw ecr_registry', returnStdout: true).trim()

          // Hardcode image name to avoid parsing issues
          env.IMAGE_NAME = "user_management"

          // Parse other variables
          env.AWS_S3_BUCKET = sh(
            script: 'grep "^s3_bucket_name" /home/ubuntu/terraform/terraform.tfvars | cut -d= -f2 | sed "s/ //g" | sed "s/\\"//g"',
            returnStdout: true
          ).trim()

          env.AWS_REGION = sh(
            script: 'grep "^aws_region" /home/ubuntu/terraform/terraform.tfvars | cut -d= -f2 | sed "s/ //g" | sed "s/\\"//g"',
            returnStdout: true
          ).trim()

          echo "Backend IPs: ${env.BACKEND_IPS}"
          echo "Database IP: ${env.DATABASE_IP}"
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
          echo "Building image: ${env.ECR_REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"

          sh """
            docker build -t ${env.ECR_REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG} .
            docker tag ${env.ECR_REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG} \
                      ${env.ECR_REGISTRY}/${env.IMAGE_NAME}:latest
          """
        }
      }
    }

    stage('Push to ECR') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'awsCredential',
          usernameVariable: 'AWS_ACCESS_KEY_ID',
          passwordVariable: 'AWS_SECRET_ACCESS_KEY'
        )]) {
          script {
            sh """
              export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
              export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
              export AWS_DEFAULT_REGION=us-east-1
              aws ecr get-login-password --region us-east-1 | \
                docker login --username AWS --password-stdin ${env.ECR_REGISTRY}

              docker push ${env.ECR_REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}
              docker push ${env.ECR_REGISTRY}/${env.IMAGE_NAME}:latest
            """
          }
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
            export ANSIBLE_HOST_KEY_CHECKING=False
            ansible-playbook -i /home/ubuntu/ansible/inventory/hosts \
              /home/ubuntu/ansible/playbooks/deploy_backend_docker.yml \
              --private-key=/home/ubuntu/.ssh/userkey.pem \
              -e "aws_access_key=${AWS_ACCESS_KEY_ID}" \
              -e "aws_secret_key=${AWS_SECRET_ACCESS_KEY}" \
              -e "aws_s3_bucket=${env.AWS_S3_BUCKET}" \
              -e "aws_s3_region=${env.AWS_REGION}" \
              -e "image_tag=${env.IMAGE_TAG}" \
              -e "ecr_registry=${env.ECR_REGISTRY}" \
              -e "image_name=${env.IMAGE_NAME}" \
              -e "database_ip=${env.DATABASE_IP}"
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