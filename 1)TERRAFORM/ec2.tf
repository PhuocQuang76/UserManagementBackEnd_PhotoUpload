# EC2 Instance for Jenkins Server
resource "aws_instance" "jenkins_server" {
  ami                    = var.ami_value
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.jenkins_sg.id]
  subnet_id              = aws_subnet.public.id

  root_block_device {
    volume_size = 30  # GB
    volume_type = "gp3"
  }

  tags = {
    Name = "jenkins-server"
  }
}