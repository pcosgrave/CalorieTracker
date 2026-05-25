variable "aws_region" {
  description = "AWS region for the prod environment."
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Application name used for resource naming."
  type        = string
  default     = "bitewise"
}

variable "environment" {
  description = "Deployment environment."
  type        = string
  default     = "prod"
}

variable "web_callback_urls" {
  description = "OAuth callback URLs for the web client."
  type        = list(string)
  default     = ["https://app.bitewise.example/auth/callback"]
}

variable "web_logout_urls" {
  description = "OAuth logout URLs for the web client."
  type        = list(string)
  default     = ["https://app.bitewise.example/"]
}

variable "android_callback_urls" {
  description = "OAuth callback URLs for the Android client."
  type        = list(string)
  default     = ["bitewise://auth/callback"]
}

variable "android_logout_urls" {
  description = "OAuth logout URLs for the Android client."
  type        = list(string)
  default     = ["bitewise://signout"]
}

variable "cognito_domain_prefix" {
  description = "Optional Cognito hosted UI domain prefix."
  type        = string
  default     = null
}

variable "lambda_runtime" {
  description = "Lambda runtime for the API handlers."
  type        = string
  default     = "nodejs22.x"
}

variable "lambda_memory_mb" {
  description = "Memory size for API Lambda functions."
  type        = number
  default     = 512
}

variable "lambda_timeout_seconds" {
  description = "Execution timeout for API Lambda functions."
  type        = number
  default     = 15
}

variable "api_stage_name" {
  description = "API Gateway stage name."
  type        = string
  default     = "prod"
}

variable "google_client_id" {
  description = "Optional Google OAuth client ID for Cognito federation."
  type        = string
  default     = null
}

variable "google_client_secret" {
  description = "Optional Google OAuth client secret for Cognito federation."
  type        = string
  default     = null
  sensitive   = true
}

variable "google_authorize_scopes" {
  description = "OAuth scopes requested from Google through Cognito."
  type        = string
  default     = "openid email profile"
}
