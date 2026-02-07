# EC2 Instance for MySQL
resource "aws_instance" "mysql_server" {
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
    Name = "mysql-server"
  }
}

# EC2 Instance for Spring Boot
resource "aws_instance" "backend_server" {
  ami                    = var.ami_value
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.backend_sg.id]
  subnet_id              = aws_subnet.public.id

  tags = {
    Name = "backend-server"
  }
}

# EC2 Instance for Angular Application
resource "aws_instance" "frontend_server" {
  ami                    = var.ami_value
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.frontend_sg.id]
  subnet_id              = aws_subnet.public.id

  tags = {
    Name = "frontend-server"
  }
}


# EC2 Instance for Spring Boot
resource "aws_instance" "nexus_server" {
  ami                    = var.ami_value
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.nexus_sg.id]
  subnet_id              = aws_subnet.public.id

  tags = {
    Name = "nexus-server"
  }
}