variable "aws_region" {
  description = "AWS region for the dev environment."
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Temporary app name used for resource naming."
  type        = string
  default     = "calorie-tracker"
}

variable "environment" {
  description = "Deployment environment."
  type        = string
  default     = "dev"
}
