# Automated Play publishing

`./gradlew :app:publishPlaystoreReleaseBundle` builds the signed AAB and uploads it
to the production track, release notes included, without touching a browser.

Uploads are created as **drafts**. Nothing is sent to Google for review until a
human presses the button in Play Console.

## One-time setup

These steps need a person — they involve creating credentials, which is not
something to automate or hand off.

### 1. Google Cloud — service account and key

1. Open <https://console.cloud.google.com/> and select (or create) a project.
2. **APIs & Services → Library** → enable **Google Play Android Developer API**.
3. **IAM & Admin → Service Accounts → Create service account**.
   Name it something obvious, e.g. `geardex-play-publisher`. No roles are needed
   at the Cloud level — permissions are granted in Play Console instead.
4. Open the new service account → **Keys → Add key → Create new key → JSON**.
   A `.json` file downloads. Treat it like a password.
5. Copy the service account's email address; it looks like
   `geardex-play-publisher@<project>.iam.gserviceaccount.com`.

### 2. Play Console — link and authorise

1. Play Console → **Setup → API access** → link the Google Cloud project from
   step 1. This has to be the same project the service account lives in.
2. Play Console → **Users and permissions → Invite new users** → paste the
   service account email.
3. Grant it, scoped to GearDex rather than the whole account:
   - **Release apps to testing tracks**
   - **Release to production, exclude devices, and use Play App Signing**
   - **View app information and download bulk reports**

Permission changes can take a few minutes to take effect.

### 3. Drop the key in place

Save the downloaded JSON as `play-service-account.json` in the repository root,
next to `keystore.properties`.

It is gitignored. Do not commit it, and do not paste its contents anywhere —
anyone holding it can publish to the store as you.

## Usage

```bash
# build, sign and upload the production bundle as a draft
./gradlew :app:publishPlaystoreReleaseBundle

# upload without building, if the AAB is already current
./gradlew :app:publishPlaystoreReleaseBundle -x bundlePlaystoreRelease

# store listing text, screenshots and graphics only
./gradlew :app:publishPlaystoreReleaseListing
```

Then open Play Console, review the draft release, and submit it for review.

## How it is wired

Configuration lives in `app/build.gradle.kts` under the `play { }` block.

- `track` is `production`; `releaseStatus` is `DRAFT`.
- `defaultToAppBundles` is on, so the AAB is used rather than an APK.
- The `github` flavour is disabled in `playConfigs` — it ships outside Play and
  must never be uploaded there.
- Release notes come from `app/src/playstore/play/release-notes/<locale>/production.txt`,
  one file per store locale (`en-US`, `el-GR`, `de-DE`, `fr-FR`, `it-IT`, `es-ES`).
  Play caps each at 500 characters.

To promote a draft to a live rollout from the command line instead of the
console, change `releaseStatus` to `COMPLETED`. Leaving it on `DRAFT` is the
safer default: it keeps a human between the build and the store.

## Version bumps

`versionCode` must increase on every upload. It lives in `app/build.gradle.kts`
under `defaultConfig`. Play rejects a duplicate version code before the upload
finishes, so a stale value fails fast rather than publishing the wrong thing.

## Requirements

Gradle Play Publisher **3.13.0**. Version 4.x requires Gradle 9.1+, while this
project is on Gradle 8.11.1 — do not bump the plugin without upgrading the
wrapper and re-checking AGP compatibility first.
