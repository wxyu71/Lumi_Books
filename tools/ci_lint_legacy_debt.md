# Android Lint legacy-error debt (frozen 2026-11-21)

Reference: [CI #89](https://github.com/wxyu71/Lumi_Books/actions/runs/44879743310), `:app:lintDebug` on AGP 8.13.2 and JDK 21.

This is **not a clean lint run**. The first full Lint pass discovered **556 existing errors, 989 warnings and 31 hints** (1,576 findings); 556 errors occupy 559 XML location entries. Its HTML/XML report is retained as a CI artifact. The project must triage and reduce these findings, especially API-compatibility, App Links, JavaScript bridge and string-format concerns, before calling the codebase fully lint-clean.

| Lint error ID | Existing errors |
| --- | ---: |
| MissingTranslation | 425 |
| LocalContextGetResourceValueCall | 64 |
| NewApi | 26 |
| WrongConstant | 11 |
| NonObservableLocale | 7 |
| StateFlowValueCalledInComposition | 7 |
| RestrictedApi | 6 |
| StringFormatInvalid | 3 |
| RememberReturnType | 2 |
| AppLinkUrlError | 1 |
| ByteOrderMark | 1 |
| JavascriptInterface | 1 |
| ResourceType | 1 |
| UnusedBoxWithConstraintsScope | 1 |
| **Total** | **556** |

## Gate behavior

The required workflow **runs all of `:app:lintDebug`** without restricting issue IDs or setting `abortOnError=false`. It captures Gradle's expected exit for known legacy errors and then invokes `tools/ci_verify_lint_debt.py` on the full XML. Every Error/Fatal location must match the fixed, sorted and SHA-256-checksummed record set in `tools/ci_lint_legacy_fingerprints.bin`.

Each record identifies the issue ID, exact message, app-relative file, line, column and lint-highlighted source line with a 64-bit truncated SHA-256 fingerprint. There are 559 frozen location records; a new/changed location or issue, extra occurrence, absent report or corrupt snapshot **fails Required Gate**. Previously fixed errors may disappear (and should then be removed in a reviewed baseline-reduction PR). Code changes moving lines can intentionally cause the gate to fail, prompting triage rather than silently accepting old errors in new places. Snapshot SHA256: `5ac74c513873117dbd3af1551e93146d4a3a528d88da7a7b61717ecf6a47b542`.

The CI additionally fails for any non-Lint Gradle error, including compiler, plugin resolution and timeouts. Full warnings/hints remain visible in the uploaded HTML/XML. This is an **exact debt budget**, not a suppression of newly introduced lint errors; unlike a pure count threshold it cannot trade one fixed error for a different new one of the same issue ID.

The baseline is an intentional exception to the preferred official Android Lint `lint-baseline.xml` mechanism because the CI is required to retain the complete human-readable 1,576-finding XML/HTML report and the initial debt already spans hundreds of items. Android's official baseline approach was reviewed at https://developer.android.com/studio/write/lint. Once existing debt is reduced, replace the snapshot with an official XML baseline or a direct zero-errors check.

## Risk and release

Do not declare the 556 tracked errors fixed, and do not add new hashes automatically in CI. A conscious change to the baseline requires reviewing exact issue locations, updating the checksum and regression tests, and a successful fresh Actions run. All build, Lint, JVM, emulator UI/screenshot, artifact and workflow-security checks execute solely inside GitHub Actions. No physical-device or external device-farm tasks, secrets, provider integrations, reserved approvals or offline test gates are configured.
