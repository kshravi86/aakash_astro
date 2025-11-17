# GitHub Actions Workflows

This repository ships two automation pipelines under `.github/workflows/`. Both are designed to be self-contained (no external secrets) so forks can run them as-is. This document explains when they run, what each step accomplishes, and how to adapt them safely.

| File | Trigger | Purpose | Outputs |
| --- | --- | --- | --- |
| `build-apk.yml` | Push or PR against `main`/`master`, manual dispatch | Compile debug/release APKs on Ubuntu, upload artifacts for testers/CI consumers. | `app-debug` and `app-release-unsigned` artifacts. |
| `screenshot-capture.yml` | Manual dispatch only | Spin up a macOS runner with an Android emulator, build the debug APK, run `scripts/capture_screenshots.sh`, and collect PNG screenshots. | `android-screenshots` artifact containing Play Store-ready PNGs. |

---

## 1. `build-apk.yml`

**Trigger matrix**

```yaml
on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:
```

Every push/PR to `main` or `master` builds automatically, ensuring reviewers always have fresh APKs. The optional `workflow_dispatch` lets you rebuild on demand (e.g., after updating Gradle caches).

**Job details**

| Step | Explanation |
| --- | --- |
| `actions/checkout@v4` | Fetches the entire repo so Gradle and asset files are available. |
| `actions/setup-java@v4` (Temurin 17) | Matches the JDK version declared in Gradle (`compileOptions` → Java 11 but toolchain compiles fine with JDK 17). |
| `actions/cache@v4` for Gradle | Reuses wrapper + dependency caches between runs; cache key hashes every `*.gradle*` and `gradle-wrapper.properties` file. |
| `chmod +x gradlew` | Required on Linux runners because the executable bit is not preserved by Git on Windows commits. |
| `./gradlew assembleDebug` | Builds `app/build/outputs/apk/debug/app-debug.apk` (used both for testing and screenshot capture). |
| `actions/upload-artifact@v4` | Publishes `app-debug` artifact so reviewers can download the APK directly from the Actions page. |
| `./gradlew assembleRelease` with `continue-on-error: true` | Produces an unsigned release APK when possible. The `continue-on-error` flag prevents the entire workflow from failing if a release build is not configured (e.g., missing signing props). |
| `actions/upload-artifact@v4` (conditional) | Uploads `app-release-unsigned` only if the prior job succeeded. |

**Customization tips**

- To enforce release builds (fail fast), remove `continue-on-error: true` once `keystore.properties` is available in CI.
- Add `-Pandroid.injected.signing.store.file=...` style Gradle properties via encrypted secrets if you want fully signed artifacts.
- Extend the workflow with `./gradlew lint test` before the assemble steps if you want static analysis coverage per PR.

---

## 2. `screenshot-capture.yml`

This workflow is intentionally **manual** (`workflow_dispatch` only) because it launches a macOS runner (higher cost) and burns emulator minutes. Recommended for preparing Play Store imagery or marketing assets.

| Step | Explanation |
| --- | --- |
| `runs-on: macos-13` | Required because the `reactivecircus/android-emulator-runner` currently relies on macOS for stable hardware-accelerated emulators. `timeout-minutes: 60` guards against hung emulator boots. |
| `actions/checkout@v4` | Gets both the Android project and the `scripts/capture_screenshots.sh` helper. |
| `actions/setup-java@v4` (Temurin 17) | Same toolchain as the build workflow. |
| `actions/cache@v4` | Reuses Gradle caches between screenshot sessions. |
| `chmod +x gradlew scripts/capture_screenshots.sh` | Ensures both the Gradle wrapper and shell script are executable on macOS runners. |
| `./gradlew assembleDebug` | Produces the APK consumed by the emulator step. |
| `reactivecircus/android-emulator-runner@v2` | Boots a Pixel 5 (API 30, `google_apis`, x86_64). Options disable snapshots, audio, and UI to keep runs deterministic. Once the emulator is up, it executes `./scripts/capture_screenshots.sh`. The script installs the debug APK, launches several activities, waits eight seconds each, and saves PNGs under `artifacts/screenshots/`. |
| `actions/upload-artifact@v4` | Gathers everything under `artifacts/screenshots/` into the `android-screenshots` artifact (fails the workflow if the directory is empty). |

**Script expectations (`scripts/capture_screenshots.sh`)**

1. Requires `adb` on `PATH` (already provided by the emulator runner).
2. Installs `app/build/outputs/apk/debug/app-debug.apk`.
3. Opens `MainActivity`, `PrivacyActivity`, and `SavedHoroscopesActivity`, then captures each screen via `adb exec-out screencap -p`.
4. Stores PNGs alongside console logs documenting capture progress.

**Adapting the workflow**

- Change `api-level`, `profile`, or the activities launched inside the script to target different devices.
- Add extra `capture_screen` invocations in the script if you need additional screens (e.g., Tara Bala, Sarvatobhadra).
- If emulator boot speed is an issue, raise `emulator-boot-timeout` (in seconds) or pin to an earlier API level.

---

## 3. Common Patterns & Maintenance

1. **Caching** – Both workflows use the same Gradle cache key. When you bump AGP/Kotlin versions or clean the wrapper, the cache automatically invalidates because the hash includes `gradle-wrapper.properties` and any `*.gradle*` file.
2. **Toolchain parity** – Keep `java-version` aligned with local development. If the project adopts Kotlin/AGP versions requiring a newer JDK, update both workflow files together.
3. **Artifacts retention** – GitHub retains artifacts for 90 days by default. Override via the `retention-days` input on `actions/upload-artifact` if you want shorter retention for screenshots.
4. **Manual runs** – Use the GitHub UI (“Run workflow” dropdown) to populate optional inputs. Neither workflow currently accepts parameters, but you can add them to `workflow_dispatch` sections if you need toggles (e.g., choose emulator API level).
5. **Security** – No secrets are referenced. If you introduce signing or third-party uploads, store credentials as encrypted repository secrets and reference them via `${{ secrets.MY_SECRET }}`.

---

## 4. Troubleshooting

| Failure | Likely Cause | Fix |
| --- | --- | --- |
| `chmod +x gradlew` step fails | Repo checked out with LF-normalized scripts on Windows without executable bit. | Ensure `core.autocrlf` doesn’t modify shell scripts, or set `git update-index --chmod=+x gradlew`. |
| `assembleRelease` fails in `build-apk.yml` | Release signing config missing `keystore.properties`. | Provide the file or accept the failure (workflow continues). |
| Screenshot job times out | Emulator boot exceeds 15 minutes or script hangs waiting for UI. | Increase `emulator-boot-timeout`, reduce number of activities in the script, or use `-gpu host` to speed up rendering. |
| `if-no-files-found: error` fires in screenshot upload | `scripts/capture_screenshots.sh` didn’t produce any PNGs (APK build failed or app crashed). | Inspect the job logs; fix the root cause before rerunning. |

Keep this document synchronized with the workflow files whenever you change triggers, steps, or supporting scripts.
