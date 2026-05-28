variable "app_name" {
  description = "Application name prefix."
  type        = string
}

variable "environment" {
  description = "Deployment environment."
  type        = string
}

variable "web_callback_urls" {
  description = "OAuth callback URLs for the web client."
  type        = list(string)
}

variable "web_logout_urls" {
  description = "OAuth logout URLs for the web client."
  type        = list(string)
}

variable "android_callback_urls" {
  description = "OAuth callback URLs for the Android client."
  type        = list(string)
}

variable "android_logout_urls" {
  description = "OAuth logout URLs for the Android client."
  type        = list(string)
}

variable "cognito_domain_prefix" {
  description = "Optional Cognito hosted UI domain prefix."
  type        = string
  default     = null
}

variable "create_api" {
  description = "Whether to create Lambda and API Gateway resources."
  type        = bool
  default     = true
}

variable "api_lambda_package_path" {
  description = "Path to the Lambda deployment zip for the API handlers."
  type        = string
  default     = ""
}

variable "api_lambda_source_code_hash" {
  description = "Base64-encoded SHA256 hash of the Lambda deployment zip."
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
  default     = "dev"
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

variable "log_retention_days" {
  description = "Retention period for Lambda CloudWatch log groups."
  type        = number
  default     = 30
}

variable "manage_lambda_log_groups" {
  description = "Whether Terraform should create and manage encrypted Lambda CloudWatch log groups."
  type        = bool
  default     = false
}
