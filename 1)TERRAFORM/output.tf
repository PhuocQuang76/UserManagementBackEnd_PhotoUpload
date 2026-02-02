# Output the public IP of the backend server
output "backend_public_ip" {
  value = aws_instance.backend_server.public_ip
}

# Output the private IP of the backend server
output "backend_private_ip" {
  value = aws_instance.backend_server.private_ip
}

# Output the public IP of the MySQL server
output "mysql_public_ip" {
  value = aws_instance.mysql_server.public_ip
}

# Output the private IP of the MySQL server
output "mysql_private_ip" {
  value = aws_instance.mysql_server.private_ip
}

# Output the public IP of the Jenkins server
output "jenkins_public_ip" {
  value = aws_instance.jenkins_server.public_ip
}

# Output the private IP of the Jenkins server
output "jenkins_private_ip" {
  value = aws_instance.jenkins_server.private_ip
}

# Output the Jenkins URL
output "jenkins_url" {
  value = "http://${aws_instance.jenkins_server.public_ip}:8080"
}

# Output the SSH command to connect to Jenkins
output "jenkins_ssh" {
  value = "ssh -i ${var.key_name}.pem ubuntu@${aws_instance.jenkins_server.public_ip}"
}