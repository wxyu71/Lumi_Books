#!/usr/bin/env python3
"""Fail CI when an upstream merge accidentally reintroduces excluded community features."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]

def file(path):
    return (ROOT / path).read_text(encoding="utf-8")

def ensure(ok, message):
    if not ok:
        raise SystemExit("COMMUNITY POLICY FAILED: " + message)

manifest = file("app/src/main/AndroidManifest.xml")
welcome = file("app/src/main/java/com/huangder/lumibooks/ui/welcome/WelcomeScreen.kt")
about = file("app/src/main/java/com/huangder/lumibooks/ui/settings/SettingsDetailScreens.kt")
settings = file("app/src/main/java/com/huangder/lumibooks/ui/settings/SettingsScreen.kt")
edition = file("app/src/oss/java/com/huangder/lumibooks/edition/EditionUi.kt")
main = file("app/src/main/java/com/huangder/lumibooks/MainActivity.kt")
config = json.loads(file("docs/app-config.json"))
gradle = file("app/build.gradle.kts")
community_changelog = file("app/src/main/assets/changelog.md")
tag_repository = file("app/src/main/java/com/huangder/lumibooks/data/repository/TagManagementRepository.kt")
update_checker = file("app/src/main/java/com/huangder/lumibooks/util/UpdateChecker.kt")
update_sheet = file("app/src/main/java/com/huangder/lumibooks/ui/components/UpdateDownloadSheet.kt")
journal_ui = file("app/src/oss/java/com/huangder/lumibooks/edition/CommunityReadingJournal.kt")
tags_ui = file("app/src/oss/java/com/huangder/lumibooks/edition/CommunityTagManagement.kt")
export_ui = file("app/src/oss/java/com/huangder/lumibooks/edition/CommunityAnnotationFeedExport.kt")

ensure("val premiumEdition = false" in gradle, "Fork must always use the public OSS source set")
ensure("io.github.wxyu71.lumibooks" in gradle, "Fork package ID was overwritten")
ensure(not (ROOT / "app/src/premium/java").exists(), "Private Premium source is forbidden")
ensure("EditionTagManagementSettingsEntry() = Unit" not in edition, "Community tag management must be implemented")
ensure("github.com/wxyu71/Lumi_Books/discussions" in about, "Community discussions link missing")
ensure("github.com/wxyu71/Lumi_Books" in about, "Community repository link missing")
ensure("qm.qq.com" not in about, "Official QQ group reintroduced in About")
ensure("CategoryItem(AppIcons.Tile.Feedback" not in settings, "Feedback category reintroduced")
ensure("CategoryItem(AppIcons.Tile.Sponsor" not in settings, "Official Sponsor entry reintroduced")
ensure("SponsorActivity" not in manifest and "SponsorActivity" not in about,
       "Upstream sponsor/donation page restored")
ensure(not (ROOT / "app/src/main/java/com/huangder/lumibooks/ui/settings/SponsorActivity.kt").exists(),
       "Upstream sponsor page must not ship")
ensure(not (ROOT / "app/src/main/java/com/huangder/lumibooks/data/github/GitHubContributorsClient.kt").exists(),
       "Fork has a single maintainer; do not import upstream contributor feed")
for unused in ("ContinueReadingWidgetProvider", "QuoteWidgetProvider", 'android:name=".ui.settings.FeedbackActivity"'):
    ensure(unused not in manifest, "Disabled component still registered: " + unused)

onboarding = welcome.split("internal fun welcomePages(", 1)[1].split("private data class WelcomeLanguageOption", 1)[0]
ensure("add(WelcomePage.FEATURES)" not in onboarding and "add(WelcomePage.SUPPORT)" not in onboarding,
       "Mandatory promotional welcome page reintroduced")
ensure("onContinue = onFinished" in welcome, "Agreement must immediately enter reader")
startup = main.split("private fun performStartupUpdateCheck()", 1)[1]
ensure("UpdateChecker.fetchUpdateConfig()" not in startup, "Startup network update request reintroduced")
ensure('name="community_app_name"' in file("app/src/main/res/values/community_strings.xml"), "Community launcher label missing")
ensure("premium_pricing" not in config, "Official Premium pricing reintroduced in community config")
ensure("PremiumAccess" not in tag_repository and "premium_required" not in tag_repository,
       "Tag management must not use membership entitlements")
ensure("journal.enable" in journal_ui and "community_journal_consent" in journal_ui,
       "Opt-in reading journal consent flow missing")
ensure("deleteGroup" in tags_ui and "moveTags" in tags_ui and "deleteTags" in tags_ui,
       "Community group and bulk tag operations missing")
ensure("notesExportBooks" in export_ui and "CreateDocument" in export_ui,
       "Filtered local annotation export path missing")
ensure("Premium" not in community_changelog and "会员" not in community_changelog,
       "Official subscription marketing reintroduced into the visible changelog")
ensure(config["release_url"].startswith("https://github.com/wxyu71/Lumi_Books/"),
       "Update config points to non-community releases")
ensure("https://api.github.com/repos/wxyu71/Lumi_Books/releases" in update_checker,
       "Manual update checker does not read this fork's GitHub Releases")
ensure("https://raw.githubusercontent.com/wxyu71/Lumi_Books/main/docs/app-config.json" in update_checker,
       "Policy config moved to an untrusted source")
for prohibited in ("pan.baidu.com", "lanzoub.com", "gitee.com/huangder", "github.com/huangder"):
    ensure(prohibited not in update_sheet, "Upstream download endpoint reintroduced: " + prohibited)
ensure(config.get("latest_version_code") == 0 and config["update"]["latest_version_code"] == 0,
       "Static config must not claim a new unpublished APK")
for readme in ("README.md", "README.en.md", "README.ja.md", "README.ko.md"):
    content = file(readme)
    ensure("https://github.com/huangder/Lumi_Books/releases" not in content,
           "An upstream download link reappeared in " + readme)
    ensure("pan.baidu.com" not in content and "lanzoub.com" not in content,
           "An upstream distribution mirror reappeared in " + readme)
# The obsolete parallel developer READMEs were deliberately deleted during
# the single-maintainer docs restructure. Check absence rather than opening them.
for obsolete in ("README.dev.md", "README.dev.en.md", "README.dev.ja.md", "README.dev.ko.md"):
    ensure(not (ROOT / obsolete).exists(),
           "Obsolete developer README was reintroduced: " + obsolete)
for maintained in ("docs/README.md", "docs/development/build.md", "docs/development/release.md"):
    ensure((ROOT / maintained).is_file(), "Maintained development guide is missing: " + maintained)
    ensure("Lumi_Books-premium.git" not in file(maintained),
           "Private edition checkout instructions reappeared in " + maintained)
ensure(not (ROOT / "docs/CNAME").exists(), "Upstream custom domain must not be deployed from this fork")
print("Community policy guardrails passed.")
