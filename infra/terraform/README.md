# Terraform

This directory now scaffolds a first-pass AWS `dev` environment for:

- Amazon Cognito user pool and hosted domain
- Web and Android Cognito app clients
- DynamoDB tables for products, barcode aliases, diary entries, and sync changes
- A private encrypted S3 bucket for future event/export ingestion
- Lambda definitions for the API handlers
- API Gateway with Cognito-protected routes for foods, diary, and sync

## Layout

- `modules/app`: the shared infrastructure module
- `environments/dev`: the first deployable environment

## Current Deployment Model

The Terraform `dev` environment always creates Cognito, DynamoDB, and S3.

Lambda and API Gateway are created only when the API deployment zip exists at:

`apps/api/dist/lambda/api.zip`

That zip can be built from the repo root with:

```powershell
.\scripts\build-api-lambda.ps1
```

## Dev Apply Flow

From the repo root:

```powershell
.\scripts\build-api-lambda.ps1
terraform -chdir=infra/terraform/environments/dev init
terraform -chdir=infra/terraform/environments/dev plan
terraform -chdir=infra/terraform/environments/dev apply
```

## Important Outputs

After apply, useful outputs include:

- `cognito_user_pool_id`
- `cognito_web_client_id`
- `cognito_android_client_id`
- `cognito_user_pool_domain`
- `api_base_url`
- `sync_changes_table_name`

## Notes

- Google federation is still intentionally out of scope for this first pass.
- The Android and web apps still need Cognito client integration and token handling.
- The Lambda package currently vendors `zod` and the shared package, while relying on the AWS Lambda Node.js runtime's included AWS SDK v3. See AWS Lambda Node.js runtime docs for the runtime-included SDK behavior: [Building Lambda functions with Node.js](https://docs.aws.amazon.com/lambda/latest/dg/lambda-nodejs.html).
