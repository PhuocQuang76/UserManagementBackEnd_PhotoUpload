pipeline {
  agent any

  tools {
    maven 'Maven3'
    jdk 'JDK17'
  }

  environment {
    IMAGE_TAG         = "${BUILD_NUMBER}"
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: 'main',
          url: 'https://github.com/neerajbalodi/user-management-backend.git'
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
        sh """
          ansible-playbook -i /home/ubuntu/ansible/inventory/hosts \
            /home/ubuntu/ansible/playbooks/deploy_backend_docker.yml \
            --private-key=/var/lib/jenkins/.ssh/userkey.pem \
            -e "aws_access_key=${AWS_ACCESS_KEY}" \
            -e "aws_secret_key=${AWS_SECRET_KEY}" \
            -e "aws_s3_bucket=user-management-s3-bucket-syn" \
            -e "aws_s3_region=eu-north-1" \
            -e "image_tag=${IMAGE_TAG}" \
            -e "ecr_registry=${ECR_REGISTRY}" \
            -e "image_name=${IMAGE_NAME}"
        """
      }
    }
  }

   stage('Deploy with Ansible') {
      steps {
          sh '''
              # Copy Ansible files to workspace first
              cp /home/ubuntu/ansible/inventory/hosts ./hosts
              cp /home/ubuntu/ansible/playbooks/deploy_backend.yml ./deploy_backend.yml

              # Use workspace copies
              ansible-playbook -i ./hosts ./deploy_backend.yml \
                  --private-key=/var/lib/jenkins/userkey.pem \
                  -e "aws_access_key=${AWS_ACCESS_KEY}" \
                  -e "aws_secret_key=${AWS_SECRET_KEY}" \
                  -e "aws_s3_bucket=${AWS_S3_BUCKET}" \
                  -e "aws_s3_region=${AWS_REGION}"
          '''
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