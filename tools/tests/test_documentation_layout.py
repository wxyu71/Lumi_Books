"""Documents are navigable and obsolete parallel authorities stay removed."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class DocumentationLayoutTest(unittest.TestCase):
    def test_single_entry_and_structured_docs(self):
        for name in ("docs/README.md", "docs/development/build.md",
                     "docs/development/ci.md", "docs/development/release.md",
                     "docs/reference/ui-motion.md", "docs/reference/community-ui.md",
                     "AGENTS.md", "COMMUNITY.md"):
            self.assertTrue((ROOT / name).is_file(), name)
        self.assertFalse((ROOT / "devdocs").exists())
        for name in ("README.dev.md", "README.dev.en.md",
                     "README.dev.ja.md", "README.dev.ko.md",
                     "docs/index.html", "docs/sponsor.html", "docs/premium.html",
                     "docs/remote-notice-update-handoff.md"):
            self.assertFalse((ROOT / name).exists(), name)

    def test_user_docs_have_no_old_downloads(self):
        for name in ("README.md", "README.en.md", "README.ja.md", "README.ko.md"):
            readme = (ROOT / name).read_text(encoding="utf-8")
            self.assertIn("github.com/wxyu71/Lumi_Books/releases", readme)
            self.assertNotIn("github.com/huangder/Lumi_Books/releases", readme)
            self.assertNotIn("README.dev.", readme)

if __name__ == "__main__":
    unittest.main()
