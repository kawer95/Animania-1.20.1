"""Regression checks for render layers used by exact CraftStudio prop meshes."""
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class CraftStudioRenderLayerTest(unittest.TestCase):
    def test_solid_pet_facilities_restore_legacy_culling(self) -> None:
        renderers = [
            ROOT / "catsdogs/src/main/java/com/animania/catsdogs/client/render/CatsDogsPetFacilityRenderer.java",
        ]
        for renderer in renderers:
            source = renderer.read_text(encoding="utf-8")
            self.assertIn("RenderType.entityCutout(", source, renderer)
            self.assertNotIn("entityCutoutNoCull", source, renderer)

    def test_hives_use_baked_geometry_with_two_sided_bee_wings(self) -> None:
        renderer = ROOT / "farm/src/main/java/com/animania/farm/client/render/FarmHiveRenderer.java"
        source = renderer.read_text(encoding="utf-8")
        self.assertIn("context.bakeLayer(FarmNativeModelLayers.LAYERS.get", source, renderer)
        self.assertIn("RenderType.entityCutoutNoCull(", source, renderer)
        self.assertNotIn("FarmLegacyPropModels.create", source, renderer)

    def test_zero_thickness_hamster_wheel_remains_deliberately_two_sided(self) -> None:
        renderer = ROOT / "extra/src/main/java/com/animania/extra/client/render/ExtraHamsterWheelRenderer.java"
        self.assertIn("RenderType.entityCutoutNoCull(WHEEL_TEXTURE)", renderer.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
