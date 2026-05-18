#!/bin/bash
set -e

# Log all output
exec > >(tee /var/log/user-data.log)
exec 2>&1

echo "===== MarketPulse Bootstrap Script ====="
echo "Starting at: $(date)"

# Update system
echo "Updating system packages..."
yum update -y

# Install Docker
echo "Installing Docker..."
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Install Docker Compose
echo "Installing Docker Compose..."
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# Install Git
echo "Installing Git..."
yum install -y git

# Install Java 17 (for running producer and Flink)
echo "Installing Java 17..."
yum install -y java-17-amazon-corretto-headless

# Install Maven
echo "Installing Maven..."
yum install -y maven

# Clone MarketPulse repository
echo "Cloning MarketPulse repository..."
cd /home/ec2-user
git clone ${github_repo} marketpulse || echo "Using placeholder - update with your repo URL"

# If repo doesn't exist, create placeholder structure
if [ ! -d "marketpulse" ]; then
  echo "Repository not cloned - creating placeholder..."
  mkdir -p marketpulse
  cd marketpulse

  # Create a minimal docker-compose for demo
  cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  timescaledb:
    image: timescale/timescaledb:latest-pg16
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: marketpulse
    ports:
      - "5433:5432"
    volumes:
      - timescale-data:/var/lib/postgresql/data

  grafana:
    image: grafana/grafana:latest
    depends_on:
      - timescaledb
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana-data:/var/lib/grafana

volumes:
  timescale-data:
  grafana-data:
EOF

fi

cd /home/ec2-user/marketpulse

# Change ownership to ec2-user
chown -R ec2-user:ec2-user /home/ec2-user/marketpulse

# Start Docker containers
echo "Starting MarketPulse services..."
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to initialize..."
sleep 30

echo "===== Bootstrap Complete ====="
echo "Grafana: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):3000"
echo "Username: admin, Password: admin"
echo "Completed at: $(date)"
