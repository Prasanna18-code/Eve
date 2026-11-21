variable "aws_region" {
  description = "AWS region for the deployment and its Secrets Manager references."
  type        = string
}

variable "project_name" {
  description = "Prefix used for AWS resource names."
  type        = string
  default     = "college-event-hub"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "production"
}

variable "vpc_cidr" {
  description = "CIDR range for the application VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "Two public subnet CIDRs for the load balancer and NAT gateway."
  type        = list(string)
  default     = ["10.0.0.0/24", "10.0.1.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) == 2
    error_message = "Exactly two public subnet CIDRs are required."
  }
}

variable "private_subnet_cidrs" {
  description = "Two private subnet CIDRs for ECS tasks and RDS."
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]

  validation {
    condition     = length(var.private_subnet_cidrs) == 2
    error_message = "Exactly two private subnet CIDRs are required."
  }
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for the HTTPS load balancer listener."
  type        = string
}

variable "container_image" {
  description = "ECR image URI. Leave null for the first apply, then push an image and set its URI."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition     = var.container_image == null || trimspace(var.container_image) != ""
    error_message = "Use null to defer application deployment, or provide a non-empty image URI."
  }
}

variable "directory_db_url_secret_arn" {
  description = "Secrets Manager ARN whose SecretString is COLLEGE_STUDENT_DIRECTORY_DB_URL."
  type        = string
  default     = null
  nullable    = true
}

variable "directory_db_username_secret_arn" {
  description = "Secrets Manager ARN whose SecretString is COLLEGE_STUDENT_DIRECTORY_DB_USERNAME."
  type        = string
  default     = null
  nullable    = true
}

variable "directory_db_password_secret_arn" {
  description = "Secrets Manager ARN whose SecretString is COLLEGE_STUDENT_DIRECTORY_DB_PASSWORD."
  type        = string
  default     = null
  nullable    = true
}

variable "jwt_secret_arn" {
  description = "Secrets Manager ARN whose SecretString is COLLEGE_EVENT_HUB_JWT_SECRET."
  type        = string
  default     = null
  nullable    = true
}

variable "db_instance_class" {
  description = "RDS PostgreSQL instance class. The default is intended for a cost-conscious starter."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_multi_az" {
  description = "Enable a standby RDS instance for higher availability; this increases cost."
  type        = bool
  default     = false
}

variable "nat_gateway_per_az" {
  description = "Create one NAT gateway per Availability Zone instead of sharing one gateway."
  type        = bool
  default     = false
}

variable "db_backup_retention_days" {
  description = "RDS automated backup retention period."
  type        = number
  default     = 7

  validation {
    condition     = var.db_backup_retention_days >= 1 && var.db_backup_retention_days <= 35
    error_message = "RDS backup retention must be between 1 and 35 days."
  }
}

variable "db_deletion_protection" {
  description = "Protect the database from accidental deletion."
  type        = bool
  default     = true
}

variable "fargate_cpu" {
  description = "ECS task CPU units."
  type        = number
  default     = 512
}

variable "fargate_memory" {
  description = "ECS task memory in MiB."
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Number of application tasks after an image is configured."
  type        = number
  default     = 1

  validation {
    condition     = var.desired_count >= 1
    error_message = "desired_count must be at least one when deploying the application."
  }
}
