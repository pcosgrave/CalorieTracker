output "cognito_user_pool_id" {
  value = module.app.cognito_user_pool_id
}

output "cognito_user_pool_arn" {
  value = module.app.cognito_user_pool_arn
}

output "cognito_user_pool_domain" {
  value = module.app.cognito_user_pool_domain
}

output "cognito_web_client_id" {
  value = module.app.cognito_web_client_id
}

output "cognito_android_client_id" {
  value = module.app.cognito_android_client_id
}

output "products_table_name" {
  value = module.app.products_table_name
}

output "barcode_aliases_table_name" {
  value = module.app.barcode_aliases_table_name
}

output "diary_entries_table_name" {
  value = module.app.diary_entries_table_name
}

output "sync_changes_table_name" {
  value = module.app.sync_changes_table_name
}

output "events_bucket_name" {
  value = module.app.events_bucket_name
}

output "api_gateway_rest_api_id" {
  value = module.app.api_gateway_rest_api_id
}

output "api_base_url" {
  value = module.app.api_base_url
}
