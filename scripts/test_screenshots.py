import json
from pathlib import Path
import struct
import tempfile
import unittest

from collect_screenshots import collect, SCENES


class ScreenshotCollectionTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / "screenshots"
        self.source.mkdir()
        self.output = self.root / "published"
        self.png = b"\x89PNG\r\n\x1a\n" + struct.pack(">I", 13) + b"IHDR" + struct.pack(">II", 1440, 900) + b"\x08\x06\0\0\0"
        self.manifest = self.source / "completed.json"

    def complete_run(self):
        for name in SCENES:
            (self.source / f"{name}.png").write_bytes(self.png)
        self.manifest.write_text(json.dumps({"completed": True, "scenes": list(SCENES)}))

    def test_complete_run_copies_only_the_required_original_images(self):
        self.complete_run()
        (self.source / "unrelated.png").write_bytes(b"old screenshot")
        result = collect(self.source, self.output)
        self.assertEqual(set(result), set(SCENES))
        self.assertEqual({p.name for p in self.output.iterdir()}, {f"{n}.png" for n in SCENES})
        self.assertTrue(all(p.read_bytes() == self.png for p in self.output.iterdir()))

    def test_failed_missing_or_incomplete_run_never_publishes_images(self):
        for manifest in [None, {"completed": False, "scenes": list(SCENES)},
                         {"completed": True, "scenes": list(SCENES)[:-1]}]:
            with self.subTest(manifest=manifest):
                self.complete_run()
                if manifest is None:
                    self.manifest.unlink()
                else:
                    self.manifest.write_text(json.dumps(manifest))
                with self.assertRaises((ValueError, FileNotFoundError)):
                    collect(self.source, self.output)
                self.assertFalse(self.output.exists())

    def test_missing_corrupt_or_wrong_resolution_file_is_rejected_before_copying(self):
        for failure in ["missing", "corrupt", "resolution"]:
            with self.subTest(failure=failure):
                self.complete_run()
                image = self.source / f"{SCENES[-1]}.png"
                if failure == "missing":
                    image.unlink()
                elif failure == "corrupt":
                    image.write_bytes(b"not an image")
                else:
                    image.write_bytes(self.png[:16] + struct.pack(">II", 640, 480) + self.png[24:])
                with self.assertRaises((ValueError, FileNotFoundError)):
                    collect(self.source, self.output)
                self.assertFalse(self.output.exists())

    def test_unexpected_or_escaping_scene_name_is_rejected(self):
        self.complete_run()
        self.manifest.write_text(json.dumps({"completed": True, "scenes": ["../private", *SCENES]}))
        with self.assertRaises(ValueError):
            collect(self.source, self.output)
        self.assertFalse(self.output.exists())


if __name__ == "__main__":
    unittest.main()
