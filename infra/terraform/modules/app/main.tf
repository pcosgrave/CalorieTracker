locals {
  name_prefix = "${var.app_name}-${var.environment}"

  tags = {
    App         = var.app_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }

  lambda_environment = {
    PRODUCTS_TABLE_NAME        = aws_dynamodb_table.products.name
    BARCODE_ALIASES_TABLE_NAME = aws_dynamodb_table.barcode_aliases.name
    DIARY_ENTRIES_TABLE_NAME   = aws_dynamodb_table.diary_entries.name
    WEIGHT_ENTRIES_TABLE_NAME  = aws_dynamodb_table.weight_entries.name
    SYNC_CHANGES_TABLE_NAME    = aws_dynamodb_table.sync_changes.name
  }

  cognito_domain_prefix = coalesce(var.cognito_domain_prefix, replace(local.name_prefix, "/[^a-zA-Z0-9-]/", "-"))
}

data "aws_region" "current" {}

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

resource "aws_cognito_user_pool_client" "web" {
  name         = "${local.name_prefix}-web"
  user_pool_id = aws_cognito_user_pool.main.id

  generate_secret = false

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["email", "openid", "profile"]
  callback_urls                        = var.web_callback_urls
  logout_urls                          = var.web_logout_urls
  supported_identity_providers         = ["COGNITO"]

  explicit_auth_flows = [
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_USER_SRP_AUTH",
  ]
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
  supported_identity_providers         = ["COGNITO"]

  explicit_auth_flows = [
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_USER_SRP_AUTH",
  ]
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
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:Query",
          "dynamodb:UpdateItem",
        ]
        Resource = [
          aws_dynamodb_table.products.arn,
          aws_dynamodb_table.barcode_aliases.arn,
          aws_dynamodb_table.diary_entries.arn,
          aws_dynamodb_table.weight_entries.arn,
          aws_dynamodb_table.sync_changes.arn,
        ]
      }
    ]
  })
}

resource "aws_lambda_function" "foods_create" {
  count = var.create_api ? 1 : 0

  function_name    = "${local.name_prefix}-foods-create"
  role             = aws_iam_role.api_lambda[0].arn
  runtime          = var.lambda_runtime
  handler          = "dist/handlers/foods.create"
  filename         = var.api_lambda_package_path
  source_code_hash = var.api_lambda_source_code_hash
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  environment {
    variables = local.lambda_environment
  }

  tags = local.tags
}

resource "aws_lambda_function" "foods_list" {
  count = var.create_api ? 1 : 0

  function_name    = "${local.name_prefix}-foods-list"
  role             = aws_iam_role.api_lambda[0].arn
  runtime          = var.lambda_runtime
  handler          = "dist/handlers/foods.list"
  filename         = var.api_lambda_package_path
  source_code_hash = var.api_lambda_source_code_hash
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  environment {
    variables = local.lambda_environment
  }

  tags = local.tags
}

resource "aws_lambda_function" "foods_lookup" {
  count = var.create_api ? 1 : 0

  function_name    = "${local.name_prefix}-foods-lookup"
  role             = aws_iam_role.api_lambda[0].arn
  runtime          = var.lambda_runtime
  handler          = "dist/handlers/foods.lookup"
  filename         = var.api_lambda_package_path
  source_code_hash = var.api_lambda_source_code_hash
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  environment {
    variables = local.lambda_environment
  }

  tags = local.tags
}

resource "aws_lambda_function" "diary_create" {
  count = var.create_api ? 1 : 0

  function_name    = "${local.name_prefix}-diary-create"
  role             = aws_iam_role.api_lambda[0].arn
  runtime          = var.lambda_runtime
  handler          = "dist/handlers/diary.create"
  filename         = var.api_lambda_package_path
  source_code_hash = var.api_lambda_source_code_hash
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  environment {
    variables = local.lambda_environment
  }

  tags = local.tags
}

