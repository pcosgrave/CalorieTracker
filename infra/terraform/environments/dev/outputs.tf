output "cognito_user_pool_id" {
  value = module.app.cognito_user_pool_id
}

output "cognito_user_pool_client_id" {
  value = module.app.cognito_user_pool_client_id
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
