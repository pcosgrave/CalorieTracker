variable "aws_region" {
  description = "AWS region for the dev environment."
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Application name used for resource naming."
  type        = string
  default     = "calorie-tracker"
}

variable "environment" {
  description = "Deployment environment."
  type        = string
  default     = "dev"
}

variable "web_callback_urls" {
  description = "OAuth callback URLs for the web client."
  type        = list(string)
  default     = ["http://localhost:3000/auth/callback"]
}

variable "web_logout_urls" {
  description = "OAuth logout URLs for the web client."
  type        = list(string)
  default     = ["http://localhost:3000/"]
}

variable "android_callback_urls" {
  description = "OAuth callback URLs for the Android client."
  type        = list(string)
  default     = ["calorietracker://auth/callback"]
}

variable "android_logout_urls" {
  description = "OAuth logout URLs for the Android client."
  type        = list(string)
  default     = ["calorietracker://signout"]
}

variable "cognito_domain_prefix" {
  description = "Optional Cognito hosted UI domain prefix."
  type        = string
  default     = null
}

variable "lambda_runtime" {
  description = "Lambda runtime for the API."
  type        = string
  default     = "dotnet8"
}

variable "lambda_memory_mb" {
  description = "Memory size for the API Lambda function."
  type        = number
  default     = 512
}

variable "lambda_timeout_seconds" {
  description = "Execution timeout for the API Lambda function."
  type        = number
  default     = 15
}

variable "api_stage_name" {
  description = "API Gateway stage name."
  type        = string
  default     = "dev"
}

variable "manage_lambda_log_groups" {
  description = "Whether Terraform should create and manage encrypted Lambda CloudWatch log groups."
  type        = bool
  default     = false
}

variable "log_retention_days" {
  description = "Retention period for API Lambda CloudWatch log groups."
  type        = number
  default     = 30
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

variable "gemini_api_secret_arn" {
  description = "Optional Secrets Manager ARN for the Gemini API secret."
  type        = string
  default     = null
  sensitive   = true
}
