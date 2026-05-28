# BiteWise

Local-first calorie and weight tracking app by Cosgrave Labs.

## Phase 1

- Manual food entry
- Private per-user barcode mappings
- Web app and Android app
- Google sign-in through Cognito
- DynamoDB-backed cloud sync path for development/paid-mode iteration
- Customer-managed KMS encryption for AWS-stored app data and Lambda log groups
- Terraform-managed AWS infrastructure in `us-east-1`

## Repository Layout

- `apps/web`: Next.js web app
- `apps/api`: TypeScript AWS Lambda API
- `apps/android`: Android Kotlin/Jetpack Compose app skeleton
- `shared`: Shared TypeScript domain contracts
- `infra/terraform`: AWS infrastructure
- `docs`: Product and architecture notes

## Security

`BiteWise` currently uses a server-readable cloud sync model with stronger infrastructure protections:

- HTTPS in transit
- customer-managed KMS encryption for DynamoDB, S3, and Lambda log groups
- per-user storage partitioning in the backend
- offline-first sync reconciliation on the server

This is not an end-to-end encrypted design. Operators with sufficient AWS permissions can still read stored user data. For the current detailed posture, see:

- [Security And Privacy](./docs/security-and-privacy.md)

## Privacy

Current privacy model:

- user-owned records are stored per authenticated user
- account deletion removes only that user’s owned rows
- community food records are stored separately and are not deleted with a user account
- cloud-stored data is encrypted at rest, but remains server-readable

## Development Setup

### Required Tools

- Node.js and npm
- Terraform
- Android Studio for Android development
- AWS CLI for deployed infrastructure work

On Windows, the recommended package-manager setup is Scoop because it installs command-line tools into your user profile and does not require an Administrator shell:

```powershell
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
Invoke-RestMethod -Uri https://get.scoop.sh | Invoke-Expression
scoop install nodejs-lts terraform aws
```

If this is a fresh PowerShell session, confirm Scoop's shims are on `PATH`:

```powershell
node --version
npm --version
terraform version
aws --version
```

If you prefer official installers instead, install:

- Node.js LTS from <https://nodejs.org/>
- Git for Windows from <https://git-scm.com/download/win>
- Android Studio from <https://developer.android.com/studio>
- Terraform from <https://developer.hashicorp.com/terraform/install>
- AWS CLI v2 from <https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html>

After installing tools, close and reopen PowerShell so `PATH` changes are loaded.

On macOS, install Node.js and npm with Homebrew:

```bash
brew install node
```

Confirm Node.js and npm are available:

```bash
node --version
npm --version
```

On Windows, run the setup check from PowerShell:

```powershell
.\scripts\setup-windows.ps1
```

If PowerShell blocks script execution for this command, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-windows.ps1
```

The setup check reports missing or broken tools. It can also install workspace npm dependencies once Node.js and npm are working:

```powershell
.\scripts\setup-windows.ps1 -InstallDependencies
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

Before the Android app can talk to AWS/Cognito, create a local `apps/android/secure.properties` file from `apps/android/secure.properties.example` and fill in the real `CT_*` values. That file is ignored by git on purpose.

For the current BiteWise setup, the Android callback/logout scheme should be:

```text
bitewise://auth/callback
bitewise://signout
```

The current Android app is also a static shell. It includes the temporary package name and declares the ML Kit barcode scanning dependency, but the camera scanner and persistence are not wired yet.

The Android app currently ships with:

```text
applicationId: com.cosgravelabs.bitewise
display name: BiteWise
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

On Windows, open `apps\android` in Android Studio for the easiest path. To build from PowerShell after Android Studio has installed the SDK, run:

```powershell
cd apps\android
.\gradlew.bat :app:assembleDebug
```

If Gradle cannot find the Android SDK, write a local `local.properties` file from the repo root:

```powershell
.\scripts\setup-windows.ps1 -WriteAndroidLocalProperties
```

Or pass the SDK location explicitly:

```powershell
.\scripts\setup-windows.ps1 -WriteAndroidLocalProperties -AndroidSdkPath "$env:LOCALAPPDATA\Android\Sdk"
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

If you rename the Cognito hosted domain prefix, update:
- `infra/terraform/environments/dev/terraform.tfvars`
- `apps/android/secure.properties`
- any Google OAuth redirect URI that points to `https://<domain>.auth.us-east-1.amazoncognito.com/oauth2/idpresponse`

## Windows Troubleshooting

If `node --version` fails with `Access is denied`, PowerShell is probably finding a packaged app shim instead of a normal Node.js install. Install Node.js LTS from the official installer, reopen PowerShell, and confirm that `Get-Command node -All` shows `C:\Program Files\nodejs\node.exe` before any WindowsApps or app-package paths.

If `npm` is missing after installing Node.js, repair or reinstall Node.js LTS and choose the option that adds Node.js to `PATH`.

If PowerShell blocks `npm.ps1`, either run `Set-ExecutionPolicy RemoteSigned -Scope CurrentUser` or use `npm.cmd` for the same commands, for example `npm.cmd run typecheck`.

If `terraform`, `aws`, or Android SDK commands are missing, install the relevant tool and reopen PowerShell. Terraform and AWS CLI are only required when working with deployed infrastructure.
