# EC2 Instance for MySQL
resource "aws_instance" "mysql" {
  ami                    = var.ami_value
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.mysql_sg.id]
  subnet_id              = aws_subnet.public.id

  user_data = <<-EOF
                #!/bin/bash
                sudo apt update
                sudo apt install mysql-server -y
                sudo systemctl start mysql
                sudo systemctl enable mysql
                EOF

  tags = {
    Name = "mysql-db"
  }
}

# EC2 Instance for Spring Boot
resource "aws_instance" "backend" {
  ami                    = var.ami_value
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.backend_sg.id]
  subnet_id              = aws_subnet.public.id

  user_data = <<-EOF
                #!/bin/bash
                set -e  # Exit on any error

                # Update package lists
                sudo apt-get update -y

                # Install Java 21 (with JRE and JDK)
                sudo apt-get install -y openjdk-21-jdk openjdk-21-jre

                # Install Maven
                sudo apt-get install -y maven

                # Set Java environment variables
                echo 'JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' | sudo tee -a /etc/environment
                echo 'PATH=$PATH:$JAVA_HOME/bin' | sudo tee -a /etc/environment
                source /etc/environment

                # Verify installations
                echo -e "\n=== Java Version ==="
                /usr/bin/java -version 2>&1

                echo -e "\n=== Maven Version ==="
                /usr/bin/mvn --version 2>&1

                # Create a test file to verify script execution
                echo "Installation completed at $(date)" | sudo tee /var/tmp/terraform_install.log
                EOF
  tags = {
    Name = "spring-backend"
  }
}


# EC2 Instance for Jenkins Server
resource "aws_instance" "jenkins" {
  ami                    = var.ami_value
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.jenkins_sg.id]  # You'll need to define this security group
  subnet_id              = aws_subnet.public.id

  # Increase root volume size (recommended for Jenkins)
  root_block_device {
    volume_size = 30  # GB
    volume_type = "gp3"
  }

  tags = {
    Name = "jenkins-server"
  }
}