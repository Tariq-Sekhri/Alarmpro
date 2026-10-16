# Google Play publishing

Every push to `main` runs `.github/workflows/android-play-release.yml`, builds a
signed Android App Bundle, and creates a completed release on **Internal testing**.
Production releases remain a separate promotion in Play Console.

The workflow can also be started from GitHub Actions with **Run workflow** on
`main`. Each new run gets version code `100000 + GITHUB_RUN_NUMBER` and a version
name based on `app/build.gradle.kts`, such as `0.8.1-ci.42`. A rerun keeps the same
version code; start a new run if that code has already been uploaded to Play.
Future manually uploaded bundles must also have a higher version code than the
last CI upload.

GitHub Actions repository secrets:

- `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and
  `ANDROID_KEY_PASSWORD`: existing upload-key signing configuration.
- `GCP_SERVICE_ACCOUNT`: the `github-play-deploy` service account in Cloud project
  `project-33477e16-5086-4c11-933`.
- `GCP_WORKLOAD_IDENTITY_PROVIDER`:
  `projects/837350808864/locations/global/workloadIdentityPools/alarmpro-github/providers/github`.

Google authentication uses GitHub OIDC and short-lived service account
impersonation, without a downloaded service account key. The provider and account
binding restrict access to `repo:Tariq-Sekhri/Alarmpro:ref:refs/heads/main`.
Play Console permissions are limited to viewing Alarm Pro and releasing it to
testing tracks.

To diagnose a failure, inspect the failed GitHub Actions step. The signed bundle
is also retained as a workflow artifact for 30 days after a successful build.
