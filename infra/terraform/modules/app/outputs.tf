output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.main.id
}

output "cognito_user_pool_client_id" {
  value = aws_cognito_user_pool_client.web.id
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

output "events_bucket_name" {
  value = aws_s3_bucket.events.bucket
}
