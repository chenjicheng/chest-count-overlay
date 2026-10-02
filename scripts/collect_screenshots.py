"""Copy only original screenshots from a successfully completed client test."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import struct


SCENES = (
    "cco-chest-counts", "cco-nested-containers", "cco-shulker-box", "cco-collapsed",
    "cco-scrolling", "cco-settings-en", "cco-settings-zh",
)


def collect(source, output):
    manifest = json.loads((source / "completed.json").read_text(encoding="utf-8"))
    if manifest.get("completed") is not True or manifest.get("scenes") != list(SCENES):
        raise ValueError("Screenshots require a completed run with every expected scene")
    originals = {}
    for name in SCENES:
        path = source / f"{name}.png"
        data = path.read_bytes()
        if len(data) < 29 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
            raise ValueError(f"Invalid PNG header: {path.name}")
        if struct.unpack(">II", data[16:24]) != (1440, 900):
            raise ValueError(f"Unexpected screenshot resolution: {path.name}")
        originals[name] = hashlib.sha256(data).hexdigest()
    # Validate the entire set before changing any published image.
    output.mkdir(parents=True, exist_ok=True)
    for name in SCENES:
        shutil.copyfile(source / f"{name}.png", output / f"{name}.png")
    return originals


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=Path("build/run/clientGameTest/documentation-screenshots"))
    parser.add_argument("--output", type=Path, default=Path("docs/public/images"))
    args = parser.parse_args()
    print(json.dumps(collect(args.source, args.output), indent=2))
