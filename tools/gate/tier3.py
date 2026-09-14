"""Run the gate's tier 3: the client's own answers - a tooltip, and pictures of the bunny.

    python tools/gate/tier3.py                    # attach to the dev client on mcmod.port; start one if none
    python tools/gate/tier3.py --only dress       # scenes whose name contains this
    python tools/gate/tier3.py --bless            # keep this run's frames as the goldens (a person looked)
    python tools/gate/tier3.py --list
    python tools/gate/tier3.py --json build/gate/tier3.json

The only tier whose subject is a picture. `studio` stands the bunny in a dimension with no sky and
flat light, `freeze` stops the tick, and `render` shoots out of band at a fixed size - so the same
subject renders bit-identically, and a frame can be held against the golden a person blessed
(tools/gate/frames.py). A frame with no golden is `new`: it fails the run and prints its path.

Unlike tier 2 this ATTACHES by default: a client takes minutes to boot and a person modding this
tree usually has one open on the project's port (`launch_game`). With none there, it starts
`gradlew runClient`, creates a flat world, runs, and quits. Either way the studio is left as it
was found (`studio {leave:true}`).
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
    from gate.bridge import ROOT, Bridge, BridgeError, project_port
    from gate.frames import Goldens
    from gate.scenarios import BARDING, BUNNY, EGG, SNUFFY, Failed, expect
else:
    from .bridge import ROOT, Bridge, BridgeError, project_port
    from .frames import Goldens
    from .scenarios import BARDING, BUNNY, EGG, SNUFFY, Failed, expect

GRADLEW = str(ROOT / ("gradlew.bat" if sys.platform == "win32" else "gradlew"))
OUT = ROOT / "build" / "gate" / "tier3"

#: How every golden frame is taken. The studio stands its subject facing the corner it puts the
#: client in (-x -z), so the camera belongs at yaw -45; a little pitch shows the ears and the back.
FRAME = {"yaw": -45, "pitch": 12, "width": 640, "height": 640, "downscale": 2}


@dataclass
class Client:
    bridge: Bridge
    goldens: Goldens
    staged: bool = False
    problems: list[str] = field(default_factory=list)

    def tooltip(self, item: str) -> list[str]:
        return [str(line) for line in self.bridge.call("get_tooltip", item=item)["lines"]]

    def stand(self, entity: str, *, nbt: str | None = None, equipment: dict | None = None) -> dict:
        """Stand a subject on the studio floor, frozen, and return the box it stands in."""
        args: dict = {"entity": entity, "freeze": True}
        if nbt:
            args["nbt"] = nbt
        if equipment:
            args["equipment"] = equipment
        box = self.bridge.call("studio", **args)
        self.staged = True
        expect("look_at" in box, f"studio returned no box for {entity}: {box}")
        return box["look_at"]

    def shot(self, name: str, box: dict) -> Path:
        """Photograph the staged subject and hold the frame against its golden."""
        # The subject's own column and nothing else: `render` stands back far enough to fit the
        # box it is given, and the studio's stage would put a bunny in the middle of a lot of white.
        centre = {"x": (box["min"]["x"] + box["max"]["x"]) // 2, "z": (box["min"]["z"] + box["max"]["z"]) // 2}
        column = {"min": {"x": centre["x"], "y": box["min"]["y"], "z": centre["z"]},
                  "max": {"x": centre["x"], "y": box["max"]["y"], "z": centre["z"]}}  # full height: a GROWN bunny's ears reach the top
        reply = self.bridge.call("render", look_at=column, out=f"gate/tier3/{name}.png", **FRAME)
        taken = Path(reply["path"])
        expect(taken.is_file(), f"render said it wrote {taken} and there is no such file")
        result = self.goldens.compare(name, taken)
        if result.status in ("FAIL", "new"):
            self.problems.append(result.note)
        return taken

    def leave(self) -> None:
        if self.staged:
            try:
                self.bridge.call("studio", leave=True)
            except BridgeError:
                pass
            self.staged = False


SCENES: list[tuple[str, str, Callable[[Client], None]]] = []


def scene(name: str, what: str):
    def register(fn):
        SCENES.append((name, what, fn))
        return fn
    return register


@scene("tooltip_barding", "the barding's tooltip names it and prices it: +6 armor, +2 toughness when equipped")
def tooltip_barding(client: Client) -> None:
    lines = client.tooltip(BARDING)
    text = "\n".join(lines)
    expect(lines and lines[0] == "Bunny Barding", f"the first tooltip line is {lines[:1]}, not 'Bunny Barding' (lang?)")
    expect("When equipped:" in text, f"no 'When equipped:' header - the modifiers are not on the BODY slot group:\n{text}")
    expect("+6 Armor" in text, f"no '+6 Armor' line:\n{text}")
    expect("+2 Armor Toughness" in text, f"no '+2 Armor Toughness' line:\n{text}")


@scene("tooltip_egg", "the spawn egg's tooltip names it")
def tooltip_egg(client: Client) -> None:
    lines = client.tooltip(EGG)
    expect(lines and lines[0] == "Nijntje Spawn Egg", f"the egg's tooltip starts {lines[:1]}")


@scene("frame_adult", "an adult Nijntje in the studio, against its golden")
def frame_adult(client: Client) -> None:
    client.shot("nijntje_adult", client.stand(BUNNY))


@scene("frame_baby", "a baby Nijntje: the smallest stage renders at its scale")
def frame_baby(client: Client) -> None:
    client.shot("nijntje_baby", client.stand(BUNNY, nbt="{GrowthStage:0}"))


@scene("frame_grown_saddled", "a grown Nijntje in barding with a saddle: the body layer and the saddle both draw")
def frame_grown_saddled(client: Client) -> None:
    client.shot("nijntje_grown_saddled",
                client.stand(BUNNY, nbt="{GrowthStage:3}", equipment={"body": BARDING, "saddle": "minecraft:saddle"}))


@scene("frame_blue_dress", "a dyed dress: the dress layer takes the colour")
def frame_blue_dress(client: Client) -> None:
    client.shot("nijntje_blue_dress", client.stand(BUNNY, nbt="{DressColor:11b}"))


@scene("frame_snuffy", "Snuffy in the studio, against its golden")
def frame_snuffy(client: Client) -> None:
    client.shot("snuffy", client.stand(SNUFFY))


# ---- the runner ----------------------------------------------------------------------------

def start_client(log: Path) -> subprocess.Popen:
    log.parent.mkdir(parents=True, exist_ok=True)
    handle = log.open("w", encoding="utf-8", errors="replace")
    return subprocess.Popen([GRADLEW, "runClient", "--console=plain"], cwd=ROOT, stdout=handle, stderr=subprocess.STDOUT)


def stop_client(client: subprocess.Popen, bridge: Bridge) -> None:
    try:
        bridge.call("quit_game")
    except (BridgeError, OSError):
        pass
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        if client.poll() is not None and not bridge.alive():
            return
        time.sleep(2)
    if sys.platform == "win32":
        subprocess.run(["taskkill", "/F", "/T", "/PID", str(client.pid)], capture_output=True, check=False)
    else:
        client.kill()


def in_a_world(bridge: Bridge) -> bool:
    """Whether the CLIENT is standing in a loaded level with its player in it.

    `serverRunning` is not that: the integrated server comes up first, and a `studio` or a
    `get_tooltip` in that window is refused with "no level loaded". What answers only once the
    client is really in a level is get_world_info's world_uuid, and once the player exists,
    get_entities' source.
    """
    try:
        if not bridge.call("ping").get("serverRunning"):
            return False
        if not bridge.call("get_world_info").get("world_uuid"):
            return False
        return "player" in (bridge.call("get_entities", radius=1, load=False).get("source") or "")
    except (BridgeError, OSError):
        return False


def ensure_world(bridge: Bridge, seconds: int) -> None:
    """A client at the title screen gets a fresh flat world; one already in a world keeps it."""
    if not bridge.call("ping").get("serverRunning"):
        bridge.wait(seconds, tool="create_world")
        bridge.call("create_world", name="gate", generator="flat", gamemode="creative",
                    difficulty="peaceful", replace=True)
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if in_a_world(bridge):
            time.sleep(2)  # and let the first chunks arrive before the studio moves the player
            return
        time.sleep(2)
    raise SystemExit(f"the client did not finish loading a world within {seconds}s")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0],
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=None, help="bridge port (default mcmod.port)")
    parser.add_argument("--only", action="append", default=[], metavar="NAME")
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--json", metavar="PATH")
    parser.add_argument("--bless", action="store_true", help="this run's frames become the goldens")
    parser.add_argument("--boot", type=int, default=300, metavar="SECONDS")
    args = parser.parse_args(argv)

    if args.list:
        for name, what, _ in SCENES:
            print(f"  {name:<24} {what}")
        return 0

    bridge = Bridge.on(args.port or project_port())
    started: subprocess.Popen | None = None
    log = ROOT / "build" / "gate" / "tier3-client.log"
    if not bridge.alive():
        print(f"tier 3: no client on {bridge.url}; starting one (log: {log.relative_to(ROOT)})")
        started = start_client(log)
        try:
            bridge.wait(args.boot, tool="create_world")
        except BridgeError as never:
            print(f"tier 3: {never}")
            stop_client(started, bridge)
            return 2
    else:
        bridge.wait(60, tool="studio")  # a client's own tools register after the first pong

    results = []
    instance = ""
    try:
        ensure_world(bridge, args.boot)
        instance = bridge.check_instance(need_client=True)
        print(f"tier 3: {instance}")
        client = Client(bridge, Goldens(OUT, bless=args.bless))
        for name, what, fn in SCENES:
            if args.only and not any(o in name for o in args.only):
                continue
            client.problems.clear()
            began = time.monotonic()
            try:
                fn(client)
                status, note = ("FAIL", "\n        ".join(client.problems)) if client.problems else ("pass", "")
            except Failed as failure:
                status, note = "FAIL", str(failure)
            except BridgeError as refused:
                status, note = "FAIL", f"the bridge refused: {refused}"
            finally:
                client.leave()
            results.append({"name": name, "what": what, "status": status, "note": note,
                            "seconds": round(time.monotonic() - began, 1)})
            print(f"  {status:<5} {name:<24} {what}" + (f"\n        {note}" if note else ""))
        blessed = [r for r in client.goldens.results if r.status == "blessed"]
        for r in blessed:
            print(f"  blessed {r.name}: {r.note}")
    finally:
        if started is not None:
            stop_client(started, bridge)

    failed = [r for r in results if r["status"] != "pass"]
    print(f"tier 3: {len(results) - len(failed)}/{len(results)} passed")
    if args.json:
        out = Path(args.json)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps({"tier": 3, "instance": instance, "results": results}, indent=2), encoding="utf-8")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
