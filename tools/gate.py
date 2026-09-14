"""The gate: every check this mod has, in one command, one line per check, non-zero if any failed.

Checks are grouped by WHAT THEY NEED TO RUN, not by what they are about (wiki/mod-testing.md):

    tier 0   the tree      nothing but Gradle's asset walker. Seconds.
    tier 1   the JVM       gradle: compiles the mod and runs the JUnit tests (src/test). ~30s.
    tier 2   the server    a dedicated server with the mcp-toolkit bridge, started and stopped
                             here on its own port and directory. ~1 min boot, then seconds.
    tier 3   the client    a client with the bridge, for the tooltip and the studio frames.
                             Attaches to the dev client on this project's port when one is up
                             (it usually is), else starts one. The only tier whose subject is a
                             picture: tools/gate/frames.py, goldens under tools/gate/goldens/.

    python tools/gate.py                    # every tier this machine can run
    python tools/gate.py --tier 1           # tiers 0 and 1 only
    python tools/gate.py --only saddle      # one check, by name
    python tools/gate.py --list             # what would run, without running it
    python tools/gate.py --json report.json # the same result as a file
    python tools/gate.py --bless            # tier 3: keep this run's frames as the goldens (a person looked)

Run the tiers in order and stop at the first failure that matters; run them sequentially, never
in parallel (two games cannot share a port). The mechanics of each live tier are in its own file.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLEW = str(ROOT / ("gradlew.bat" if sys.platform == "win32" else "gradlew"))
PY = sys.executable


@dataclass
class Check:
    name: str
    tier: int
    what: str
    command: list[str]
    needs: str = ""  # "" | "python-pil"


@dataclass
class Result:
    check: Check
    status: str  # pass | FAIL | skip
    seconds: float
    tail: list[str] = field(default_factory=list)


def checks(bless: bool) -> list[Check]:
    tier3 = [PY, "tools/gate/tier3.py", "--json", "build/gate/tier3.json"]
    if bless:
        tier3.append("--bless")
    return [
        Check("assets", 0, "no dangling asset reference in src/main/resources (checkAssets)",
              [GRADLEW, "-q", "checkAssets"]),
        Check("junit", 1, "it compiles and src/test passes: registries, shipped files, growth ladder",
              [GRADLEW, "-q", "test"]),
        Check("server", 2, "scenarios on a dedicated server: stages, barding, saddle, Snuffy, the log",
              [PY, "tools/gate/tier2.py", "--json", "build/gate/tier2.json"]),
        Check("client", 3, "the barding's tooltip and studio frames of Nijntje and Snuffy against their goldens",
              tier3, needs="python-pil"),
    ]


def available(needs: str) -> bool:
    if needs == "python-pil":
        try:
            import PIL  # noqa: F401
            return True
        except ImportError:
            return False
    return True


def run(check: Check, tail: int) -> Result:
    started = time.monotonic()
    if not available(check.needs):
        return Result(check, "skip", 0.0, [f"needs {check.needs} (pip install pillow)"])
    completed = subprocess.run(check.command, cwd=ROOT, capture_output=True, text=True,
                               encoding="utf-8", errors="replace")
    lines = (completed.stdout + completed.stderr).strip().splitlines()
    return Result(check, "pass" if completed.returncode == 0 else "FAIL",
                  time.monotonic() - started, lines[-tail:])


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0],
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--tier", type=int, default=None, help="run tiers up to this one (default: all)")
    parser.add_argument("--only", action="append", default=[], metavar="NAME", help="run checks whose name contains this")
    parser.add_argument("--list", action="store_true", help="print what would run and stop")
    parser.add_argument("--json", metavar="PATH", help="write the result as JSON")
    parser.add_argument("--bless", action="store_true", help="tier 3: this run's frames become the goldens")
    parser.add_argument("--tail", type=int, default=25, metavar="N", help="lines of a failed check's output to show")
    args = parser.parse_args(argv)

    selected = checks(args.bless)
    if args.tier is not None:
        selected = [c for c in selected if c.tier <= args.tier]
    if args.only:
        selected = [c for c in selected if any(o in c.name for o in args.only)]

    if args.list:
        for check in selected:
            state = "" if available(check.needs) else f"   (skip: needs {check.needs})"
            print(f"tier {check.tier}  {check.name:<10} {check.what}{state}")
        return 0

    results: list[Result] = []
    for check in selected:
        print(f"tier {check.tier}  {check.name:<10} ...", end="", flush=True)
        result = run(check, args.tail)
        results.append(result)
        print(f"\rtier {check.tier}  {check.name:<10} {result.status:<5} {result.seconds:6.1f}s  {check.what}")
        if result.status != "pass":
            for line in result.tail:
                print(f"           {line}")

    failed = [r for r in results if r.status == "FAIL"]
    skipped = [r for r in results if r.status == "skip"]
    print(f"\n{len(results) - len(failed) - len(skipped)} passed, {len(failed)} failed, {len(skipped)} skipped")
    if args.json:
        out = Path(args.json)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps([{
            "name": r.check.name, "tier": r.check.tier, "what": r.check.what,
            "status": r.status, "seconds": round(r.seconds, 1), "tail": r.tail,
        } for r in results], indent=2), encoding="utf-8")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
