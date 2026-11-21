# Lumi Community Android CI/CD — operational design

Updated 2026-11-21. This document supersedes the earlier three-runner and
two-runner plans. The final required workflow was verified green at commit
`90f14c54fc74` (CI #125); repository-level merge enforcement is still separate.

## Open-source comparison (GitHub API snapshot, 2026-11-21)

Projects selected for Android relevance, active pushes in November 2026,
stars and visible issue/PR backlog. GitHub `open_issues_count` includes PRs.

| Project | Stars | Open issues + PRs | Inspected workflows and useful patterns |
| --- | ---: | ---: | --- |
| [NewPipe](https://github.com/TeamNewPipe/NewPipe/blob/dev/.github/workflows/ci.yml) | 40,851 | 1,450 | Build APK, lint and JVM tests together; emulator separately |
| [Signal Android](https://github.com/signalapp/Signal-Android/blob/main/.github/workflows/android.yml) | 29,675 | 491 | Pinned Actions, wrapper verification, read-only PR cache, targeted PR checks |
| [Now in Android](https://github.com/android/nowinandroid/blob/main/.github/workflows/Build.yaml) | 22,111 | 281 | Unified local tests + APKs under JDK 21, screenshot baselines, Lint, device matrix |
| [AnkiDroid](https://github.com/ankidroid/Anki-Android/blob/main/.github/workflows/tests_unit.yml) | 12,298 | 603 | JDK 21, dependency prep, JUnit reporting, Gradle cache controls |
| [Bitwarden](https://github.com/bitwarden/android/blob/main/.github/workflows/test.yml) | 9,667 | 197 | Strict sharded tests and reports; protected-key release pipeline in build.yml |
| [AntennaPod](https://github.com/AntennaPod/AntennaPod/blob/develop/.github/workflows/checks.yml) | 8,322 | 398 | Wrapper, analysis, variant tests, emulator and required summary |

The Signal/AnkiDroid/AntennaPod/Bitwarden code bases use AGPL/GPL licenses;
we adapt workflow design independently, not copy their source verbatim.

## Workflow execution and required stages

`.github/workflows/android-beta.yml`: on PR to main, push to main, manual run.
All third-party Actions are SHA pinned; workflow permissions are `contents: read`.
Untrusted PRs never access release keys or execute `pull_request_target`.

1. **Community policy (5m):** privacy and OSS restrictions, localization,
   source/test inventory, shell syntax, Python classifier, integration and
   workflow contract regression tests. Fails closed; no Gradle installation.
2. **Build + test (single Ubuntu 24.04 / JDK 21 / 100m runner):** install API 37,
   Build Tools 37.0.0, NDK r28, CMake 3.22.1; validate edition/package ID;
   probe Hilt POM endpoints; build debug-signed OSS APK; validate package,
   version and SHA-256; run the *reader*, *ui*, *core* JVM test filters
   sequentially in one checkout, reusing Kotlin compilation. XML and HTML
   reports remain isolated/verified for each logical shard.
3. **Static quality:** full Android Lint, diagnostic and unsigned release builds.
   The XML verifier rejects every Error/Fatal and rejects missing or malformed
   reports even if a Gradle process erroneously exits successfully.
4. **API29/API35 emulator matrix:** real instrumentation, Room migration,
   Compose interaction, before/after PNG evidence and independently checked XML.
5. **Required CI summary (`ci-gate`):** depends on policy, verify, quality and
   device-smoke; every upstream dependency must succeed (no skipped jobs).

Use `gradle/actions/setup-gradle` as the sole Gradle user-home cache, writing
only for trusted same-repository PRs or main. External fork PRs are read-only.
No separate JDK 17 APK runner is required for routine PRs: Android Gradle
Plugin 8.13 supports JDK 21 runtime, and existing Robolectric tests require
JDK 21. JDK 17 remains in weekly device and Android Lint workflows.

## Corrective action: Maven POM availability, not just negative caching

Run #77: Hilt 2.58 plugin marker resolution failed on a JVM runner.
Run #80: APK succeeded; 3 real UI tests failed.
Run #81: APK was unable to resolve Hilt despite 3 repeated attempts;
UI reached 789 tests with one failing landscape statistics test.
Run #83: APK passed but a fresh JVM runner had no matching Gradle cache;
Gradle failed on the pinned Hilt implementation before any test.
Run #84: APK failed cold Hilt resolution; reader/core passed, UI had 1 failure.
Run #85: Even with `--refresh-dependencies`, detailed `--info` logs reported
**`Resource missing [HTTP GET: https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android-gradle-plugin/2.58/hilt-android-gradle-plugin-2.58.pom]`**.
The 2.58 POM/JAR are listed in external Maven indexes, but availability to
the failing GitHub runner is not established. Refresh alone cannot fix a
repository returning a missing resource. Hilt 2.59+ cannot be substituted
independently: Dagger's 2.59.1 notes require AGP 9 while Lumi uses AGP 8.13.2.

Source: https://github.com/google/dagger/releases/tag/dagger-2.59.1
Published 2.58 artifact: https://mvnrepository.com/artifact/com.google.dagger/hilt-android-gradle-plugin/2.58
Official Google Cloud-hosted Maven Central mirror:
https://docs.cloud.google.com/docs/buildpacks/java

Resolution strategy:

- Keep Hilt Gradle plugin, runtime and KSP compiler aligned at 2.58 and keep
  AGP/Kotlin/Gradle pinned. `pluginManagement.resolutionStrategy` routes Hilt
  directly to the released implementation artifact to avoid the marker hop.
- Configure official **Google Cloud Maven Central mirror** and Sonatype's
  **repo1.maven.org** endpoint as Dagger-only alternatives, retaining normal
  Maven Central and Google plugin repositories. Do not enable untrusted HTTP,
  JitPack fallback or broad mirrors for unrelated dependencies.
- `tools/ci_hilt_artifact_probe.sh` performs bounded HTTP GET checks for the
  *exact* Hilt POM on the independent endpoints and captures statuses in
  the Step Summary and diagnostics artifact. The probe is informational;
  Gradle's actual dependency resolution remains the pass/fail authority.
- Bounded retry (`tools/ci_gradle_retry.sh`) uses refresh-dependencies only
  for verified pinned-plugin misses or explicit network errors. Arbitrary
  unresolved plugins, compiler errors, assertions, timeouts and OOM are fatal.

## Test and release security

- Required: all 3 logical JVM groups, source partition validation, no empty
  JUnit XML, test failure summaries, privacy/edition checks and APK metadata.
- All verification runs only inside GitHub Actions. PR API 29/35 emulator
  tests exercise real Room/Compose code with image-diff assertions and
  exported PNG evidence. Weekly/manual `.github/workflows/android-emulator-extended.yml`
  adds API 26/33/36 emulators and optional emulator-only Macrobenchmark.
- PR artifacts are **temporary debug-signed**, never release-signed; publish
  APK and SHA256SUMS only after package/version validation. No Play credentials,
  persistent signing keys or release tokens appear in the PR workflow.
  CI never distributes these release variants or reserves additional
  signing/device-farm credentials, outside-device approvals or tests.

## Validation process and known risks

Check the exact GitHub Actions run, not only the aggregate red/green icon.
Review source, Gradle plugin download, cache read/write, and test XML before
each fix; CI is green only when the required summary is `success`. Keep
Draft status until the complete GitHub Actions Required CI Gate is green.
An observed Maven 404 may be a transient CDN event or an artifact distribution
issue, and the mirror may also lag: compare HTTP status per endpoint rather
than blindly upgrading dependencies or disabling tests.

## Expanded mandatory verification — instrumentation, lint, variants and release isolation

Added after the successful #87 baseline (3,148 JVM test cases, zero failures).
The new gate preserves that JVM suite **without reducing package coverage**.
Android-specific checks run only after the APK+JVM job passes so shared
Gradle caches can be read by subsequent CI runners. An APK failure does not
skip JVM diagnostics inside the combined verification job.

| Check | Scope | Blocking on PR? | Evidence |
| --- | --- | --- | --- |
| Community policy + source/test inventory | Privacy, locale, workflow/Gradle/self-test contracts | **Yes** | Tool logs |
| APK + 3 JVM shards (JDK21) | Debug signing, version, SHA-256, 3,148+ tests | **Yes** | APK, signature file, checksums, per-shard JUnit |
| Static Lint + variants | `:app:lintDebug`, `:app:assembleDiagnostic`, `:app:assembleRelease` | **Yes** | Lint HTML/XML, mappings, Gradle task logs |
| Android emulator API29 + API35 | Room folder/migration integration, Compose controls, icon-style snapshot diff | **Yes**, both matrix children | Device JUnit XML, logcat and PNGs |
| API26/33/36 expanded devices | More migrations, welcome, preferences, baseline visual interaction | No; weekly/manual | Instrumentation XML, device screenshots, logcat |
| Release/diagnostic Android Lint | `:app:lintRelease :app:lintDiagnostic` | No; weekly/manual | Lint HTML/XML |
| Reader Macrobenchmark | Separate benchmark instrumentation on latest API | No; explicit manual option | Perf trace/artifacts |

### Device-result integrity

`tools/ci_run_device_smoke.sh` runs explicit instrumentation class names
on hardware-accelerated emulators. The main API29/35 gate requires at least
21 reported JUnit testcases (including real Room schema migration and bundled
font-axis tests) and zero failure/error nodes; the extended suite requires 25.
`tools/ci_verify_android_junit.py` rejects missing TEST-*.xml, malformed XML,
zero tests and failed assertions. A Python regression suite proves these
failure modes. Each device matrix leg retains its own report, Gradle log,
Android logcat, and available screenshot PNGs. Device evidence remains
explicitly distinct from Robolectric JUnit reports.

### Screenshot regression contract

`AppIconScreenshotInstrumentedTest` renders the actual App Icon options UI,
takes before/after PNGs on the emulator, changes the selected icon style
through the UI, validates semantic selection and asserts significant
pixel differences. PNGs on API 29+ are stored through Android MediaStore in
public `Pictures/LumiCI` and pulled before emulator teardown; missing PNG
artifacts now fail the required job. This checks a real interactive visual regression but is
**not** a full golden-image baseline: anti-aliased text, GPU RenderNodes
and OEM drivers are not yet certified against golden screenshots. The
existing reader pixel geometry instrumentation remains in the codebase
and can be promoted separately after stable device benchmarks.

### Security and build-variant gates

PR APKs must pass `aapt` identity/version verification, `apksigner verify`
and Debug certificate inspection, followed by SHA256SUMS generation and
round-trip verification. Only validated debug APKs are uploaded; Release
and Diagnostic APKs are built but *never distributed or signed* from
this PR workflow. Trusted main/same-repository PR may write Gradle basic
cache; fork PR caches are strictly read-only. Workflows have read-only
`GITHUB_TOKEN` and no release secrets. All external actions use pinned SHA.

### Failure handling

An APK failure keeps the required verification job red but the three
JVM shard runner is still invoked (`always && !cancelled`) for independent
diagnostics; JUnit and log artifacts always upload. A Lint failure does
not suppress the subsequent release/diagnostic build check. Each emulator
matrix leg runs with `fail-fast: false` so both APIs report independently.
The final gate depends on policy, verify, quality and device-smoke and
rejects `skipped`, `failure` or `cancelled` for any mandatory stage.

References: [Android Lint official documentation](https://developer.android.com/studio/write/lint),
[Android Emulator Runner](https://github.com/ReactiveCircus/android-emulator-runner),
[GitHub Actions job dependencies](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax).

## Expanded CI #89 findings and correction (2026-11-21)

[Run #89](https://github.com/wxyu71/Lumi_Books/actions/runs/44879743310) proved: Community policy **success**; debug APK, debug certificate and SHA256SUMS verification **success**; Reader 1,153, UI 795, Core 1,204 JVM test cases (**3,152 total, zero failures**); both API29 and API35 Google APIs emulator legs each executed **26 instrumented tests with zero failures**; `assembleDiagnostic` and `assembleRelease` **success**.

It also exposed two real gaps:

1. Full `:app:lintDebug` was not previously in the PR Gate. The first run found **556 pre-existing errors**, 989 warnings and 31 hints, so the strict raw Lint task failed. An earlier version of this PR accepted the known errors using a fingerprint allowlist. **That exception has been removed**: full Lint runs with its normal failure behavior and an independent XML check rejects **every** Error/Fatal finding, including previously known errors. A missing/malformed report or an unrelated Gradle failure also fails. That 556-error count is historical: the errors were subsequently fixed in source,
with one narrow `ResourceType` compatibility exception only for Android 26–28's
real `@font` binary stream access. CI #125 reports **zero errors**. Full HTML/XML remains uploaded and the weekly lint review remains available. Risk categories include NewApi, JavaScript interfaces, App Link URLs and format strings; prioritize those for remediation.
2. The new `AppIconScreenshotInstrumentedTest` passed a real pixel-difference and UI-semantics assertion on API29 and API35 but the initial artifacts contained **no PNGs**: Gradle removed the app-specific external-files directory when instrumentation finished. Screenshots now write through Android 29+ MediaStore into public `Pictures/LumiCI`; the script downloads them before the emulator exits and **fails closed** if no PNG was exported. All required instrumented test *class names* must also appear in parsed JUnit XML; a total test count alone cannot conceal a skipped screenshot suite.

Required checks remain policy, full APK/JVM verify, zero-error
full Android Lint gate, release/diagnostic variant compilation, both
API 29/35 emulator legs with actual exported PNGs and per-class JUnit,
and a strict final summary. Additional API 26/33/36 emulators, variant
Lint and emulator Macrobenchmark are GitHub Actions schedule/dispatch
tasks only. No physical-device, third-party cloud device-farm,
secret/credential or offline acceptance task exists or is reserved.


## CI #90 emulator dependency-resolution regression

Run #90 proved the previous Lint fingerprint budget gate passed (not a zero-error lint run), all 3,152
JVM tests passed, and API 35 completed 26 instrumented tests and persisted
**both** icon-style PNG screenshots. The API 29 Gradle compile failed *before*
instrumented execution because it could not find four JetBrains Compose 1.7.1
POMs (runtime-saveable, ui-geometry, ui-graphics and ui-unit) from the only
configured Google and canonical Maven Central endpoints. The resolver's
required JUnit floor correctly failed closed on zero testcases.

The follow-up restricts alternate official Maven Central endpoints to
`org.jetbrains.compose.*` groups, adds a bounded per-emulator GET probe,
and reuses the verified dependency-only `--refresh-dependencies` retry
for this **specific AndroidTest 1.7.1** miss. Genuine compiler failures,
assertions, OOM and missing test results remain unretryable failures.

### GitHub Actions-only policy

No GitHub workflow invokes a phone, physical test hardware, Firebase Test Lab,
BrowserStack, AWS Device Farm or other external device-farm provider. No
credentials, provider secrets, reservation steps, or manual test checklists
are created for those services. Every required test stays on GitHub-hosted
Ubuntu runners or Android emulators running inside those same runners.


## Verified zero-error Android Lint and GitHub merge protection (2026-11-21)

[Android Community Beta #125](https://github.com/wxyu71/Lumi_Books/actions/runs/44905832516)
completed successfully on commit `90f14c54fc74abff522b8f143a097417e4b84f93`.
The quality job completed `:app:lintDebug`, the independent strict XML
verifier, `:app:assembleDiagnostic` and `:app:assembleRelease` successfully.
The verifier's actual log is **0 Error/Fatal, 978 warnings, 31 other findings**.
Both API29/API35 emulator jobs, policy, APK/JVM verification and the final
`Required CI summary` passed. This validates *error-level* clean Lint; it does
not claim that all warnings and hints have been eliminated.

The initial Lint XML had 556 errors across 559 locations. No rule was globally
disabled, no global baseline or legacy fingerprint allowlist remains, and no
file-wide suppressions were added. On Android 29+, built-in Source Serif 4
font metadata is read through `Font.Builder(Resources, @FontRes).buffer`;
on Android 26–28, a single narrowly scoped `@SuppressLint("ResourceType")`
is needed because `Resources.openRawResource` accepts a packaged font resource
at runtime but is annotated `@RawRes`. The new instrumented test checks the
packaged variable-font axis, guarding the actual intended behavior.

### Repository administrator action required: protect `main`

The public GitHub branch API returned `main.protected = false` and its
`required_status_checks.enforcement_level = off`; the rulesets API returned
an empty list. **A green `ci-gate` job is not, by itself, a required merge
check**. The connected GitHub App lacks repository administration API access
(branch-protection endpoint returned HTTP 403), so it cannot enforce this.

An authorized repository administrator must open **Settings → Rules → Rulesets**
(or **Settings → Branches → Branch protection rules**) and protect `main`:

1. Require a pull request before merging and **require status checks to pass**.
2. Select the GitHub Actions checks **Required CI summary** (job `ci-gate`)
   and **Android Lint + release/diagnostic variants** (job `quality`) as
   required checks. The former already requires policy, verify, quality and
   both emulator legs; requiring the latter directly makes Lint conspicuous.
3. Consider requiring branches to be up to date before merging, and avoid
   enabling bypass for normal merges. Save/activate the rule for `main`.
4. Verify the GitHub API reports `protected = true` and that these two
   statuses are listed among required checks, then validate that an intentionally
   failing check blocks merge. Do **not** merge this Draft PR merely because
   the workflow itself is green.

The workflow uses `pull_request` and `push` triggers for `main`, and the
strict zero-error check remains in `.github/workflows/android-beta.yml`.
