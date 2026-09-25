locals {
  name_prefix              = "${var.app_name}-${var.environment}"
  api_lambda_function_name = "${local.name_prefix}-api"

  tags = {
    App         = var.app_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }

  lambda_environment = merge(
    {
      PRODUCTS_TABLE_NAME        = aws_dynamodb_table.products.name
      BARCODE_ALIASES_TABLE_NAME = aws_dynamodb_table.barcode_aliases.name
      DIARY_ENTRIES_TABLE_NAME   = aws_dynamodb_table.diary_entries.name
      WEIGHT_ENTRIES_TABLE_NAME  = aws_dynamodb_table.weight_entries.name
      SYNC_CHANGES_TABLE_NAME    = aws_dynamodb_table.sync_changes.name
      HOUSEHOLDS_TABLE_NAME      = aws_dynamodb_table.households.name
      USER_POOL_ID               = aws_cognito_user_pool.main.id
    }
  )

  managed_api_lambda_log_group_names = var.create_api && var.manage_lambda_log_groups ? toset([local.api_lambda_function_name]) : toset([])

  cognito_domain_prefix = coalesce(var.cognito_domain_prefix, replace(local.name_prefix, "/[^a-zA-Z0-9-]/", "-"))
  google_provider_enabled = (
    var.google_client_id != null &&
    trimspace(var.google_client_id) != "" &&
    var.google_client_secret != null &&
    trimspace(var.google_client_secret) != ""
  )
  supported_identity_providers = local.google_provider_enabled ? ["COGNITO", "Google"] : ["COGNITO"]
}

data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_vpc" "default" {
  default = true
}

