# Community CI Architecture and Test Execution

2026-11-21: this runbook supersedes the former three-runner JVM matrix.
Recent runs #74, #77 and #78 revealed duplicate test compilation errors on
three separate runners, and intermittent Maven/plugin marker resolution errors
for Hilt 2.58. The artifact exists on Maven Central, so version changes are
not the first response to a single transient resolver failure.

## Required PR checks

| Job | Scope | Budget | Diagnostics |
| --- | --- | --- | --- |
| policy | Fork restrictions, translation checks, shard source inventory and Python tests | 5 min | Console |
| apk | JDK 17, OSS debug APK, metadata and checksum | 50 min job | APK artifact or failure log |
| unit-tests | JDK 21; reader, ui and core sequential on ONE runner | 80 min job | XML, HTML and log per shard |
| ci-gate | policy, APK and all three JVM shards required | 5 min | GitHub Step Summary |

## Why logical test sharding now uses one runner

Running three parallel JVM jobs meant each installed the SDK, downloaded Gradle
plugins and compiled the same approximately 571 JVM test source files before
its package filter could take effect. A single Kotlin test compiler error
therefore failed all three independent jobs without running any assertions.

The tools/ci_run_jvm_shards.sh script invokes :app:testDebugUnitTest in one
workspace with -Plumi.testShard=reader, ui and core in succession.
The first pass compiles shared sources and subsequent passes reuse the outputs.
Each test task has a fresh XML directory and an isolated worker invocation.
The existing tools/ci_verify_junit.py checks every shard for nonempty reports,
out-of-shard cases and failures; tools/ci_junit_summary.py publishes failures
and slow cases with their logs. A shared compiler failure with no XML aborts
immediately rather than being reproduced on two more expensive runners.
Real assertion failures do not stop the remaining shards and fail the final
job after all available evidence is gathered.

## Run #81 follow-up: plugin marker & independent statistics loading

GitHub-hosted APK attempts resolved the same missing Hilt marker three times, so
retrying the message itself is not evidence of a transient outage. The settings
pluginManagement block now maps the pinned Hilt plugin ID directly to the
published `com.google.dagger:hilt-android-gradle-plugin` implementation using
Gradle's `resolutionStrategy.eachPlugin.useModule`; version remains 2.58.
Only actual network timeouts, resets, 429 or 5xx now qualify for retry.

The last UI test failure was switching to a MONTH period in landscape. The
statistics model used `combine(insights, comparison)` and could withhold valid
primary insights while the optional comparison query had not emitted. It now
seeds comparison with null, displays primary data immediately, and updates
comparison when available. A dedicated JVM regression test covers a comparison
Flow that completes without any emission. The landscape UI test now reports
its observed selection, data and error on timeout instead of only saying
`ComposeTimeoutException`.

## CI #83: cold-Runner plugin resolution and cache handoff

CI #83 proves that the OSS APK could successfully resolve/build Hilt 2.58,
while the independent JDK 21 JVM Runner failed before any tests on the
published `com.google.dagger:hilt-android-gradle-plugin:2.58` implementation.
The failed job's Gradle summary showed `Entry not restored: no match found`
and `Cache was read-only`. This is not evidence that version 2.58 is invalid;
the transient failure is inconsistent across separate GitHub-hosted Runners.

The APK job now writes the basic Gradle User Home cache on main and for
**same-repository PRs only**. Fork PRs keep read-only access. The JVM job
depends on completion of the APK job and restores the warmed Gradle cache
read-only, while still collecting separate failure diagnostics if APK fails.
The required gate continues to require both the APK and all JVM shards.
Maven Central precedes Google for plugin resolution, matching the actual
Hilt distribution host, without adding custom mirrors or changing versions.

GitHub caches for PRs are scoped to `refs/pull/<number>/merge`; PR cache
entries cannot become main-branch caches. The official Gradle setup Action
documents cache save on job completion, cache restore at the start of the
downstream job, and this exact producer/read-only-consumer pattern:
https://github.com/gradle/actions/blob/main/docs/setup-gradle.md

Do not add a second Gradle Home cache using `actions/cache` or setup-java.
Review each run's cache restore/save keys and the Hilt resolution log before
changing any dependency version or suppressing a test.
## CI #84 cold-cache resolution follow-up

The first trusted-PR APK job can itself start with an empty cache. Run #84
failed to resolve the already published Hilt 2.58 implementation during
that initial APK build; the Gradle setup action still saved its partial
cache at job completion. Cache handoff alone cannot fix a cold-cache
repository lookup failure.

