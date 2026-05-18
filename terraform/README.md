# MarketPulse Terraform Deployment

Infrastructure as Code for deploying MarketPulse to AWS.

## Prerequisites

1. **Install Terraform:**
   ```bash
   brew install terraform
   ```

2. **AWS CLI configured:**
   ```bash
   brew install awscli
   aws configure
   ```
   Enter your AWS credentials when prompted.

3. **SSH Key pair:**
   ```bash
   # Generate if you don't have one
   ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa
   ```

4. **Push code to GitHub:**
   ```bash
   # From MarketPulse root directory
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/yourusername/marketpulse.git
   git push -u origin main
   ```

## Deployment Steps

### 1. Initialize Terraform

```bash
cd terraform
terraform init
```

### 2. Update Variables

Edit `variables.tf` or create `terraform.tfvars`:

```hcl
aws_region           = "us-east-1"
ssh_public_key_path  = "~/.ssh/id_rsa.pub"
github_repo_url      = "https://github.com/yourusername/marketpulse.git"
```

### 3. Preview Changes

```bash
terraform plan
```

This shows what resources will be created (VPC, EC2, Security Groups, etc.)

### 4. Deploy to AWS

```bash
terraform apply
```

Type `yes` to confirm. Deployment takes ~5 minutes.

### 5. Get Connection Info

```bash
terraform output
```

You'll see:
```
grafana_url = "http://54.123.45.67:3000"
instance_public_ip = "54.123.45.67"
ssh_command = "ssh -i ~/.ssh/id_rsa ec2-user@54.123.45.67"
```

### 6. Access Your Dashboard

1. Open the `grafana_url` in your browser
2. Login: `admin` / `admin`
3. Take screenshots for your portfolio

### 7. Monitor Deployment

SSH into the instance to check logs:

```bash
ssh -i ~/.ssh/id_rsa ec2-user@<PUBLIC_IP>

# Check bootstrap logs
sudo tail -f /var/log/user-data.log

# Check Docker containers
docker ps

# Check logs
docker-compose logs -f
```

### 8. Destroy Infrastructure (Important!)

**After taking screenshots, immediately destroy to avoid charges:**

```bash
terraform destroy
```

Type `yes` to confirm. This deletes ALL AWS resources.

## Cost Breakdown

- **t2.micro EC2 instance:** FREE (750 hours/month for 12 months)
- **20GB EBS volume:** FREE (30GB/month for 12 months)
- **Data transfer:** First 100GB/month FREE

**Total monthly cost:** $0.00 (within Free Tier limits)

## Architecture Deployed

```
┌─────────────────────────────────────────┐
│           AWS VPC (10.0.0.0/16)         │
│  ┌───────────────────────────────────┐  │
│  │  Public Subnet (10.0.1.0/24)      │  │
│  │                                   │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │   EC2 t2.micro (Free Tier)  │  │  │
│  │  │                             │  │  │
│  │  │  • Docker + Docker Compose  │  │  │
│  │  │  • Kafka + Zookeeper        │  │  │
│  │  │  • TimescaleDB              │  │  │
│  │  │  • Grafana (port 3000)      │  │  │
│  │  │  • Producer JAR             │  │  │
│  │  │  • Flink Processor JAR      │  │  │
│  │  └─────────────────────────────┘  │  │
│  └───────────────────────────────────┘  │
│                                         │
│  Internet Gateway                       │
└─────────────────────────────────────────┘
```

## Troubleshooting

**Issue:** `terraform apply` fails with "InvalidKeyPair.NotFound"
- **Fix:** Make sure your SSH key exists at `~/.ssh/id_rsa.pub`

**Issue:** Can't access Grafana at public IP
- **Fix:** Wait 2-3 minutes for user-data script to complete. Check logs via SSH.

**Issue:** Docker containers not starting
- **Fix:** SSH in and run `docker-compose up -d` manually in `/home/ec2-user/marketpulse`

## Security Notes

⚠️ **This configuration opens ports to 0.0.0.0/0 for demo purposes.**

For production:
- Restrict SSH to your IP only
- Use AWS Secrets Manager for credentials
- Enable VPC Flow Logs
- Use Application Load Balancer with SSL
- Implement proper authentication

## Next Steps

1. ✅ Deploy with Terraform
2. ✅ Access live Grafana dashboard
3. ✅ Take screenshot for portfolio
4. ✅ Run `terraform destroy`
5. ✅ Update README with architecture diagram and screenshot
