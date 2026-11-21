output "ecr_repository_url" {
  description = "Push the application image to this ECR repository."
  value       = aws_ecr_repository.app.repository_url
}

output "load_balancer_dns_name" {
  description = "Configure a DNS alias for the domain covered by the ACM certificate to point here."
  value       = aws_lb.app.dns_name
}

output "database_endpoint" {
  description = "Private RDS PostgreSQL endpoint for diagnostics."
  value       = aws_db_instance.app.address
}

output "database_master_secret_arn" {
  description = "RDS-managed master credential secret ARN; the application task reads its password."
  value       = aws_db_instance.app.master_user_secret[0].secret_arn
}

output "nat_gateway_public_ips" {
  description = "Allowlist these egress IPs on the external Student Directory database firewall."
  value       = [for gateway in aws_eip.nat : gateway.public_ip]
}

output "ecs_service_name" {
  description = "ECS service name, or null until a container_image is configured."
  value       = try(aws_ecs_service.app[0].name, null)
}
