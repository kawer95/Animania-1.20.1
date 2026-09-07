import unittest
from pathlib import Path
from unittest.mock import patch

from tools.audit_resources import _is_external_model_parent, _resolve_legacy_root


class AuditResourcesTest(unittest.TestCase):
    def test_prefers_pinned_legacy_checkout(self) -> None:
        root = Path("workspace/Animania-1.20.1").resolve()
        pinned = root / "upstream" / "Animania-1.12"
        with patch.object(Path, "is_dir", autospec=True,
                          side_effect=lambda candidate: candidate == pinned):
            self.assertEqual(pinned.resolve(), _resolve_legacy_root(root))

    def test_falls_back_to_sibling_legacy_checkout(self) -> None:
        root = Path("workspace/Animania-1.20.1").resolve()
        sibling = root.parent / "_legacy_animania"
        with patch.object(Path, "is_dir", autospec=True,
                          side_effect=lambda candidate: candidate == sibling):
            self.assertEqual(sibling.resolve(), _resolve_legacy_root(root))

    def test_accepts_only_known_external_model_parents(self) -> None:
        self.assertTrue(_is_external_model_parent("minecraft:item/generated"))
        self.assertTrue(_is_external_model_parent("forge:item/bucket"))
        self.assertFalse(_is_external_model_parent("forge:item/missing"))
        self.assertFalse(_is_external_model_parent("animania_farm:item/missing"))


if __name__ == "__main__":
    unittest.main()
