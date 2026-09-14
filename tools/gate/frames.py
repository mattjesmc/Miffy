"""Goldens: a frame a person has LOOKED AT, and the diff when a new one stops matching it.

Tier 3 is the only tier whose subject is a picture, and a picture cannot be asserted the way a
number can - but it can be asserted not to have CHANGED. Every frame the suite takes is compared
with its copy under `tools/gate/goldens/`; a frame with no golden beside it is neither a pass nor
a failure, it is a frame nobody has looked at yet, and it fails the run saying so. `--bless` is
the moment a person looks, and the only way a golden gets written.

The studio renders the same subject bit-identically (no sky, flat light, a frozen tick, out-of-band
`render`), so the tolerance is not for renderer noise: it absorbs a driver or a game update moving
a texel, and it is a COUNT of pixels, low enough that a dress that stopped drawing cannot hide in
it. A failure writes the golden, this run, and a mask of where they differ side by side, because
the number that failed says nothing about whether the render layer died or a texture moved.
"""

from __future__ import annotations

import shutil
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parent.parent.parent
GOLDENS = ROOT / "tools" / "gate" / "goldens"

#: A channel may move by this much without the pixel counting as different.
TOLERANCE = 4
#: How many pixels may differ before the frame has changed. The dress alone is thousands.
ALLOWANCE = 24


@dataclass
class Comparison:
    name: str
    status: str  # pass | FAIL | new | blessed
    note: str = ""
    pixels: int = 0


@dataclass
class Goldens:
    out: Path
    bless: bool = False
    results: list[Comparison] = field(default_factory=list)

    def compare(self, name: str, taken: Path) -> Comparison:
        result = self._compare(name, taken)
        self.results.append(result)
        return result

    def _compare(self, name: str, taken: Path) -> Comparison:
        golden = GOLDENS / f"{name}.png"
        if not golden.is_file():
            if self.bless:
                golden.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(taken, golden)
                return Comparison(name, "blessed", f"written from {taken}")
            return Comparison(name, "new", (
                f"no golden for {name}. This frame has never been looked at: open {taken}, and if "
                f"it is right, run this scene again with --bless to keep it as the answer."))

        with Image.open(golden) as before, Image.open(taken) as now:
            first, second = before.convert("RGB"), now.convert("RGB")
            if first.size != second.size:
                return Comparison(name, "FAIL", (
                    f"the frame is {second.size[0]}x{second.size[1]} and its golden is "
                    f"{first.size[0]}x{first.size[1]} - a render argument changed, not the mod"))
            differing, mask = difference(first, second)

        if differing <= ALLOWANCE:
            return Comparison(name, "pass", "", differing)
        if self.bless:
            shutil.copyfile(taken, golden)
            return Comparison(name, "blessed", f"replaced, {differing} pixels changed", differing)
        panels = self._panels(name, golden, taken, mask)
        return Comparison(name, "FAIL", (
            f"{differing} pixels differ from the golden (more than {ALLOWANCE} may). What changed is "
            f"drawn in {panels}: golden, this run, and the mask between them. If this run is the "
            f"better picture, --bless replaces the golden."), differing)

    def _panels(self, name: str, golden: Path, taken: Path, mask: Image.Image) -> Path:
        with Image.open(golden) as before, Image.open(taken) as now:
            width, height = before.size
            sheet = Image.new("RGB", (width * 3 + 8, height), (24, 24, 28))
            sheet.paste(before.convert("RGB"), (0, 0))
            sheet.paste(now.convert("RGB"), (width + 4, 0))
            sheet.paste(mask, (width * 2 + 8, 0))
        path = self.out / f"{name}.diff.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(path)
        return path


def difference(first: Image.Image, second: Image.Image) -> tuple[int, Image.Image]:
    """How many pixels moved by more than the tolerance in ANY channel, and a red mask of where."""
    channels = ImageChops.difference(first, second).split()
    worst = channels[0]
    for channel in channels[1:]:
        worst = ImageChops.lighter(worst, channel)
    over = worst.point(lambda value: 255 if value > TOLERANCE else 0, mode="1")
    differing = sum(over.histogram()[1:])
    mask = Image.new("RGB", first.size, (255, 255, 255))
    mask.paste(Image.new("RGB", first.size, (220, 40, 40)), (0, 0), over)
    return differing, mask
