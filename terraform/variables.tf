variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-1"
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key for EC2 access"
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "github_repo_url" {
  description = "GitHub repository URL for MarketPulse project"
  type        = string
  default     = "https://github.com/yourusername/marketpulse.git"
}
