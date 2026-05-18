terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC for MarketPulse
resource "aws_vpc" "marketpulse_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name    = "marketpulse-vpc"
    Project = "MarketPulse"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "marketpulse_igw" {
  vpc_id = aws_vpc.marketpulse_vpc.id

  tags = {
    Name    = "marketpulse-igw"
    Project = "MarketPulse"
  }
}

# Public Subnet
resource "aws_subnet" "marketpulse_subnet" {
  vpc_id                  = aws_vpc.marketpulse_vpc.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = {
    Name    = "marketpulse-subnet"
    Project = "MarketPulse"
  }
}

# Route Table
resource "aws_route_table" "marketpulse_rt" {
  vpc_id = aws_vpc.marketpulse_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.marketpulse_igw.id
  }

  tags = {
    Name    = "marketpulse-rt"
    Project = "MarketPulse"
  }
}

resource "aws_route_table_association" "marketpulse_rta" {
  subnet_id      = aws_subnet.marketpulse_subnet.id
  route_table_id = aws_route_table.marketpulse_rt.id
}

# Security Group
resource "aws_security_group" "marketpulse_sg" {
  name        = "marketpulse-sg"
  description = "Security group for MarketPulse streaming platform"
  vpc_id      = aws_vpc.marketpulse_vpc.id

  # SSH access
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH access"
  }

  # Grafana
  ingress {
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Grafana dashboard"
  }

  # Kafka (for external producers/consumers)
  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Kafka broker"
  }

  # TimescaleDB (optional, for external access)
  ingress {
    from_port   = 5433
    to_port     = 5433
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "TimescaleDB"
  }

  # Outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = {
    Name    = "marketpulse-sg"
    Project = "MarketPulse"
  }
}

# EC2 Key Pair (you'll need to create this manually or import existing)
resource "aws_key_pair" "marketpulse_key" {
  key_name   = "marketpulse-key"
  public_key = file(var.ssh_public_key_path)

  tags = {
    Name    = "marketpulse-key"
    Project = "MarketPulse"
  }
}

# EC2 Instance (t3.micro - Free Tier eligible)
resource "aws_instance" "marketpulse_server" {
  ami                    = data.aws_ami.amazon_linux_2023.id
  instance_type          = "t3.micro"
  key_name               = aws_key_pair.marketpulse_key.key_name
  vpc_security_group_ids = [aws_security_group.marketpulse_sg.id]
  subnet_id              = aws_subnet.marketpulse_subnet.id

  # User data script to bootstrap Docker and MarketPulse
  user_data = templatefile("${path.module}/user-data.sh", {
    github_repo = var.github_repo_url
  })

  # Root volume
  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  tags = {
    Name    = "marketpulse-server"
    Project = "MarketPulse"
  }
}

# Data sources
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}
