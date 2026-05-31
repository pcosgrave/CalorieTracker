# GitHub Actions Release Setup

This repo now has three GitHub Actions workflows:

- [`.github/workflows/deploy-dev-internal.yml`](/D:/Projects/CalorieTracker/.github/workflows/deploy-dev-internal.yml)
- [`.github/workflows/deploy-prod.yml`](/D:/Projects/CalorieTracker/.github/workflows/deploy-prod.yml)
- [`.github/workflows/_deploy-mobile-and-infra.yml`](/D:/Projects/CalorieTracker/.github/workflows/_deploy-mobile-and-infra.yml)

The intended branch model is:

- `main` branch
  - all normal feature work lands here first
  - release PRs are opened from `main` into `dev` or `prod`

- `dev` branch
  - receives PRs from `main` only
  - when a PR into `dev` is merged, GitHub Actions automatically deploys dev
  - applies `infra/terraform/environments/dev`
  - builds Android `devRelease`
  - uploads `com.cosgravelabs.bitewise` to the Google Play `internal` track

- `prod` branch
  - receives PRs from `dev` only
  - when a PR into `prod` is merged, GitHub Actions automatically deploys prod
  - applies `infra/terraform/environments/prod`
  - builds Android `prodRelease`
  - uploads `com.cosgravelabs.bitewise` to the Google Play `production` track

## 1. Create The Branches

From the repo root:

```powershell
git checkout main
git pull
git checkout -b dev
git push -u origin dev
git checkout main
git checkout -b prod
git push -u origin prod
git checkout main
```

Recommended:

- protect `main`
- protect `dev`
- protect `prod`
- require PRs into both
- require PRs from `main` into `dev`
- require PRs from `dev` into `prod`
- use GitHub Environment protection on `prod`

## 2. Create The Play Console App

The current release setup uses a single Play Console app:

- App package: `com.cosgravelabs.bitewise`

Create or confirm that app in Play Console:

