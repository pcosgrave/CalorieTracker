# Android Dev And Publishing Guide

This document covers both Android workflows for `BiteWise`:

- a local `dev` workflow you can keep using while Google verification is pending
- a `prod` workflow for the publishable Play Console build

## Android Flavors

### Dev flavor

- App name: `BiteWise Dev`
- Application ID: `com.cosgravelabs.bitewise.dev`
- Android callback URI: `bitewise-dev://auth/callback`
- Android logout URI: `bitewise-dev://signout`
- Suggested use: local testing, dev AWS/Cognito stack, side-by-side install

### Prod flavor

- App name: `BiteWise`
- Application ID: `com.cosgravelabs.bitewise`
- Android callback URI: `bitewise://auth/callback`
- Android logout URI: `bitewise://signout`
- Suggested use: release bundle for Google Play

## One-Time Setup

### 1. Create a release keystore

If you do not already have a release/upload keystore, create one with `keytool`.

Example:

```powershell
& "C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot\bin\keytool.exe" `
  -genkeypair `
  -v `
  -keystore "C:\Users\Philip\keys\bitewise-upload-key.jks" `
  -alias bitewise-upload `
  -keyalg RSA `
  -keysize 2048 `
  -validity 9125
```

Save the following securely:

- keystore path
- keystore password
- key alias
- key password

### 2. Create local Android config files

Create these local files if they do not already exist:

- `apps/android/secure.properties`
- `apps/android/release.properties`

Both are git-ignored.

#### `apps/android/secure.properties`

Copy from:

```text
apps/android/secure.properties.example
```

Fill in the flavor-specific values:

- `CT_DEV_AWS_REGION`
- `CT_DEV_COGNITO_DOMAIN`
- `CT_DEV_COGNITO_USER_POOL_ID`
- `CT_DEV_COGNITO_ANDROID_CLIENT_ID`
- `CT_DEV_SYNC_API_BASE_URL`
- `CT_DEV_COGNITO_ANDROID_REDIRECT_URI`
- `CT_DEV_COGNITO_ANDROID_LOGOUT_URI`
- `CT_PROD_AWS_REGION`
- `CT_PROD_COGNITO_DOMAIN`
- `CT_PROD_COGNITO_USER_POOL_ID`
- `CT_PROD_COGNITO_ANDROID_CLIENT_ID`
- `CT_PROD_SYNC_API_BASE_URL`
- `CT_PROD_COGNITO_ANDROID_REDIRECT_URI`
- `CT_PROD_COGNITO_ANDROID_LOGOUT_URI`

Recommended current setup while prod is still pending:

```text
CT_DEV_COGNITO_DOMAIN=cosgravelabs-bitewise-dev
CT_DEV_SYNC_API_BASE_URL=https://example.execute-api.us-east-1.amazonaws.com/dev
CT_DEV_COGNITO_ANDROID_REDIRECT_URI=bitewise-dev://auth/callback
CT_DEV_COGNITO_ANDROID_LOGOUT_URI=bitewise-dev://signout

CT_PROD_COGNITO_DOMAIN=cosgravelabs-bitewise-dev
CT_PROD_SYNC_API_BASE_URL=https://example.execute-api.us-east-1.amazonaws.com/dev
CT_PROD_COGNITO_ANDROID_REDIRECT_URI=bitewise://auth/callback
CT_PROD_COGNITO_ANDROID_LOGOUT_URI=bitewise://signout
```

The `CT_PROD_*` values can temporarily point at the same backend as `dev`, then move to true production values later.

#### `apps/android/release.properties`

Copy from:

```text
apps/android/release.properties.example
```

Fill in:

```text
BW_UPLOAD_STORE_FILE=C:\path\to\bitewise-upload-key.jks
BW_UPLOAD_STORE_PASSWORD=your-store-password
BW_UPLOAD_KEY_ALIAS=your-key-alias
BW_UPLOAD_KEY_PASSWORD=your-key-password
```

## Local Dev Workflow

Use this while waiting on Google verification or whenever you want a side-by-side testing app.

Run this from the repo root:

```powershell
.\scripts\build-android-dev.ps1
```

Optional clean build:

```powershell
.\scripts\build-android-dev.ps1 -Clean
```

Expected output:

```text
apps/android/app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