resource "aws_lambda_function" "weights_create" {
  count = var.create_api ? 1 : 0

  function_name    = "${local.name_prefix}-weights-create"
  role             = aws_iam_role.api_lambda[0].arn
  runtime          = var.lambda_runtime
  handler          = "dist/handlers/weights.create"
  filename         = var.api_lambda_package_path
  source_code_hash = var.api_lambda_source_code_hash
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  environment {
    variables = local.lambda_environment
  }

  tags = local.tags
}

resource "aws_lambda_function" "weights_list" {
  count = var.create_api ? 1 : 0

  function_name    = "${local.name_prefix}-weights-list"
  role             = aws_iam_role.api_lambda[0].arn
  runtime          = var.lambda_runtime
  handler          = "dist/handlers/weights.list"
  filename         = var.api_lambda_package_path
  source_code_hash = var.api_lambda_source_code_hash
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  environment {
    variables = local.lambda_environment
  }

  tags = local.tags
}

resource "aws_lambda_function" "sync_push" {
  count = var.create_api ? 1 : 0

  function_name    = "${local.name_prefix}-sync-push"
  role             = aws_iam_role.api_lambda[0].arn
  runtime          = var.lambda_runtime
  handler          = "dist/handlers/sync.push"
  filename         = var.api_lambda_package_path
  source_code_hash = var.api_lambda_source_code_hash
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  environment {
    variables = local.lambda_environment
  }

  tags = local.tags
}

resource "aws_lambda_function" "sync_pull" {
  count = var.create_api ? 1 : 0

  function_name    = "${local.name_prefix}-sync-pull"
  role             = aws_iam_role.api_lambda[0].arn
  runtime          = var.lambda_runtime
  handler          = "dist/handlers/sync.pull"
  filename         = var.api_lambda_package_path
  source_code_hash = var.api_lambda_source_code_hash
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

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

  name          = "${local.name_prefix}-cognito"
  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  type          = "COGNITO_USER_POOLS"
  provider_arns = [aws_cognito_user_pool.main.arn]
}

resource "aws_api_gateway_resource" "foods" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_rest_api.main[0].root_resource_id
  path_part   = "foods"
}

resource "aws_api_gateway_resource" "foods_barcode" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_resource.foods[0].id
  path_part   = "barcode"
}

resource "aws_api_gateway_resource" "foods_barcode_value" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_resource.foods_barcode[0].id
  path_part   = "{barcode}"
}

resource "aws_api_gateway_resource" "diary" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_rest_api.main[0].root_resource_id
  path_part   = "diary"
}

resource "aws_api_gateway_resource" "sync" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_rest_api.main[0].root_resource_id
  path_part   = "sync"
}

resource "aws_api_gateway_resource" "weights" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_rest_api.main[0].root_resource_id
  path_part   = "weights"
}

resource "aws_api_gateway_resource" "sync_push" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_resource.sync[0].id
  path_part   = "push"
}

resource "aws_api_gateway_resource" "sync_pull" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id
  parent_id   = aws_api_gateway_resource.sync[0].id
  path_part   = "pull"
}

resource "aws_api_gateway_method" "foods_get" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.foods[0].id
  http_method   = "GET"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito[0].id
}

resource "aws_api_gateway_method" "foods_post" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.foods[0].id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito[0].id
}

resource "aws_api_gateway_method" "foods_barcode_get" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.foods_barcode_value[0].id
  http_method   = "GET"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito[0].id
}

resource "aws_api_gateway_method" "diary_post" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.diary[0].id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito[0].id
}

resource "aws_api_gateway_method" "sync_push_post" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.sync_push[0].id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito[0].id
}

resource "aws_api_gateway_method" "weights_get" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.weights[0].id
  http_method   = "GET"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito[0].id
}

resource "aws_api_gateway_method" "weights_post" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.weights[0].id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito[0].id
}

resource "aws_api_gateway_method" "sync_pull_post" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  resource_id   = aws_api_gateway_resource.sync_pull[0].id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito[0].id
}

