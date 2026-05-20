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

## Testing Locally

### Preview The Web App

From the repo root, start the Next.js dev server:

```bash
npm run dev --workspace @calorie-tracker/web
```

Then open:

```text
http://localhost:3000
```

The current web app is a static shell for the Phase 1 workflow. It shows daily totals, a sample logged food, and the manual label-entry form. The form is not connected to the API yet.

If port `3000` is busy, choose another port:

```bash
npm run dev --workspace @calorie-tracker/web -- -p 3001
```

Build the web app:

```bash
npm run build --workspace @calorie-tracker/web
```

### Test The Android App

Open `apps/android` in Android Studio, let Gradle sync, then run the `app` configuration on an emulator or device.

The current Android app is also a static shell. It includes the temporary package name and declares the ML Kit barcode scanning dependency, but the camera scanner and persistence are not wired yet.

The temporary Android package name is:

```text
com.philipcosgrave.calorietracker
```

If running Gradle from the command line on macOS, use Android Studio's bundled JDK:

```bash
cd apps/android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

If Gradle cannot write to the default home cache in a restricted environment, keep the Gradle cache inside the project:

```bash
cd apps/android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
GRADLE_USER_HOME="$PWD/.gradle-user-home" \
./gradlew :app:assembleDebug
```

### Test The API

Run API typechecks:

```bash
npm run typecheck --workspace @calorie-tracker/api
```

The API currently contains Lambda handlers, validation, and DynamoDB store code. It does not have a local HTTP runner yet.

### Terraform

The first Terraform environment is:

```text
infra/terraform/environments/dev
```

It targets `us-east-1` by default and defines the initial Cognito, DynamoDB, and S3 resources. Google federation is not wired yet because it needs real Google OAuth credentials.
