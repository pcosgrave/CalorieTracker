# Terraform

This directory now scaffolds AWS environments for:

- Amazon Cognito user pool and hosted domain
- Web and Android Cognito app clients
- DynamoDB tables for products, barcode aliases, diary entries, and sync changes
- A private encrypted S3 bucket for future event/export ingestion
- A customer-managed KMS key used by DynamoDB, S3, and Lambda CloudWatch log groups
- Lambda definitions for the API handlers
- API Gateway with Cognito-protected routes for foods, diary, and sync

## Layout

- `modules/app`: the shared infrastructure module
- `environments/dev`: the local testing / development environment
- `environments/prod`: the production scaffold for future publishing

Helpful local files:

- `environments/dev/backend.hcl.example`: example remote-state backend config
- `environments/dev/terraform.tfvars.example`: example environment values
- `environments/prod/backend.hcl.example`: example prod remote-state backend config
- `environments/prod/terraform.tfvars.example`: example prod environment values
- `../../scripts/bootstrap-terraform-backend.ps1`: one-time S3/DynamoDB backend bootstrap

## Current Deployment Model

The Terraform `dev` environment always creates Cognito, DynamoDB, and S3.

Lambda and API Gateway are created only when the API deployment zip exists at:

`apps/api/dist/lambda/api.zip`

That zip can be built from the repo root with:

```powershell
.\scripts\build-api-lambda.ps1
```

## Recommended Remote State Setup

Before your first real deploy, use a remote Terraform backend instead of local state. The recommended setup is:

- S3 bucket for Terraform state
- DynamoDB table for Terraform state locking

You can bootstrap those backend resources with:

```powershell
.\scripts\bootstrap-terraform-backend.ps1 -BucketName <your-terraform-state-bucket> -LockTableName <your-terraform-locks-table> -Region <your-aws-region>
```

Then copy:

- `environments/dev/backend.hcl.example` to `environments/dev/backend.hcl`
- `environments/dev/terraform.tfvars.example` to `environments/dev/terraform.tfvars`

Fill in your real bucket, lock table, region, and Cognito domain prefix before running `terraform init`.

## Dev Apply Flow

From the repo root:

```powershell
.\scripts\build-api-lambda.ps1
terraform -chdir=infra/terraform/environments/dev init -backend-config="backend.hcl"
terraform -chdir=infra/terraform/environments/dev plan -var-file="terraform.tfvars"
terraform -chdir=infra/terraform/environments/dev apply -var-file="terraform.tfvars"
```

## Prod Apply Flow

When you are ready to separate production from dev, the equivalent prod flow is:

```powershell
.\scripts\build-api-lambda.ps1
terraform -chdir=infra/terraform/environments/prod init -backend-config="backend.hcl"
terraform -chdir=infra/terraform/environments/prod plan -var-file="terraform.tfvars"
terraform -chdir=infra/terraform/environments/prod apply -var-file="terraform.tfvars"
```

## First-Time Operator Checklist

From your side, here is the step-by-step flow:

1. Install prerequisites locally:
   - AWS CLI
   - Terraform
   - Node.js/npm

2. Configure AWS credentials:
   - `aws configure`
   - or use AWS SSO / named profiles if you prefer

3. Pick values you want to use:
   - AWS region, for example `ca-central-1`
   - Terraform state bucket name, which must be globally unique
   - Terraform lock table name
   - Cognito domain prefix, which must also be unique in AWS

4. Bootstrap the Terraform backend:

```powershell
.\scripts\bootstrap-terraform-backend.ps1 -BucketName <your-terraform-state-bucket> -LockTableName <your-terraform-locks-table> -Region <your-aws-region>
```

5. Create your local backend config:

```powershell
Copy-Item infra\terraform\environments\dev\backend.hcl.example infra\terraform\environments\dev\backend.hcl
```

6. Edit `infra/terraform/environments/dev/backend.hcl` with your real values.

7. Create your local Terraform variable file:

```powershell
Copy-Item infra\terraform\environments\dev\terraform.tfvars.example infra\terraform\environments\dev\terraform.tfvars
```

8. Edit `infra/terraform/environments/dev/terraform.tfvars` with your real values.
   - At minimum, set `aws_region` and `cognito_domain_prefix`
   - Update callback/logout URLs later if your auth flow changes

9. Build the Lambda deployment zip:

```powershell
.\scripts\build-api-lambda.ps1
```

10. Initialize Terraform using the remote backend:

```powershell
terraform -chdir=infra/terraform/environments/dev init -backend-config="backend.hcl"
```

11. Review the plan:

```powershell
terraform -chdir=infra/terraform/environments/dev plan -var-file="terraform.tfvars"
```

12. Apply when the plan looks right:

```powershell
terraform -chdir=infra/terraform/environments/dev apply -var-file="terraform.tfvars"
```

13. Capture the outputs you’ll need for the apps:
   - `cognito_user_pool_id`
   - `cognito_web_client_id`
   - `cognito_android_client_id`
   - `cognito_user_pool_domain`
   - `api_base_url`

## Important Outputs

After apply, useful outputs include:

- `cognito_user_pool_id`
- `cognito_web_client_id`
- `cognito_android_client_id`
- `cognito_user_pool_domain`
- `api_base_url`
- `sync_changes_table_name`

## Notes

- Stored application data now uses a customer-managed KMS key instead of only service-default encryption.
- Lambda log groups are pre-created with retention and KMS encryption so production logs follow the same storage policy.
- Pre-created encrypted Lambda log groups are optional and should be enabled only on clean environments or after importing existing log groups into Terraform state.
- Google federation is still intentionally out of scope for this first pass.
- The Android and web apps still need Cognito client integration and token handling.
- The Lambda package currently vendors `zod` and the shared package, while relying on the AWS Lambda Node.js runtime's included AWS SDK v3. See AWS Lambda Node.js runtime docs for the runtime-included SDK behavior: [Building Lambda functions with Node.js](https://docs.aws.amazon.com/lambda/latest/dg/lambda-nodejs.html).
- `backend.hcl` and `terraform.tfvars` are intended to stay local and are ignored by git.
- Right now it is fine to keep both Android flavors pointed at the `dev` Terraform environment until you are ready to stand up real production AWS resources.