Gradle's dependency resolution metadata cache stores missing artifacts,
including repository misses. A retry that repeats the same command can
return the cached miss in seconds; CI #81 showed this exact fast-repeat
pattern. Gradle documents `--refresh-dependencies` to re-query metadata
and validate artifacts against configured repositories. The retry wrapper
now adds `--refresh-dependencies --info` **only after** the classifier
positively identifies a transient network error or the pinned, known
published Hilt 2.58 marker/implementation miss. A maximum of three
attempts with bounded backoff remains; Kotlin compiler failures, failed
JUnit assertions, timeouts, OOM and arbitrary missing plugins remain fatal.

Evidence: https://docs.gradle.org/current/userguide/dependency_caching.html
and https://central.sonatype.com/artifact/com.google.dagger/hilt-android-gradle-plugin .
Inspect attempt-specific logs and cache save/restore summaries; a green
build is never inferred from a successful retry alone.
## CI #84: landscape statistics UI regression

JUnit confirmed 1 failed UI test in 791 UI cases; reader and core shards
passed. The explicit failure state after tapping Month was:
`scope=MONTH, loaded=false, comparison=false, error=null`.
This confirms the tab state changed, but the asynchronous insights
recomposition/collection had not completed at the assertion.

The Compose test now calls `assertIsSelected()` and `waitForIdle()`
before polling the independent `StateFlow` for data, matching its existing
passing theme/portrait test. Historical period navigation also synchronizes
the Compose clock before awaiting data and now retains detailed state
diagnostics if loading fails. This preserves every original interaction,
data-loaded assertion and screenshot capture; no timeout increase or skip.

Official reference: https://developer.android.com/develop/ui/compose/testing/synchronization
## CI #85: verified repository 404 despite forced Gradle refresh

