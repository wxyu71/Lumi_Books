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

## Scoped dependency retries and secure caching

The tools/ci_gradle_retry.sh wrapper allows at most three total attempts and
only when tools/ci_dependency_failure.py positively detects plugin/dependency
resolution or transient network errors. Kotlin compiler errors, test failures,
timeouts, OOM and cancellations are terminal; no tests are ignored or retried
to obtain an accidental pass. Each attempt is logged separately and reported.
The Gradle commands have a 21-minute per-attempt timeout and bounded jobs.

The shared composite action pins SDK, NDK, CMake and the Gradle setup Action.
Only trusted main writes the Gradle basic cache. PRs read only; no third-party
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
