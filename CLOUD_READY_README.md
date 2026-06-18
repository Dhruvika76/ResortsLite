# ResortsLite - Cloud-Ready Application

## Overview
This application has been transformed to be fully cloud-ready for AWS deployment. All cloud compatibility blockers have been resolved.

## Cloud Readiness Fixes Applied

### 1. File System & Storage (cr-java-0061, cr-java-0062, cr-java-0063)
**Problem**: Hard-coded file paths and local file system dependencies
**Solution**: 
- Replaced all local file operations with Amazon S3
- Reports and backups now stored in S3 buckets
- Configuration via environment variables: `S3_REPORTS_BUCKET`, `S3_BACKUPS_BUCKET`

### 2. Configuration Management (cr-java-0069, cr-java-0071)
**Problem**: Hard-coded database credentials and environment URLs
**Solution**:
- Database credentials retrieved from AWS Secrets Manager
- All service endpoints externalized to AWS Systems Manager Parameter Store
- Environment-specific configuration without code changes

### 3. Session Management (cr-java-0065)
**Problem**: HTTP session state storage preventing horizontal scaling
**Solution**:
- Migrated to Amazon ElastiCache for Redis using Spring Session
- Distributed session management across all instances
- Configuration via: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`

### 4. Caching (cr-java-0067)
**Problem**: In-memory cache without TTL causing memory issues
**Solution**:
- Replaced HashMap with Redis-backed distributed cache
- TTL policies configured (24 hours default)
- Consistent cache across all application instances

### 5. Networking (cr-java-0077)
**Problem**: Hard-coded ports preventing dynamic assignment
**Solution**:
- Port configuration via environment variable: `SERVER_PORT`
- Compatible with ECS/EKS dynamic port assignment

### 6. Authentication (cr-java-0090)
**Problem**: File-based authentication not scalable
**Solution**:
- Migrated to AWS Secrets Manager for credential storage
- Centralized, encrypted, auditable authentication

### 7. Time Dependencies (cr-java-0111)
**Problem**: Local timezone dependencies causing inconsistencies
**Solution**:
- Migrated from java.util.Date to java.time API
- All timestamps standardized to UTC
- Consistent time handling across distributed instances

## Environment Variables

### Required for AWS Deployment
```bash
# Server Configuration
SERVER_PORT=8080

# Database (if not using Secrets Manager)
DB_URL=jdbc:postgresql://rds-endpoint:5432/resorts
DB_USERNAME=admin
DB_PASSWORD=<from-secrets-manager>

# Redis (Amazon ElastiCache)
REDIS_HOST=elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=<from-secrets-manager>
REDIS_SSL=true

# AWS Configuration
AWS_REGION=us-east-1

# S3 Buckets
S3_REPORTS_BUCKET=resorts-reports-prod
S3_BACKUPS_BUCKET=resorts-backups-prod

# AWS Secrets Manager
DB_SECRET_NAME=resorts-db-credentials
USER_SECRET_NAME=resorts-user-credentials

# Service Endpoints
PAYMENT_ENDPOINT=https://payment-svc.internal:9090/charge
INVENTORY_ENDPOINT=https://inventory-svc.internal:8081/rooms
NOTIFICATION_ENDPOINT=https://notify.internal:7070/send
REPORTS_BASE_URL=https://reports.resorts-internal.com
```

## AWS Services Required

1. **Amazon ElastiCache for Redis** - Session and cache management
2. **AWS Secrets Manager** - Credential storage and rotation
3. **Amazon S3** - File storage (reports, backups)
4. **AWS Systems Manager Parameter Store** - Configuration management
5. **Amazon RDS** (optional) - Managed database service

## Deployment Checklist

- [ ] Create ElastiCache Redis cluster
- [ ] Create S3 buckets for reports and backups
- [ ] Store database credentials in Secrets Manager
- [ ] Store user credentials in Secrets Manager
- [ ] Configure Parameter Store with service endpoints
- [ ] Set up IAM roles with appropriate permissions
- [ ] Configure security groups for service communication
- [ ] Set environment variables in ECS/EKS/Elastic Beanstalk

## IAM Permissions Required

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::resorts-reports-*/*",
        "arn:aws:s3:::resorts-backups-*/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:*:*:secret:resorts-*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": [
        "arn:aws:ssm:*:*:parameter/resorts/*"
      ]
    }
  ]
}
```

## Testing Locally

For local development, ensure Redis is running:
```bash
docker run -d -p 6379:6379 redis:latest
```

Set minimal environment variables:
```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export AWS_REGION=us-east-1
```

## Migration Notes

- All file paths have been removed from code
- No hard-coded credentials remain in source code
- Application is now stateless and horizontally scalable
- Compatible with AWS ECS, EKS, and Elastic Beanstalk
- Follows 12-factor app principles
