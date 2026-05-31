terraform {
  required_version = ">= 1.8.0"

  backend "s3" {}

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

locals {
  api_lambda_package_path   = abspath("${path.root}/../../../../apps/api/dist/lambda/api.zip")
  api_lambda_package_exists = fileexists(local.api_lambda_package_path)
}

provider "aws" {
  region = var.aws_region
}

module "app" {
  source = "../../modules/app"

  app_name    = var.app_name
  environment = var.environment

  web_callback_urls     = var.web_callback_urls
  web_logout_urls       = var.web_logout_urls
  android_callback_urls = var.android_callback_urls
  android_logout_urls   = var.android_logout_urls
  cognito_domain_prefix = var.cognito_domain_prefix

  create_api                  = local.api_lambda_package_exists
  api_lambda_package_path     = local.api_lambda_package_path
  api_lambda_source_code_hash = local.api_lambda_package_exists ? filebase64sha256(local.api_lambda_package_path) : null
  lambda_runtime              = var.lambda_runtime
  lambda_memory_mb            = var.lambda_memory_mb
  lambda_timeout_seconds      = var.lambda_timeout_seconds
  api_stage_name              = var.api_stage_name
  log_retention_days          = var.log_retention_days
  manage_lambda_log_groups    = var.manage_lambda_log_groups
  google_client_id            = var.google_client_id
  google_client_secret        = var.google_client_secret
  google_authorize_scopes     = var.google_authorize_scopes
}
