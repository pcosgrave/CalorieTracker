output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.main.id
}

output "cognito_user_pool_arn" {
  value = aws_cognito_user_pool.main.arn
}

output "cognito_user_pool_domain" {
  value = aws_cognito_user_pool_domain.main.domain
}

output "cognito_web_client_id" {
  value = aws_cognito_user_pool_client.web.id
}

output "cognito_android_client_id" {
  value = aws_cognito_user_pool_client.android.id
}

output "products_table_name" {
  value = aws_dynamodb_table.products.name
}

output "barcode_aliases_table_name" {
  value = aws_dynamodb_table.barcode_aliases.name
}

output "diary_entries_table_name" {
  value = aws_dynamodb_table.diary_entries.name
}

output "weight_entries_table_name" {
  value = aws_dynamodb_table.weight_entries.name
}

output "sync_changes_table_name" {
  value = aws_dynamodb_table.sync_changes.name
}

output "events_bucket_name" {
  value = aws_s3_bucket.events.bucket
}

output "api_gateway_rest_api_id" {
  value = var.create_api ? aws_api_gateway_rest_api.main[0].id : null
}

output "api_base_url" {
  value = var.create_api ? "https://${aws_api_gateway_rest_api.main[0].id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${aws_api_gateway_stage.main[0].stage_name}" : null
}
