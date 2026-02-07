# Output the public IP of the backend server
output "backend_public_ip" {
  value = aws_instance.backend_server.public_ip
}

# Output the private IP of the backend server
output "backend_private_ip" {
  value = aws_instance.backend_server.private_ip
}

# Output the public IP of the frontend server
output "frontend_public_ip" {
  value = aws_instance.frontend_server.public_ip
}

# Output the private IP of the frontend server
output "frontend_private_ip" {
  value = aws_instance.frontend_server.private_ip
}

# Output the public IP of the MySQL server
output "mysql_public_ip" {
  value = aws_instance.mysql_server.public_ip
}

# Output the private IP of the MySQL server
output "mysql_private_ip" {
  value = aws_instance.mysql_server.private_ip
}