1. Open [Google Play Console](https://play.google.com/console).
2. Create or open the app for `com.cosgravelabs.bitewise`.
3. Enable the `Internal testing` track for dev releases.
4. Complete the normal production app setup checklist for prod releases.

## 3. Set Up Google Play Auth

The workflow uses a Google service-account JSON key stored in GitHub secrets.

### 3.1 Create Or Reuse A Google Cloud Project

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Pick the project linked to your Play Console setup, or create one just for Play automation.

### 3.2 Create A Service Account

1. Go to `IAM & Admin` -> `Service Accounts`.
2. Click `Create service account`.
3. Name it something like `github-play-deployer`.
4. Create it.

### 3.3 Create A JSON Key

1. Open the new service account.
2. Go to `Keys`.
3. `Add key` -> `Create new key`.
4. Choose `JSON`.
5. Download the JSON file.

This JSON file will become the GitHub secret:

- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`

### 3.4 Link The Service Account To Play Console

1. Open Play Console.
2. Go to `Users and permissions`.
3. Click `Invite new users`.
4. Use the service-account email from the JSON.
5. Grant the minimum app permissions needed to upload releases.

Recommended app permissions:

- `Release to testing tracks`
- `Release to production`
- `View app information`

If your Play Console uses the newer API access flow:

1. Open `Setup` -> `API access`.
2. Link the Google Cloud project if it is not linked yet.
3. Grant the service account access to `com.cosgravelabs.bitewise`.

## 4. Set Up AWS IAM Auth With GitHub OIDC

The workflow uses GitHub OIDC and `aws-actions/configure-aws-credentials`.

That means:

- no AWS access keys in GitHub
- GitHub exchanges its OIDC token for an AWS role

### 4.1 Create The GitHub OIDC Provider In AWS

In AWS:

1. Open `IAM` -> `Identity providers`.
2. Click `Add provider`.
3. Provider type: `OpenID Connect`.
4. Provider URL:

```text
https://token.actions.githubusercontent.com
```

5. Audience:

```text
sts.amazonaws.com
```

### 4.2 Create Two IAM Roles

Create one role per GitHub environment:

- `GitHubActionsBiteWiseDevDeploy`
- `GitHubActionsBiteWiseProdDeploy`

Suggested trust policy for `dev`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<AWS_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:<GITHUB_OWNER>/<GITHUB_REPO>:environment:dev"
        }
      }
    }
  ]
}
```

Suggested trust policy for `prod`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<AWS_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:<GITHUB_OWNER>/<GITHUB_REPO>:environment:prod"
        }
      }
    }
  ]
}
```

### 4.3 Permissions To Attach To The Roles

Terraform needs enough access to manage:

- S3
- DynamoDB
- Lambda
- API Gateway
- Cognito
- IAM
- CloudWatch Logs
- KMS

Practical first pass:

- attach `AdministratorAccess`
- verify releases work
- tighten later

## 5. Create GitHub Environments

In GitHub:

1. Open repo `Settings`.
2. Open `Environments`.
3. Create:
   - `dev`
   - `prod`

Recommended:

- `prod` should require reviewers
- `dev` can be lightly protected

## 6. Add GitHub Environment Variables And Secrets

### 6.1 Secrets For `dev`

Create these secrets on environment `dev`:

- `AWS_ROLE_TO_ASSUME`
- `TF_STATE_BUCKET`
- `TF_LOCK_TABLE`
- `TF_GOOGLE_CLIENT_SECRET`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- `BW_UPLOAD_STORE_FILE_BASE64`
- `BW_UPLOAD_STORE_PASSWORD`
- `BW_UPLOAD_KEY_ALIAS`
- `BW_UPLOAD_KEY_PASSWORD`

### 6.2 Variables For `dev`

Create these variables on environment `dev`:

- `AWS_REGION`
- `TF_APP_NAME`
- `TF_COGNITO_DOMAIN_PREFIX`
- `TF_WEB_CALLBACK_URL`
- `TF_WEB_LOGOUT_URL`
- `TF_GOOGLE_CLIENT_ID`
- `TF_GOOGLE_AUTHORIZE_SCOPES`
- `ANDROID_AWS_REGION`
- `ANDROID_COGNITO_DOMAIN`
- `ANDROID_COGNITO_USER_POOL_ID`
- `ANDROID_COGNITO_ANDROID_CLIENT_ID`
- `ANDROID_SYNC_API_BASE_URL`
- `ANDROID_COGNITO_ANDROID_REDIRECT_URI`
- `ANDROID_COGNITO_ANDROID_LOGOUT_URI`

### 6.3 Secrets For `prod`

Create these secrets on environment `prod`:

- `AWS_ROLE_TO_ASSUME`
- `TF_STATE_BUCKET`
- `TF_LOCK_TABLE`
- `TF_GOOGLE_CLIENT_SECRET`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- `BW_UPLOAD_STORE_FILE_BASE64`
- `BW_UPLOAD_STORE_PASSWORD`
- `BW_UPLOAD_KEY_ALIAS`
- `BW_UPLOAD_KEY_PASSWORD`

### 6.4 Variables For `prod`

Create these variables on environment `prod`:

- `AWS_REGION`
- `TF_APP_NAME`
- `TF_COGNITO_DOMAIN_PREFIX`
- `TF_WEB_CALLBACK_URL`
- `TF_WEB_LOGOUT_URL`
- `TF_GOOGLE_CLIENT_ID`
- `TF_GOOGLE_AUTHORIZE_SCOPES`
- `TF_LOG_RETENTION_DAYS`
- `ANDROID_AWS_REGION`
- `ANDROID_COGNITO_DOMAIN`
- `ANDROID_COGNITO_USER_POOL_ID`
- `ANDROID_COGNITO_ANDROID_CLIENT_ID`
- `ANDROID_SYNC_API_BASE_URL`
- `ANDROID_COGNITO_ANDROID_REDIRECT_URI`
- `ANDROID_COGNITO_ANDROID_LOGOUT_URI`

### 6.5 Repo Settings Needed For Prod Version Bumps And Tags

The release workflows now:

- bumps `apps/android/app/build.gradle.kts`
- commits that version bump back to the target branch

Additionally, the prod workflow:

- creates a git tag for the release

To allow that to work, make sure:

1. the workflow has `contents: write` permissions
2. your repository or branch protection allows GitHub Actions to push to `dev` and `prod`
3. tag creation is allowed from GitHub Actions for prod releases

If `prod` is protected, the easiest setup is:

- allow GitHub Actions to bypass branch protection for this workflow, or
- use a dedicated bot/PAT approach later if you want tighter control

No extra GitHub secret is required for tagging if `GITHUB_TOKEN` with `contents: write` is allowed to push.

## 7. Convert The Android Keystore To Base64

From PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("D:\path\to\upload-keystore.jks")) | Set-Clipboard
```

Paste that into:

- `BW_UPLOAD_STORE_FILE_BASE64`

## 8. First Dev Release Test

1. Merge these workflow files into `main`.
2. Open a PR from `main` into `dev`.
3. Merge that PR.
4. Open the Actions tab and watch `Deploy Dev Internal` run automatically.

Expected result:

- Terraform `dev` applies
- Lambda package is built
- Android `devRelease` AAB is built
- `com.cosgravelabs.bitewise` uploads to the Play internal testing track

## 9. First Prod Release Test

1. Open a PR from `dev` into `prod`.
2. Review it carefully.
3. Merge that PR.
4. Open the Actions tab and watch `Deploy Prod` run automatically.

Expected result:

- Terraform `prod` applies
- Android `prodRelease` AAB is built
- `com.cosgravelabs.bitewise` uploads to Play production

## 10. Important Notes

- The prod workflow currently uploads directly to the `production` track.
- If you want a safer first rollout, change [`.github/workflows/deploy-prod.yml`](/D:/Projects/CalorieTracker/.github/workflows/deploy-prod.yml) to use `internal` or `closed` first.
- Deploys happen only when a pull request is merged into `dev` or `prod`, not when a PR is merely opened.
- `Deploy Dev Internal` only accepts `main -> dev` merges.
- `Deploy Prod` only accepts `dev -> prod` merges.
- Dev deploys now auto-bump the Android release version, commit that bump back to `dev`, and create a tag like `android-dev-v0.1.2+3`.
- Prod deploys now auto-bump the Android release version, commit that bump back to `prod`, and create a tag like `android-prod-v0.1.2+3`.
- The workflows generate temporary Terraform backend and tfvars files during the run.
- The workflows use GitHub environment variables and secrets instead of local `secure.properties`, `release.properties`, `backend.hcl`, or `terraform.tfvars`.
