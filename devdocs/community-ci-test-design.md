# Community CI Architecture and Test Execution

This document is the operational runbook for the community OSS fork. The existing
android-beta.yml used one job for a full APK followed by approximately **571 JVM
test files**. The previous workflow could spend over half an hour on one undiagnosed
test step. It also saved no cache from in-repo PRs while their matching branch Push
run was immediately cancelled.

## Required PR checks (all are mandatory)

| Job | Scope | Budget | Diagnostic |
| --- | --- | --- | --- |
| Community policy & test inventory | No SDK needed; guardrails, translation placeholders, exhaustive test package routing | 5 min | Standard output |
| OSS APK and checksum | JDK 17 / Android 37.0; universal APK in PR, all ABI APKs on main/manual | 50 min | Artifact and SHA-256 |
| JVM unit tests (reader) | `com.huangder.lumibooks.ui.reader.*` (~199 source files) | 26 min Gradle command / 38 min job | JUnit XML + console log |
| JVM unit tests (ui) | Other `com.huangder.lumibooks.ui.*` (~164 source files) | 26 min / 38 min | JUnit XML + console log |
| JVM unit tests (core) | Everything outside `ui` (~208 source files) | 26 min / 38 min | JUnit XML + console log |
| Required CI summary | Explicit result gate for all four stages | 5 min | GitHub step summary |

Counts are approximate and are verified on every commit by
`tools/ci_test_inventory.py`. The `testDebugUnitTest` task is the same one
used locally; `-Plumi.testShard` only filters packages without disabling any
class for the combined suite. The XML verifier confirms non-empty reports and
prevents accidental test leakage between shards.

The 26-minute step limit returns exit 124 on timeout, leaving a log artifact with
the latest Gradle `STARTED` test. The 38-minute job limit covers SDK installation
and failure-report uploads as well. Do **not** turn all unit-test failures into warnings
or blanket-disable slow tests. If a group times out, rerun that one shard locally with
the saved log, then inspect its last started test, external I/O, deadlocks, and
Robolectric runtime resolution. `forkEvery = 100`, `maxParallelForks = 1`
and a 1536 MB test heap limit process leaks and avoid parallel fork memory pressure;
the matrix handles parallelism across three isolated runners (at most two at once).

## Cache design

The official Gradle action validates the wrapper. APK builds on trusted same-repo
PRs are allowed to save a scoped Gradle cache (fork-originated PRs stay read-only).
The three test jobs read the cache but never compete to update it. Main branch
builds also save Gradle state. This addresses the former default-branch-only cache
rule, where a cancelled branch Push left the PR without warm dependencies.

Never let a job executed from an external fork access signing keys, repository write
permissions, a production release token, or untrusted workflow dispatch inputs.

## Performance, device tests and follow-up automation

* Start by measuring the duration of each shard and its JUnit XML. Preserve the
  full-suite behavior locally with `./gradlew :app:testDebugUnitTest`.
* New features require focused JVM tests around repository transactions,
  consent/clear races, serialization, Markdown escaping, and hidden books.
* Compose interactions require separate instrumented tests on an emulator/device,
  across Lumi, Material 3, Liquid Glass, and e-ink/reduced-motion fallbacks.
  These must be added to a **scheduled/manual device-test workflow**, not run
  inside the fast JVM stage. Screenshots, TalkBack and gesture tests must use
  actual UI state and stable semantics, not arbitrary sleeps.
* Cold start, opening a large EPUB, switching themes, bulk tag editing,
  and writing 1000+ journal events should be measured separately using
  Android Macrobenchmark (the existing `benchmark/` module) and a baseline.
  Do not classify an emulator-in-CI measurement as a real-device performance
  regression until hardware has been controlled.
* Each blocking job must leave machine-readable JUnit output and concise
  human-readable failures. Never replace the JUnit gate with a “best effort” echo.
* For upstream sync PRs, the same mandatory gates must pass after conflicts
  and community policy checks are resolved.

References:
- https://docs.gradle.org/current/userguide/java_testing.html
- https://docs.gradle.org/current/userguide/performance.html
- https://docs.gradle.org/current/userguide/github-actions.html
- https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax
- https://robolectric.org/configuring/

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
