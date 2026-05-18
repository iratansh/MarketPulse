output "instance_public_ip" {
  description = "Public IP address of the EC2 instance"
  value       = aws_instance.marketpulse_server.public_ip
}

output "instance_public_dns" {
  description = "Public DNS name of the EC2 instance"
  value       = aws_instance.marketpulse_server.public_dns
}

output "grafana_url" {
  description = "URL to access Grafana dashboard"
  value       = "http://${aws_instance.marketpulse_server.public_ip}:3000"
}

output "ssh_command" {
  description = "SSH command to connect to the instance"
  value       = "ssh -i ~/.ssh/id_rsa ec2-user@${aws_instance.marketpulse_server.public_ip}"
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.marketpulse_vpc.id
}

output "security_group_id" {
  description = "Security Group ID"
  value       = aws_security_group.marketpulse_sg.id
}
