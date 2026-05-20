# Terraform

Phase 1 infrastructure starts with the pieces that define the data boundary:

- Cognito User Pool
- Cognito app client
- DynamoDB products table
- DynamoDB barcode aliases table
- DynamoDB diary entries table
- Private encrypted S3 bucket for event/data-lake ingestion

The first environment is `environments/dev` in `us-east-1`.

Google federation is intentionally not wired yet because it needs real Google OAuth client credentials. Add a Cognito identity provider once those credentials exist.