resource "aws_amplify_app" "web" {
  count = var.web_hosting_repository != null && var.web_hosting_access_token != null ? 1 : 0

  name         = "${local.name_prefix}-web"
  repository   = var.web_hosting_repository
  access_token = var.web_hosting_access_token
  platform     = "WEB_COMPUTE"

  build_spec = <<-EOT
    version: 1
    applications:
      - appRoot: apps/web
        frontend:
          phases:
            preBuild:
              commands:
                - cd ../..
                - npm ci
            build:
              commands:
                - npm run build --workspace @calorie-tracker/web
          artifacts:
            baseDirectory: apps/web/.next
            files:
              - '**/*'
          cache:
            paths:
              - node_modules/**
              - apps/web/.next/cache/**
  EOT

  environment_variables = {
    NEXT_PUBLIC_AWS_REGION           = data.aws_region.current.name
    NEXT_PUBLIC_COGNITO_DOMAIN       = local.cognito_domain_prefix
    NEXT_PUBLIC_COGNITO_USER_POOL_ID = aws_cognito_user_pool.main.id
    NEXT_PUBLIC_SYNC_API_BASE_URL    = var.create_api ? "https://${aws_api_gateway_rest_api.main[0].id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${aws_api_gateway_stage.main[0].stage_name}" : ""
  }
}

resource "aws_amplify_branch" "web" {
  count = length(aws_amplify_app.web)

  app_id      = aws_amplify_app.web[0].id
  branch_name = var.web_hosting_branch
  stage       = var.environment == "prod" ? "PRODUCTION" : "DEVELOPMENT"
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_cognito_user_pool" "main" {
  name = "${local.name_prefix}-users"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  password_policy {
    minimum_length                   = 10
    require_lowercase                = true
    require_numbers                  = true
    require_symbols                  = false
    require_uppercase                = true
    temporary_password_validity_days = 7
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  admin_create_user_config {
    allow_admin_create_user_only = false
  }

  verification_message_template {
    default_email_option = "CONFIRM_WITH_CODE"
  }

  tags = local.tags
}

resource "aws_cognito_user_pool_domain" "main" {
  domain       = local.cognito_domain_prefix
  user_pool_id = aws_cognito_user_pool.main.id
}

resource "aws_cognito_identity_provider" "google" {
  count = local.google_provider_enabled ? 1 : 0

  user_pool_id  = aws_cognito_user_pool.main.id
  provider_name = "Google"
  provider_type = "Google"

  provider_details = {
    authorize_scopes = var.google_authorize_scopes
    client_id        = var.google_client_id
    client_secret    = var.google_client_secret
  }

  attribute_mapping = {
    email          = "email"
    email_verified = "email_verified"
    name           = "name"
    username       = "sub"
  }
}

resource "aws_cognito_user_pool_client" "web" {
  name         = "${local.name_prefix}-web"
  user_pool_id = aws_cognito_user_pool.main.id

  generate_secret = false

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["email", "openid", "profile"]
  callback_urls                        = var.web_callback_urls
  logout_urls                          = var.web_logout_urls
  supported_identity_providers         = local.supported_identity_providers

  explicit_auth_flows = [
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_USER_SRP_AUTH",
  ]

  depends_on = [aws_cognito_identity_provider.google]
}

resource "aws_cognito_user_pool_client" "android" {
  name         = "${local.name_prefix}-android"
  user_pool_id = aws_cognito_user_pool.main.id

  generate_secret = false

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["email", "openid", "profile"]
  callback_urls                        = var.android_callback_urls
  logout_urls                          = var.android_logout_urls
  supported_identity_providers         = local.supported_identity_providers

  explicit_auth_flows = [
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_USER_SRP_AUTH",
  ]

  depends_on = [aws_cognito_identity_provider.google]
}

resource "aws_dynamodb_table" "products" {
  name         = "${local.name_prefix}-products"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ownerUserId"
  range_key    = "productId"

  attribute {
    name = "ownerUserId"
    type = "S"
  }

  attribute {
    name = "productId"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = local.tags
}

resource "aws_dynamodb_table" "barcode_aliases" {
  name         = "${local.name_prefix}-barcode-aliases"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ownerUserId"
  range_key    = "barcode"

  attribute {
    name = "ownerUserId"
    type = "S"
  }

  attribute {
    name = "barcode"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = local.tags
}

resource "aws_dynamodb_table" "diary_entries" {
  name         = "${local.name_prefix}-diary-entries"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ownerUserId"
  range_key    = "entryId"

  attribute {
    name = "ownerUserId"
    type = "S"
  }

  attribute {
    name = "entryId"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = local.tags
}

resource "aws_dynamodb_table" "weight_entries" {
  name         = "${local.name_prefix}-weight-entries"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ownerUserId"
  range_key    = "entryId"

  attribute {
    name = "ownerUserId"
    type = "S"
  }

  attribute {
    name = "entryId"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = local.tags
}

resource "aws_dynamodb_table" "sync_changes" {
  name         = "${local.name_prefix}-sync-changes"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ownerUserId"
  range_key    = "changeKey"

  attribute {
    name = "ownerUserId"
    type = "S"
  }

  attribute {
    name = "changeKey"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = local.tags
}

resource "aws_dynamodb_table" "households" {
  name         = "${local.name_prefix}-households"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ownerUserId"
  range_key    = "householdId"

  attribute {
    name = "ownerUserId"
    type = "S"
  }

  attribute {
    name = "householdId"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = local.tags
}

resource "aws_s3_bucket" "events" {
  bucket_prefix = "${local.name_prefix}-events-"
  tags          = local.tags
}

resource "aws_s3_bucket_public_access_block" "events" {
  bucket = aws_s3_bucket.events.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "events" {
  bucket = aws_s3_bucket.events.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_cloudwatch_log_group" "api_lambdas" {
  for_each = local.managed_api_lambda_log_group_names

  name              = "/aws/lambda/${each.value}"
  retention_in_days = var.log_retention_days
  tags              = local.tags
}

resource "aws_security_group" "api_lambda" {
  name_prefix = "${local.name_prefix}-api-"
  description = "API Lambda network access"
  vpc_id      = data.aws_vpc.default.id
  egress {
    protocol    = "-1"
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = local.tags
}

resource "aws_iam_role" "api_lambda" {
  count = var.create_api ? 1 : 0

  name = "${local.name_prefix}-api-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = local.tags
}

resource "aws_iam_role_policy" "api_lambda" {
  count = var.create_api ? 1 : 0

  name = "${local.name_prefix}-api-lambda-policy"
  role = aws_iam_role.api_lambda[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [
        {
          Effect = "Allow"
          Action = [
            "logs:CreateLogGroup",
          ]
          Resource = "*"
        },
        {
          Effect = "Allow"
          Action = [
            "ec2:CreateNetworkInterface",
            "ec2:DescribeNetworkInterfaces",
            "ec2:DeleteNetworkInterface",
          ]
          Resource = "*"
        },
        {
          Effect = "Allow"
          Action = [
            "logs:CreateLogStream",
            "logs:PutLogEvents",
          ]
          Resource = var.manage_lambda_log_groups ? flatten([
            for log_group in values(aws_cloudwatch_log_group.api_lambdas) : [
              log_group.arn,
              "${log_group.arn}:*"
            ]
          ]) : ["arn:aws:logs:*:*:*"]
        },
        {
          Effect = "Allow"
          Action = ["dynamodb:GetItem", "dynamodb:DeleteItem", "dynamodb:PutItem", "dynamodb:Query", "dynamodb:UpdateItem"]
          Resource = [
            aws_dynamodb_table.products.arn,
            aws_dynamodb_table.barcode_aliases.arn,
            aws_dynamodb_table.diary_entries.arn,
            aws_dynamodb_table.weight_entries.arn,
            aws_dynamodb_table.sync_changes.arn,
            aws_dynamodb_table.households.arn,
          ]
        },
        {
          Effect = "Allow"
          Action = [
            "cognito-idp:AdminDeleteUser",
          ]
          Resource = aws_cognito_user_pool.main.arn
        }
      ],
      []
    )
  })
}

resource "aws_lambda_function" "api" {
  count = var.create_api ? 1 : 0

  function_name    = local.api_lambda_function_name
  role             = aws_iam_role.api_lambda[0].arn
  runtime          = var.lambda_runtime
  handler          = "CalorieTracker.Api"
  filename         = var.api_lambda_package_path
  source_code_hash = var.api_lambda_source_code_hash
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  vpc_config {
    subnet_ids         = data.aws_subnets.default.ids
    security_group_ids = [aws_security_group.api_lambda.id]
  }

  environment {
    variables = local.lambda_environment
  }

  tags = local.tags
}

resource "aws_api_gateway_rest_api" "main" {
  count = var.create_api ? 1 : 0

  name = "${local.name_prefix}-api"

  endpoint_configuration {
    types = ["REGIONAL"]
  }

  tags = local.tags
}

resource "aws_api_gateway_authorizer" "cognito" {
  count = var.create_api ? 1 : 0

  name            = "${local.name_prefix}-cognito-authorizer"
  rest_api_id     = aws_api_gateway_rest_api.main[0].id
  type            = "COGNITO_USER_POOLS"
  provider_arns   = [aws_cognito_user_pool.main.arn]
  identity_source = "method.request.header.Authorization"
}

resource "aws_api_gateway_resource" "health" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_rest_api.main[0].root_resource_id
  path_part   = "health"
}

resource "aws_api_gateway_resource" "proxy" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_rest_api.main[0].root_resource_id
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "root_get" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_rest_api.main[0].root_resource_id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "health_get" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.health[0].id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "proxy_any" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.proxy[0].id
  http_method   = "ANY"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito[0].id
}

resource "aws_api_gateway_integration" "root_get" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_rest_api.main[0].root_resource_id
  http_method             = aws_api_gateway_method.root_get[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.api[0].invoke_arn
}

resource "aws_api_gateway_integration" "health_get" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.health[0].id
  http_method             = aws_api_gateway_method.health_get[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.api[0].invoke_arn
}

resource "aws_api_gateway_integration" "proxy_any" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.proxy[0].id
  http_method             = aws_api_gateway_method.proxy_any[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.api[0].invoke_arn
}

resource "aws_lambda_permission" "apigw_api" {
  count = var.create_api ? 1 : 0

  statement_id  = "AllowApiGatewayInvokeApi"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.api[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.main[0].execution_arn}/*/*"
}

resource "aws_api_gateway_deployment" "main" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id

  triggers = {
    redeploy = sha1(jsonencode([
      aws_api_gateway_method.root_get[0].id,
      aws_api_gateway_method.health_get[0].id,
      aws_api_gateway_method.proxy_any[0].id,
      aws_api_gateway_integration.root_get[0].id,
      aws_api_gateway_integration.health_get[0].id,
      aws_api_gateway_integration.proxy_any[0].id,
      aws_api_gateway_authorizer.cognito[0].id,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }

  depends_on = [
    aws_api_gateway_integration.root_get,
    aws_api_gateway_integration.health_get,
    aws_api_gateway_integration.proxy_any,
  ]
}

resource "aws_api_gateway_stage" "main" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  deployment_id = aws_api_gateway_deployment.main[0].id
  stage_name    = var.api_stage_name

  tags = local.tags
}
