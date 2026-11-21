# AWS deployment

This Terraform stack deploys the Spring Boot API to ECS Fargate behind an HTTPS
Application Load Balancer. The College Event Hub database is a private,
encrypted RDS PostgreSQL instance. The existing Student Directory database
remains external and is reached from the private ECS subnets through the NAT
gateway.

## Architecture and cost posture

- The ALB spans two public subnets. HTTP redirects to HTTPS; an ACM certificate
  for a domain you control is required.
- ECS tasks run without public IPs in two private subnets. Their security group
  accepts API traffic only from the ALB.
- One NAT gateway provides private-subnet egress to ECR, CloudWatch Logs,
  Secrets Manager, and the external Student Directory database.
- RDS is encrypted, private, uses RDS-managed master credentials, retains
  automated backups for seven days, and has deletion protection enabled.
- The default RDS instance is single-AZ and the stack uses one NAT gateway to
  keep a student-project deployment cost-conscious. This is not a
  high-availability production configuration. Set `db_multi_az = true` for a
  Multi-AZ database, `nat_gateway_per_az = true` for redundant egress, and
  `desired_count = 2` for two application tasks. These settings increase cost.
- The app currently connects using the RDS-managed master account so Hibernate
  can retain the existing `ddl-auto=update` behavior. Before production use,
  add schema migrations and provision a least-privilege application database
  role.
- AWS charges for the ALB, NAT gateway, RDS, Fargate, storage, and data
  transfer. Check current prices for your chosen region before applying.

The ALB health check sends `GET /api/v1/auth/login`. The API exposes login as a
POST route, so the load balancer expects `405 Method Not Allowed`; missing
routes and server errors are unhealthy.

## Prerequisites

- Terraform 1.9 or later and AWS credentials with permissions for VPC, EC2,
  ECS, ECR, RDS, IAM, ACM, and CloudWatch resources.
- A public ACM certificate in the same AWS region as the load balancer.
- An external Student Directory PostgreSQL database with the existing
  `student_directory` table and student records. Its endpoint must be reachable
  from the ECS tasks; a private database in another VPC needs explicit routing
  such as VPC peering or VPN, which this stack does not create.
- If the external database uses an IP allowlist, allow the `nat_gateway_public_ips`
  output from Terraform. With the default one-NAT setup, this is one address.
- Secrets Manager secrets containing the directory JDBC URL, username,
  password, and a JWT key with at least 32 UTF-8 bytes. Pass only their ARNs to
  Terraform; secret values are not stored in Terraform configuration or state.

Create the four application secrets in Secrets Manager in the same AWS
account and region as this stack, using the AWS-managed Secrets Manager
encryption key. Set each secret's plaintext `SecretString` to the corresponding
single value:

| Terraform variable | Application secret value |
|---|---|
| `directory_db_url_secret_arn` | JDBC URL for the existing Student Directory PostgreSQL database |
| `directory_db_username_secret_arn` | Username allowed to read `student_directory` |
| `directory_db_password_secret_arn` | Password for that database user |
| `jwt_secret_arn` | Random JWT signing key of at least 32 UTF-8 bytes |

Create `terraform.tfvars` from `terraform.tfvars.example`, then set the AWS
region, ACM certificate ARN, and secret ARNs. The actual `terraform.tfvars` and
Terraform state files are excluded from Git.

## Deploy

Run these commands from the repository root in PowerShell:

```powershell
terraform -chdir=deploy/aws/terraform init
terraform -chdir=deploy/aws/terraform validate
terraform -chdir=deploy/aws/terraform plan
terraform -chdir=deploy/aws/terraform apply
```

The first apply leaves ECS application resources undeployed while creating the
ECR repository and shared infrastructure. This avoids starting a service before
an application image exists. Build and push an image from the repository root:

```powershell
$region = "YOUR_AWS_REGION"
$repository = terraform -chdir=deploy/aws/terraform output -raw ecr_repository_url
$accountId = aws sts get-caller-identity --query Account --output text
$registry = "$accountId.dkr.ecr.$region.amazonaws.com"
aws ecr get-login-password --region $region | docker login --username AWS --password-stdin $registry
docker build --platform linux/amd64 -t college-event-hub .
docker tag college-event-hub:latest "$repository:release-1"
docker push "$repository:release-1"
```

Set `container_image` in `terraform.tfvars` to the pushed ECR image URI and set
the four Secrets Manager ARNs, then run `terraform apply` again. The stack
creates the ECS task and service. Point the domain covered by the ACM
certificate at the resulting `load_balancer_dns_name` output.

## State and cleanup

Terraform state is ignored locally but is not a shared or production-grade
backend. Before team use, configure an encrypted, access-controlled remote
state backend with locking. Do not commit state or secret values.

The RDS deletion protection default intentionally blocks accidental teardown.
Before removing the stack, back up any data, set `db_deletion_protection =
false`, and apply that change first. Removing the stack deletes the database
without a final snapshot; create and verify a separate snapshot before
destroying if the data must be retained.

This stack does not deploy to AWS by itself. Review the plan and estimated
charges before applying with an AWS account.
