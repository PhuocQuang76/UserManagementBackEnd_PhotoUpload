
# Output the Jenkins URL
output "jenkins_url" {
  value = "http://${aws_instance.jenkins_server.public_ip}:8080"
}

# Output the SSH command to connect to Jenkins
output "jenkins_ssh" {
  value = "ssh -i ${var.key_name}.pem ubuntu@${aws_instance.jenkins_server.public_ip}"
}

# Output the public IP of the Jenkins server
output "jenkins_public_ip" {
  value = aws_instance.jenkins_server.public_ip
}

# Output the private IP of the Jenkins server
output "jenkins_private_ip" {
  value = aws_instance.jenkins_server.private_ip
}
