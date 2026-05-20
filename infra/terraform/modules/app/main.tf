locals {
  name_prefix = "${var.app_name}-${var.environment}"

  tags = {
    App         = var.app_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

resource "aws_cognito_user_pool" "main" {
  name = "${local.name_prefix}-users"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  tags = local.tags
}

resource "aws_cognito_user_pool_client" "web" {
  name         = "${local.name_prefix}-web"
  user_pool_id = aws_cognito_user_pool.main.id

  generate_secret = false

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["email", "openid", "profile"]
  callback_urls                        = ["http://localhost:3000/auth/callback"]
  logout_urls                          = ["http://localhost:3000/"]
  supported_identity_providers         = ["COGNITO"]
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
