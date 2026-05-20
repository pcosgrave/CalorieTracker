# CalorieTracker

Temporary project name for a local-first calorie tracking app.

## Phase 1

- Manual food entry
- Private per-user barcode mappings
- Web app and Android app
- Google sign-in through Cognito
- DynamoDB-backed cloud sync path for development/paid-mode iteration
- Terraform-managed AWS infrastructure in `us-east-1`

## Repository Layout

- `apps/web`: Next.js web app
- `apps/api`: TypeScript AWS Lambda API
- `apps/android`: Android Kotlin/Jetpack Compose app skeleton
- `shared`: Shared TypeScript domain contracts
- `infra/terraform`: AWS infrastructure
- `docs`: Product and architecture notes

## Development Setup

### Required Tools

- Node.js and npm
- Terraform
- Android Studio for Android development
- AWS CLI for deployed infrastructure work

On macOS, install Node.js and npm with Homebrew:

```bash
brew install node
```

Confirm Node.js and npm are available:

```bash
node --version
npm --version
```

### Install Dependencies

Install JavaScript/TypeScript workspace dependencies from the repo root:

```bash
npm install
```

TypeScript is installed locally through the workspace dependencies. Do not install TypeScript globally for this project.

### Verify The Workspace

Run the TypeScript checks:

```bash
npm run typecheck
```

Check for whitespace or patch formatting issues:

```bash
git diff --check
```

Format Terraform files:

```bash
terraform fmt -recursive infra/terraform
```

### Web App

Start the Next.js development server:

```bash
npm run dev --workspace @calorie-tracker/web
```

The web app should be available at `http://localhost:3000`.

### API

The API package currently contains Lambda handlers and shared request validation. It is not wired to a local API server yet.

Run its typecheck directly:

```bash
npm run typecheck --workspace @calorie-tracker/api
```

### Android App

Open `apps/android` in Android Studio. The temporary Android package name is:

```text
com.philipcosgrave.calorietracker
```

Android barcode scanning is planned around Google ML Kit.

### Terraform

The first Terraform environment is:

```text
infra/terraform/environments/dev
```

It targets `us-east-1` by default and defines the initial Cognito, DynamoDB, and S3 resources. Google federation is not wired yet because it needs real Google OAuth credentials.
