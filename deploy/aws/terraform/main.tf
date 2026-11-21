locals {
  name = "${var.project_name}-${var.environment}"

  common_tags = {
    Application = "CollegeEventHub"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }

  deploy_application = var.container_image != null && trimspace(var.container_image) != ""
  app_port           = 8081
  nat_gateway_keys   = var.nat_gateway_per_az ? ["0", "1"] : ["0"]
}

resource "aws_vpc" "app" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${local.name}-vpc"
  }
}

resource "aws_internet_gateway" "app" {
  vpc_id = aws_vpc.app.id

  tags = {
    Name = "${local.name}-igw"
  }
}

resource "aws_subnet" "public" {
  for_each = {
    for index, cidr in var.public_subnet_cidrs : tostring(index) => cidr
  }

  vpc_id                  = aws_vpc.app.id
  cidr_block              = each.value
  availability_zone       = data.aws_availability_zones.available.names[tonumber(each.key)]
  map_public_ip_on_launch = true

  tags = {
    Name = "${local.name}-public-${each.key}"
    Tier = "public"
  }
}

resource "aws_subnet" "private" {
  for_each = {
    for index, cidr in var.private_subnet_cidrs : tostring(index) => cidr
  }

  vpc_id                  = aws_vpc.app.id
  cidr_block              = each.value
  availability_zone       = data.aws_availability_zones.available.names[tonumber(each.key)]
  map_public_ip_on_launch = false

  tags = {
    Name = "${local.name}-private-${each.key}"
    Tier = "private"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.app.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.app.id
  }

  tags = {
    Name = "${local.name}-public"
  }
}

resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

resource "aws_eip" "nat" {
  for_each = toset(local.nat_gateway_keys)

  domain = "vpc"

  tags = {
    Name = "${local.name}-nat-${each.key}"
  }
}

resource "aws_nat_gateway" "app" {
  for_each = aws_eip.nat

  allocation_id = each.value.id
  subnet_id     = aws_subnet.public[each.key].id

  depends_on = [aws_internet_gateway.app]

  tags = {
    Name = "${local.name}-nat-${each.key}"
  }
}

resource "aws_route_table" "private" {
  for_each = aws_subnet.private

  vpc_id = aws_vpc.app.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.app[var.nat_gateway_per_az ? each.key : "0"].id
  }

  tags = {
    Name = "${local.name}-private-${each.key}"
  }
}

resource "aws_route_table_association" "private" {
  for_each = aws_subnet.private

  subnet_id      = each.value.id
  route_table_id = aws_route_table.private[each.key].id
}

resource "aws_security_group" "load_balancer" {
  name        = "${local.name}-alb"
  description = "Public HTTPS ingress to the application load balancer."
  vpc_id      = aws_vpc.app.id

  ingress {
    description = "HTTP redirect to HTTPS"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS API traffic"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name}-alb"
  }
}

resource "aws_security_group" "application" {
  name        = "${local.name}-app"
  description = "Application ingress is limited to the load balancer."
  vpc_id      = aws_vpc.app.id

  ingress {
    description     = "Spring Boot API from the load balancer"
    from_port       = local.app_port
    to_port         = local.app_port
    protocol        = "tcp"
    security_groups = [aws_security_group.load_balancer.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name}-app"
  }
}

resource "aws_security_group" "database" {
  name        = "${local.name}-db"
  description = "PostgreSQL ingress is limited to ECS application tasks."
  vpc_id      = aws_vpc.app.id

  ingress {
    description     = "PostgreSQL from ECS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.application.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name}-db"
  }
}

resource "aws_db_subnet_group" "app" {
  name       = "${local.name}-db"
  subnet_ids = [for subnet in aws_subnet.private : subnet.id]

  tags = {
    Name = "${local.name}-db"
  }
}

resource "aws_db_instance" "app" {
  identifier                  = local.name
  engine                      = "postgres"
  instance_class              = var.db_instance_class
  allocated_storage           = 20
  max_allocated_storage       = 100
  storage_type                = "gp3"
  storage_encrypted           = true
  db_name                     = "college_event_hub"
  username                    = "college_event_hub_admin"
  manage_master_user_password = true
  multi_az                    = var.db_multi_az
  backup_retention_period     = var.db_backup_retention_days
  deletion_protection         = var.db_deletion_protection
  skip_final_snapshot         = true
  auto_minor_version_upgrade  = true
  publicly_accessible         = false
  db_subnet_group_name        = aws_db_subnet_group.app.name
  vpc_security_group_ids      = [aws_security_group.database.id]

  tags = {
    Name = "${local.name}-db"
  }
}

resource "aws_ecr_repository" "app" {
  name                 = local.name
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = {
    Name = local.name
  }
}

resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images after seven days."
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 7
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Keep the ten most recent release images."
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["release-"]
          countType     = "imageCountMoreThan"
          countNumber   = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

