output "backend_public_ip" {
  value = aws_instance.backend.public_ip
}

output "backend_private_ip" {
  value = aws_instance.backend.private_ip
}

output "mysql_public_ip" {
  value = aws_instance.mysql.public_ip
}

output "mysql_private_ip" {
  value = aws_instance.mysql.private_ip
}

output "jenkins_public_ip" {
  value = aws_instance.jenkins.public_ip
}

output "jenkins_private_ip" {
  value = aws_instance.jenkins.private_ip
}

# Output the Jenkins URL and initial admin password
output "jenkins_url" {
  value = "http://${aws_instance.jenkins.public_ip}:8080"
}

output "jenkins_ssh" {
  value = "ssh -i ${var.key_name}.pem ubuntu@${aws_instance.jenkins.public_ip}"
}