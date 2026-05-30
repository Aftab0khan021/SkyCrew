# AWS Deployment Guide — SkyCrew

## Prerequisites

- AWS Account with billing enabled
- AWS CLI installed locally
- Docker installed locally (for building images)

---

## Step 1: Create RDS PostgreSQL Instance

1. Go to **AWS Console → RDS → Create Database**
2. Settings:
   - Engine: **PostgreSQL 16**
   - Template: **Free tier** (for testing)
   - DB instance identifier: `skycrew-db`
   - Master username: `skycrew`
   - Master password: (choose a strong password)
   - DB name: `skycrew_db`
3. Connectivity:
   - VPC: Default VPC
   - Public access: **Yes** (for initial setup; disable later)
   - Security group: Create new → allow inbound **port 5432** from your EC2 security group
4. Click **Create Database**
5. Note the **Endpoint** (e.g., `skycrew-db.abc123.us-east-1.rds.amazonaws.com`)

---

## Step 2: Launch EC2 Instance

1. Go to **AWS Console → EC2 → Launch Instance**
2. Settings:
   - AMI: **Amazon Linux 2023** or **Ubuntu 22.04**
   - Instance type: **t2.micro** (free tier) or **t3.small** (recommended)
   - Key pair: Create or select an existing one
3. Security Group:
   - Allow inbound **SSH (22)** from your IP
   - Allow inbound **HTTP (8080)** from anywhere (or use a load balancer)
4. Click **Launch Instance**

---

## Step 3: Install Docker on EC2

```bash
# SSH into your EC2 instance
ssh -i your-key.pem ec2-user@<EC2-PUBLIC-IP>

# Amazon Linux 2023
sudo yum update -y
sudo yum install docker -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user

# Log out and back in for group changes
exit
ssh -i your-key.pem ec2-user@<EC2-PUBLIC-IP>

# Verify
docker --version
```

---

## Step 4: Deploy SkyCrew

```bash
# Copy the deploy script to EC2
scp -i your-key.pem deploy/aws/deploy.sh ec2-user@<EC2-PUBLIC-IP>:~/

# SSH into EC2 and run
ssh -i your-key.pem ec2-user@<EC2-PUBLIC-IP>

# Set environment variables
export RDS_HOSTNAME="skycrew-db.abc123.us-east-1.rds.amazonaws.com"
export RDS_PASSWORD="your-rds-password"
export DOCKER_IMAGE="ghcr.io/your-username/skycrew:latest"

# Run deployment
chmod +x deploy.sh
./deploy.sh
```

---

## Step 5: Verify

1. **Swagger UI**: `http://<EC2-PUBLIC-IP>:8080/swagger-ui.html`
2. **Health Check**: `curl http://<EC2-PUBLIC-IP>:8080/actuator/health`
3. **Login**: 
   ```bash
   curl -X POST http://<EC2-PUBLIC-IP>:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   ```

---

## Production Hardening Checklist

- [ ] Disable RDS public access (use VPC-only access)
- [ ] Set up Application Load Balancer (ALB) with HTTPS
- [ ] Use AWS Secrets Manager for database credentials
- [ ] Enable CloudWatch logging
- [ ] Set up Auto Scaling Group
- [ ] Configure a domain name (Route 53)
- [ ] Enable RDS automated backups
- [ ] Restrict EC2 security group to ALB only
