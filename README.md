# BiteWise

BiteWise is a local-first calorie, meal, and weight tracking app from Cosgrave Labs. The project currently includes an Android app, a web app, and an AWS-backed sync layer for authenticated cloud backup and cross-device sync.

## Overview

BiteWise is built around a local-first model:

- food, diary, and weight data are created locally first
- changes can sync to AWS when cloud sync is enabled
- Health Connect is used on Android for supported nutrition and health data flows
- community food records can be shared separately from user-owned data

Current areas of focus include:

- food logging and meal management
- weight tracking and charting
- voice-assisted logging
- barcode scanning and food lookup
- Android and web support with shared backend contracts

## Repository Layout

- `apps/android` - Kotlin + Jetpack Compose Android app
- `apps/web` - Next.js web app
- `apps/api` - TypeScript AWS Lambda API
- `shared` - shared TypeScript contracts and domain types
- `infra/terraform` - AWS infrastructure definitions for `dev` and `prod`
- `scripts` - local build and release helpers
- `docs` - product, architecture, release, and security documentation

## Security Notice

BiteWise currently uses a server-readable sync model with stronger infrastructure protections.

- data is sent over HTTPS in transit
- AWS-stored data is encrypted at rest using customer-managed KMS-backed infrastructure
- user-owned data is partitioned per authenticated user in the backend
- sync changes are reconciled server-side for offline-first workflows

This is **not** an end-to-end encrypted system. Anyone with sufficient AWS access to the production environment can still read stored user data.

For more detail, see:

- [Security and Privacy](./docs/security-and-privacy.md)

## Development Setup

### Required Tools

- Node.js and npm
- Terraform
- AWS CLI
- Android Studio for Android development

Optional but recommended:

- Git for Windows on Windows
- Scoop on Windows for easier tool installation

### Install Dependencies

From the repository root:

```bash
npm install
```

### Environment Notes

Before the Android app can connect to Cognito and the API, create:

- `apps/android/secure.properties`

using:

- `apps/android/secure.properties.example`

That file is intentionally ignored by git.

## Testing

### Web

Run the web app locally:

```bash
npm run dev --workspace @calorie-tracker/web
```

Build the web app:

```bash
npm run build --workspace @calorie-tracker/web
```

### Android

Open `apps/android` in Android Studio and run the `app` configuration on an emulator or device.

From PowerShell, you can also build the debug app with:

```powershell
cd apps\android
.\gradlew.bat :app:assembleDebug
```

### API

Run the API typechecks:

```bash
npm run typecheck --workspace @calorie-tracker/api
```

### Workspace Checks

Run the main repository checks from the root:

```bash
npm run typecheck
git diff --check
terraform fmt -recursive infra/terraform
```

## Infrastructure

Terraform environments live in:

- `infra/terraform/environments/dev`
- `infra/terraform/environments/prod`

The AWS stack currently covers:

- Cognito authentication
- DynamoDB-backed app storage
- Lambda-based API handlers
- S3-backed supporting storage

## Additional Documentation

- [Security and Privacy](./docs/security-and-privacy.md)
- [GitHub Actions Release Setup](./docs/github-actions-release-setup.md)