The APK job in [run #85](https://github.com/wxyu71/Lumi_Books/actions/runs/44872310637)
ran all three bounded attempts, including `--refresh-dependencies --info`.
Verbose Gradle output records `Resource missing. [HTTP GET:
https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android-gradle-plugin/2.58/hilt-android-gradle-plugin-2.58.pom]`.
This proves that a cache miss/negative metadata alone is not the full issue:
the Maven Central canonical endpoint returned an absent POM even when forced
to recheck. It does not establish that the artifact was never published;
Dagger 2.58 is an official release and Maven Central indexes the artifact.

Official Dagger release notes state that Dagger 2.59's **Hilt Gradle plugin
requires AGP 9**. Lumi remains pinned to AGP 8.13.2 / Kotlin 2.3.10 /
Hilt 2.58 to avoid that unsupported cross-major change.
Sources: https://github.com/google/dagger/releases/tag/dagger-2.59 ,
https://github.com/google/dagger/releases/tag/dagger-2.58 .

Before changing versions, try the second **official HTTPS Maven Central**
endpoint `https://repo1.maven.org/maven2`, which Sonatype documents as
supported alongside `repo.maven.apache.org`:
https://central.sonatype.org/faq/ssl/ .
Both plugin and project dependency resolution use this endpoint with a
`content.includeGroup` filter restricted to Dagger; Google's Android Maven
and Gradle Plugin Portal remain intact, without arbitrary third-party
mirrors or blanket dependency substitution.

This is a controlled alternate-endpoint experiment, not a claimed fix.
Check the next run's `--info` resolution URL and JUnit XML before treating
it as successful. Keep the required gate strict and the PR Draft.
## Scoped dependency retries and secure caching

The tools/ci_gradle_retry.sh wrapper allows at most three total attempts and
only when tools/ci_dependency_failure.py positively detects the pinned,
published Hilt 2.58 plugin resolution miss or transient network errors.
Retries bypass stale missing-module metadata via --refresh-dependencies. Kotlin compiler errors, test failures,
timeouts, OOM and cancellations are terminal; no tests are ignored or retried
to obtain an accidental pass. Each attempt is logged separately and reported.
The Gradle commands have a 21-minute per-attempt timeout and bounded jobs.

The shared composite action pins SDK, NDK, CMake and the Gradle setup Action.
Trusted main and same-repository PR builds may write the Gradle basic cache;
external-fork PRs and JVM consumers are read-only; no third-party
build scans, signing secrets, unsafe pull_request_target or configuration cache.
Run the emulator and Macrobenchmark workflows separately, weekly or manually.

## Community project comparison

- AnkiDroid: https://github.com/ankidroid/Anki-Android/blob/main/.github/workflows/tests_unit.yml
  Uses a bounded unit-test matrix, dependency preparation retry, and controlled
  Gradle basic caching. Its matrix covers OS/iterations, unlike Lumi's former
  repeated full compilation on identical Ubuntu workers.
- AntennaPod: https://github.com/AntennaPod/AntennaPod/blob/develop/.github/workflows/checks.yml
  Separate static analysis, variant build/test, emulator matrix and final gate.
- NewPipe: https://github.com/TeamNewPipe/NewPipe/blob/dev/.github/workflows/ci.yml
  Combines APK assembly and JVM tests per build job; instrumentation separately.
- Now in Android: https://github.com/android/nowinandroid/blob/main/.github/workflows/Build.yaml
  Adds local-unit, screenshot baseline, APK, Android Lint and emulator evidence.

Keep APK on JDK 17 and Robolectric tests on JDK 21, preserve upstream community
visual tokens, and require device-level checks for actual Liquid Glass optics.
No green Required CI summary or device smoke evidence means no release merge.
## Robolectric / JDK compatibility

The previous full-suite log (run 44744611437, attempt 2) records a
`NativeTtsMediaRoutingTest` failure at `DefaultSdkProvider.java:170`.
Robolectric 4.16.1 supports Android SDK 23–36; Android SDK 36 requires Java 21,
and API 37 is not yet a supported runtime in 4.16.x. The app still compiles on
Java 17, with compileSdk/targetSdk 37, but the **JVM unit-test jobs use Java 21**.
An inherited `robolectric.properties` sets the default runtime to Android API 35.
Explicit `@Config(sdk = [36])` tests still execute under Java 21; **no test
SDKs are silently excluded**.

The community onboarding tests must check immediate consent without promotional
stories, not assert the old official's forced welcome tour. We updated the render
assertions accordingly.

Sources: https://robolectric.org/compatibility_table/ ,
https://robolectric.org/configuring/

## Weekly/manual Android device checks

The existing `benchmark/` module exercises startup, opening TXT/EPUB and turning
pages on actual Android instrumentation, but its target package originally pointed to
the upstream app. The community fork fixes this to
`io.github.wxyu71.lumibooks` without changing the manifest's stable custom
benchmark broadcast action names.

`.github/workflows/android-device-smoke.yml` runs **weekly or manually**, using
a pinned Android emulator runner at API 35 with hardware acceleration when available.
It executes a bounded instrumented test selection that checks Room DAO behavior and
the project-specific Liquid Glass button interaction, uploading device test reports
even after failure. A manual input explicitly opts in to running the existing
`ReaderMacrobenchmark` suite. This stage does **not** gate every PR, to avoid
repeated AVD startup and unstable emulator performance. Promote a device test to a
required merge gate only after tracking its failure rate and environment sensitivity.

Emulator Macrobenchmark metrics are comparable only within controlled emulator
hardware/build configurations. Do not publish speedup percentages from different
runner hardware or infer real-device battery and thermals from these values. Test
on a representative physical device before release.

## Open-source reference and ongoing maintenance

Reference workflows examined on 2026-11-20:

| Project | Applicable pattern | Deliberate adaptation |
| --- | --- | --- |
| [AnkiDroid](https://github.com/ankidroid/Anki-Android/blob/main/.github/workflows/tests_unit.yml) | JUnit failure summaries, scheduled JVM tests, Gradle v6 basic cache | Preserve full test coverage in three isolated shards; do not delete failing tests |
| [AnkiDroid emulator](https://github.com/ankidroid/Anki-Android/blob/main/.github/workflows/tests_emulator.yml) | API/machine matrix, retained logs, manual diagnostics | Weekly device smoke, manual performance suite; avoid expensive emulators on every PR |
| [AntennaPod](https://github.com/AntennaPod/AntennaPod/blob/develop/.github/workflows/checks.yml) | Separate static-analysis, unit and emulator checks | Stage quality baseline separately before making it a required gate |
| [NewPipe](https://github.com/TeamNewPipe/NewPipe/blob/dev/.github/workflows/ci.yml) | Actionable reports and SDK-specific UI tests | Use a failing final gate but still upload artifacts and diagnostics |
| [Now in Android](https://github.com/android/nowinandroid/blob/main/.github/workflows/Build.yaml) | Android Lint, screenshot regressions, APK validation | Run Android Lint weekly and move only stable checks to PR required gates |
| [Android platform samples](https://github.com/android/platform-samples/blob/main/.github/workflows/build.yml) | Wrapper integrity, Android Lint, device reports | Use pinned actions and no publishing secrets in PR jobs |

The new local composite action centralizes JDK 17/21 setup, Gradle wrapper
validation, cache policy and Android SDK/NDK/CMake versions, so future
dependency bumps cannot silently diverge between the APK, JVM and device jobs.
`tools/ci_junit_summary.py` prints failing cases and top slow tests, or
compiler errors when there is no JUnit XML. It never turns a failing
Gradle invocation green.

`android-quality.yml` is a weekly/manual lint baseline, not yet a required
PR gate until initial warnings/errors are triaged. `dependabot.yml` proposes
reviewed SHA updates for GitHub Actions; never auto-merge, and avoid
uncontrolled Java/Gradle/Android plugin upgrade waves.

Always validate release signing and provenance independently before publishing
a stable APK; CI debug artifacts are **not** stable-upgrade-signed releases.
