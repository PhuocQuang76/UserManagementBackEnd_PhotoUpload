#!/bin/bash
# generate_inventory.sh

# Use sed to extract IPs (more reliable)
FRONTEND_IP=$(sed -n '/frontend_public_ip/,/}/p' terraform_output.json | grep '"value"' | cut -d'"' -f4)
BACKEND_IP=$(sed -n '/backend_public_ip/,/}/p' terraform_output.json | grep '"value"' | cut -d'"' -f4)
JENKINS_IP=$(sed -n '/jenkins_public_ip/,/}/p' terraform_output.json | grep '"value"' | cut -d'"' -f4)
MYSQL_IP=$(sed -n '/mysql_public_ip/,/}/p' terraform_output.json | grep '"value"' | cut -d'"' -f4)

# Debug output
echo "Extracted IPs:"
echo "Frontend: '$FRONTEND_IP'"
echo "Backend: '$BACKEND_IP'"
echo "Jenkins: '$JENKINS_IP'"
echo "MySQL: '$MYSQL_IP'"

# Generate inventory.ini
cat > inventory.ini << EOF
[jenkins_server]
$JENKINS_IP

[mysql_server]
$MYSQL_IP

[backend_server]
$BACKEND_IP

[frontend_server]
$FRONTEND_IP

[all:vars]
ansible_user=ubuntu
ansible_ssh_private_key_file=/Users/aileen/Downloads/userkey.pem
ansible_ssh_common_args='-o StrictHostKeyChecking=no'
ansible_python_interpreter=/usr/bin/python3
EOF

echo "Inventory generated: inventory.ini"
cat inventory.ini