Install the resulting APK manually on a device or emulator. Because the `dev` flavor has a different application ID and callback scheme, it can coexist with the prod app.

## Prod Workflow

Run this from the repo root:

```powershell
.\scripts\build-android-release.ps1
```

Optional clean build:

```powershell
.\scripts\build-android-release.ps1 -Clean
```

Expected output bundle:

```text
apps/android/app/build/outputs/bundle/prodRelease/app-prod-release.aab
```

The release script also copies the finished bundle into a repo folder you can commit for tagging/releases:

```text
releases/android/BiteWise-v<versionName>+<versionCode>.aab
releases/android/BiteWise-v<versionName>+<versionCode>.json
```

The JSON file includes the version, application ID, build time, and SHA-256 hash for that exact bundle.

## Play Console Publishing Steps

### 1. Create the app in Play Console

In your Google Play developer account:

1. Create a new app
2. App name: `BiteWise`
3. Default language: your choice
4. App or game: `App`
5. Free or paid: your choice

### 2. Enroll in Play App Signing

Google Play requires Android App Bundles for new apps and uses Play App Signing for distribution.

When prompted during first upload:

1. Enroll in Play App Signing
2. Use your local upload key / upload bundle as directed

After enrollment, Google Play will show:

- App signing certificate
- Upload certificate

These can later be found in:

```text
Play Console -> Release -> Setup -> App integrity
```

### 3. Upload the `.aab`

Recommended first release path:

- `Internal testing`, or
- `Closed testing`

Steps:

1. Go to `Test and release`
2. Choose `Internal testing` or `Closed testing`
3. Create release
4. Upload:

```text
releases/android/BiteWise-v<versionName>+<versionCode>.aab
```

5. Save

### 4. Complete required Play Console forms

Before production release, complete:

- App access
- Ads declaration
- Data safety
- Content rating
- Target audience and content
- Privacy policy URL

### 5. Add store listing assets

Prepare:

- app icon
- screenshots
- feature graphic
- short description
- full description

### 6. Review auth-related settings

Because BiteWise uses Cognito hosted login:

#### Dev callback URLs

- `bitewise-dev://auth/callback`
- `bitewise-dev://signout`

#### Prod callback URLs

- `bitewise://auth/callback`
- `bitewise://signout`

#### Web/local callback URLs

- `http://localhost:3000/auth/callback`
- `http://localhost:3000/`

#### Hosted domain

- `cosgravelabs-bitewise-dev.auth.us-east-1.amazoncognito.com`

#### Google authorized redirect URI

```text
https://cosgravelabs-bitewise-dev.auth.us-east-1.amazoncognito.com/oauth2/idpresponse
```

### 7. Retrieve SHA fingerprints after Play enrollment

If any Google or other SDK setup needs your Play signing certificate fingerprint:

1. Open Play Console
2. Go to `Release -> Setup -> App integrity`
3. Copy the SHA-1 or SHA-256 from the relevant certificate section

Important:

- the local debug keystore fingerprint is not the same thing as the Play signing certificate
- the upload key may also differ from the Play signing key

## Verification Commands

Before building a release, it is useful to run:

```powershell
.\apps\android\gradlew.bat :app:compileDevDebugKotlin
.\apps\android\gradlew.bat :app:compileProdDebugKotlin
npm.cmd run typecheck --workspace @calorie-tracker/web
npm.cmd run typecheck --workspace @calorie-tracker/api
```

## Notes

- `apps/android/release.properties` is local-only and must not be committed.
- `apps/android/secure.properties` is local-only and must not be committed.
- `releases/android/*.aab` can be committed if you want a tag/release artifact in git, but Git LFS is worth considering once you start keeping multiple large bundles.
- Changing the Android `applicationId` creates a different installed app identity on Android devices.
- The `dev` and `prod` flavors can point at different Cognito app clients, domains, and API stages whenever you are ready.