resource "aws_lb" "app" {
  name               = substr("${local.name}-alb", 0, 32)
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.load_balancer.id]
  subnets            = [for subnet in aws_subnet.public : subnet.id]

  tags = {
    Name = "${local.name}-alb"
  }
}

resource "aws_lb_target_group" "app" {
  name        = substr("${local.name}-app", 0, 32)
  port        = local.app_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.app.id

  health_check {
    enabled             = true
    path                = "/api/v1/auth/login"
    protocol            = "HTTP"
    matcher             = "405"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name = "${local.name}-app"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.app.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.app.arn
  port              = 443
  protocol          = "HTTPS"
  certificate_arn   = var.acm_certificate_arn
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

resource "aws_ecs_cluster" "app" {
  name = local.name

  tags = {
    Name = local.name
  }
}

resource "aws_cloudwatch_log_group" "app" {
  count = local.deploy_application ? 1 : 0

  name              = "/ecs/${local.name}"
  retention_in_days = 30

  tags = {
    Name = local.name
  }
}

resource "aws_iam_role" "task_execution" {
  count = local.deploy_application ? 1 : 0

  name               = "${local.name}-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json

  tags = {
    Name = "${local.name}-execution"
  }
}

resource "aws_iam_role_policy_attachment" "task_execution" {
  count = local.deploy_application ? 1 : 0

  role       = aws_iam_role.task_execution[0].name
  policy_arn = "arn:${data.aws_partition.current.partition}:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "task_secrets" {
  count = local.deploy_application ? 1 : 0

  name = "${local.name}-read-secrets"
  role = aws_iam_role.task_execution[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["secretsmanager:GetSecretValue"]
        Resource = compact([
          aws_db_instance.app.master_user_secret[0].secret_arn,
          var.directory_db_url_secret_arn,
          var.directory_db_username_secret_arn,
          var.directory_db_password_secret_arn,
          var.jwt_secret_arn
        ])
      }
    ]
  })
}

resource "aws_ecs_task_definition" "app" {
  count = local.deploy_application ? 1 : 0

  family                   = local.name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.fargate_cpu
  memory                   = var.fargate_memory
  execution_role_arn       = aws_iam_role.task_execution[0].arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = var.container_image
      essential = true

      portMappings = [
        {
          containerPort = local.app_port
          hostPort      = local.app_port
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = "prod"
        },
        {
          name  = "PORT"
          value = tostring(local.app_port)
        },
        {
          name  = "COLLEGE_EVENT_HUB_DB_URL"
          value = "jdbc:postgresql://${aws_db_instance.app.address}:${aws_db_instance.app.port}/${aws_db_instance.app.db_name}"
        },
        {
          name  = "COLLEGE_EVENT_HUB_DB_USERNAME"
          value = aws_db_instance.app.username
        },
        {
          name  = "COLLEGE_EVENT_HUB_JWT_EXPIRATION"
          value = "86400000"
        }
      ]

      secrets = [
        {
          name      = "COLLEGE_EVENT_HUB_DB_PASSWORD"
          valueFrom = "${aws_db_instance.app.master_user_secret[0].secret_arn}:password::"
        },
        {
          name      = "COLLEGE_STUDENT_DIRECTORY_DB_URL"
          valueFrom = var.directory_db_url_secret_arn
        },
        {
          name      = "COLLEGE_STUDENT_DIRECTORY_DB_USERNAME"
          valueFrom = var.directory_db_username_secret_arn
        },
        {
          name      = "COLLEGE_STUDENT_DIRECTORY_DB_PASSWORD"
          valueFrom = var.directory_db_password_secret_arn
        },
        {
          name      = "COLLEGE_EVENT_HUB_JWT_SECRET"
          valueFrom = var.jwt_secret_arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.app[0].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "backend"
        }
      }
    }
  ])

  lifecycle {
    precondition {
      condition = alltrue([
        var.directory_db_url_secret_arn != null,
        var.directory_db_username_secret_arn != null,
        var.directory_db_password_secret_arn != null,
        var.jwt_secret_arn != null
      ])
      error_message = "Set all Student Directory and JWT Secrets Manager ARNs before deploying the application."
    }
  }

  tags = {
    Name = local.name
  }
}

resource "aws_ecs_service" "app" {
  count = local.deploy_application ? 1 : 0

  name             = local.name
  cluster          = aws_ecs_cluster.app.id
  task_definition  = aws_ecs_task_definition.app[0].arn
  desired_count    = var.desired_count
  launch_type      = "FARGATE"
  platform_version = "1.4.0"

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
  health_check_grace_period_seconds  = 120

  network_configuration {
    subnets          = [for subnet in aws_subnet.private : subnet.id]
    security_groups  = [aws_security_group.application.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "backend"
    container_port   = local.app_port
  }

  depends_on = [
    aws_iam_role_policy_attachment.task_execution,
    aws_iam_role_policy.task_secrets,
    aws_lb_listener.https
  ]

  tags = {
    Name = local.name
  }
}