resource "aws_api_gateway_integration" "foods_get" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.foods[0].id
  http_method             = aws_api_gateway_method.foods_get[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.foods_list[0].invoke_arn
}

resource "aws_api_gateway_integration" "foods_post" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.foods[0].id
  http_method             = aws_api_gateway_method.foods_post[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.foods_create[0].invoke_arn
}

resource "aws_api_gateway_integration" "foods_barcode_get" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.foods_barcode_value[0].id
  http_method             = aws_api_gateway_method.foods_barcode_get[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.foods_lookup[0].invoke_arn
}

resource "aws_api_gateway_integration" "diary_post" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.diary[0].id
  http_method             = aws_api_gateway_method.diary_post[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.diary_create[0].invoke_arn
}

resource "aws_api_gateway_integration" "sync_push_post" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.sync_push[0].id
  http_method             = aws_api_gateway_method.sync_push_post[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.sync_push[0].invoke_arn
}

resource "aws_api_gateway_integration" "weights_get" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.weights[0].id
  http_method             = aws_api_gateway_method.weights_get[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.weights_list[0].invoke_arn
}

resource "aws_api_gateway_integration" "weights_post" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.weights[0].id
  http_method             = aws_api_gateway_method.weights_post[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.weights_create[0].invoke_arn
}

resource "aws_api_gateway_integration" "sync_pull_post" {
  count = var.create_api ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.main[0].id
  resource_id             = aws_api_gateway_resource.sync_pull[0].id
  http_method             = aws_api_gateway_method.sync_pull_post[0].http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.sync_pull[0].invoke_arn
}

resource "aws_lambda_permission" "apigw_foods_create" {
  count = var.create_api ? 1 : 0

  statement_id  = "AllowApiGatewayInvokeFoodsCreate"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.foods_create[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.main[0].execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_foods_list" {
  count = var.create_api ? 1 : 0

  statement_id  = "AllowApiGatewayInvokeFoodsList"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.foods_list[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.main[0].execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_foods_lookup" {
  count = var.create_api ? 1 : 0

  statement_id  = "AllowApiGatewayInvokeFoodsLookup"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.foods_lookup[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.main[0].execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_diary_create" {
  count = var.create_api ? 1 : 0

  statement_id  = "AllowApiGatewayInvokeDiaryCreate"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.diary_create[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.main[0].execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_sync_push" {
  count = var.create_api ? 1 : 0

  statement_id  = "AllowApiGatewayInvokeSyncPush"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.sync_push[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.main[0].execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_weights_create" {
  count = var.create_api ? 1 : 0

  statement_id  = "AllowApiGatewayInvokeWeightsCreate"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.weights_create[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.main[0].execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_weights_list" {
  count = var.create_api ? 1 : 0

  statement_id  = "AllowApiGatewayInvokeWeightsList"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.weights_list[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.main[0].execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_sync_pull" {
  count = var.create_api ? 1 : 0

  statement_id  = "AllowApiGatewayInvokeSyncPull"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.sync_pull[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.main[0].execution_arn}/*/*"
}

resource "aws_api_gateway_deployment" "main" {
  count = var.create_api ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.main[0].id

  triggers = {
    redeploy = sha1(jsonencode([
      aws_api_gateway_integration.foods_get[0].id,
      aws_api_gateway_integration.foods_post[0].id,
      aws_api_gateway_integration.foods_barcode_get[0].id,
      aws_api_gateway_integration.diary_post[0].id,
      aws_api_gateway_integration.weights_get[0].id,
      aws_api_gateway_integration.weights_post[0].id,
      aws_api_gateway_integration.sync_push_post[0].id,
      aws_api_gateway_integration.sync_pull_post[0].id,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_api_gateway_stage" "main" {
  count = var.create_api ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.main[0].id
  deployment_id = aws_api_gateway_deployment.main[0].id
  stage_name    = var.api_stage_name

  tags = local.tags
}
