"""Run the gate's tier 2: every scenario, against a dedicated server this starts and stops.

    python tools/gate/tier2.py                 # start a server on the gate's port, run everything, stop it
    python tools/gate/tier2.py --attach        # use the game already on this project's port (mcmod.port)
    python tools/gate/tier2.py --only saddle   # scenarios whose name contains this
    python tools/gate/tier2.py --list
    python tools/gate/tier2.py --json build/gate/tier2.json

A server takes about a minute to come up and the suite a few seconds after that, so the cost of the
tier is the boot. It boots on ITS OWN port (`mcmod.port` + 10, via `-Pport`) in its own directory
(`run-server`, build.gradle), so it never collides with the dev client a person has open on the
project's port. `--attach` is for writing scenarios against that client; the gate itself always
starts its own, because the point of a gate is that it needs nothing to be true of the machine.

**The stale-instance guard is not optional.** A second game cannot bind a port and the bridge
answers from the OLDER JVM without saying so, which means a green run for code that is not loaded.
Every run reads `ping.build` and refuses an instance without this mod, or one the toolkit calls stale.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
from pathlib import Path

if __package__ in (None, ""):  # run as a file rather than as a module
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
    from gate.bridge import ROOT, Bridge, BridgeError, project_port
    from gate.scenarios import SCENARIOS, Failed, Game
else:
    from .bridge import ROOT, Bridge, BridgeError, project_port
    from .scenarios import SCENARIOS, Failed, Game

GRADLEW = str(ROOT / ("gradlew.bat" if sys.platform == "win32" else "gradlew"))
SERVER_DIR = ROOT / "run-server"


def gate_port() -> int:
    return project_port() + 10


def prepare_server_dir() -> None:
    """What a dedicated server needs before it will boot unattended, written ONCE and never rewritten.

    The EULA, and a properties file that makes a gate server cheap: offline (no account), a flat
    world (nothing to generate), a small view distance, and no watchdog (a debugger or a slow disk
    must not kill the run). Yours to edit afterwards; the gate only fills an absence.
    """
    SERVER_DIR.mkdir(exist_ok=True)
    eula = SERVER_DIR / "eula.txt"
    if not eula.exists():
        eula.write_text("eula=true\n", encoding="utf-8")
    properties = SERVER_DIR / "server.properties"
    if not properties.exists():
        properties.write_text(
            "online-mode=false\nlevel-type=minecraft\\:flat\nlevel-name=gate\n"
            "spawn-protection=0\nview-distance=6\nmax-tick-time=-1\ndifficulty=peaceful\n"
            "spawn-monsters=false\nenable-command-block=false\n",
            encoding="utf-8")


def start_server(port: int, log: Path) -> subprocess.Popen:
    """`gradlew runServer` on the gate's port, its output on disk rather than in this terminal."""
    prepare_server_dir()
    log.parent.mkdir(parents=True, exist_ok=True)
    handle = log.open("w", encoding="utf-8", errors="replace")
    return subprocess.Popen(
        [GRADLEW, "runServer", f"-Pport={port}", "--console=plain"],
        cwd=ROOT, stdout=handle, stderr=subprocess.STDOUT,
    )


def stop_server(server: subprocess.Popen, bridge: Bridge) -> None:
    """Shut the server down, and make sure it is really gone.

    `/stop` is the polite way and the only one that saves the world. Killing the process this
    started is NOT enough on its own: what was started is the Gradle wrapper and the game is its
    grandchild - terminate the wrapper and the game carries on holding the port, where the next
    run finds it and mistakes it for its own.
    """
    polite = False
    try:
        polite = bool(bridge.call("ping").get("serverRunning"))
        if polite:
            bridge.command("stop")
    except (BridgeError, OSError):
        pass
    deadline = time.monotonic() + (180 if polite else 5)
    while time.monotonic() < deadline:
        if server.poll() is not None and not bridge.alive():
            return
        time.sleep(2)
    if sys.platform == "win32":
        subprocess.run(["taskkill", "/F", "/T", "/PID", str(server.pid)], capture_output=True, check=False)
    else:
        server.kill()
    try:
        server.wait(timeout=60)
    except subprocess.TimeoutExpired:
        pass


def run_scenarios(game: Game, only: list[str]) -> list[dict]:
    results = []
    for name, what, fn in SCENARIOS:
        if only and not any(o in name for o in only):
            continue
        started = time.monotonic()
        try:
            fn(game)
            status, note = "pass", ""
        except Failed as failure:
            status, note = "FAIL", str(failure)
        except BridgeError as refused:
            status, note = "FAIL", f"the bridge refused: {refused}"
        except Exception as error:  # noqa: BLE001 - a scenario's own bug is still a red line, not a crash
            status, note = "FAIL", f"{type(error).__name__}: {error}"
        finally:
            game.sweep()
        seconds = time.monotonic() - started
        results.append({"name": name, "what": what, "status": status, "note": note, "seconds": round(seconds, 1)})
        print(f"  {status:<5} {name:<28} {what}" + (f"\n        {note}" if note else ""))
    return results


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0],
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--attach", action="store_true",
                        help="use the game already running on this project's port instead of starting a server")
    parser.add_argument("--port", type=int, default=None, help="bridge port to use (default: gate port, or mcmod.port with --attach)")
    parser.add_argument("--only", action="append", default=[], metavar="NAME", help="run scenarios whose name contains this")
    parser.add_argument("--list", action="store_true", help="print what would run and stop")
    parser.add_argument("--json", metavar="PATH", help="write the result as JSON")
    parser.add_argument("--boot", type=int, default=300, metavar="SECONDS", help="how long to wait for the server (default 300)")
    args = parser.parse_args(argv)

    if args.list:
        for name, what, _ in SCENARIOS:
            print(f"  {name:<28} {what}")
        return 0

    port = args.port or (project_port() if args.attach else gate_port())
    bridge = Bridge.on(port)
    server: subprocess.Popen | None = None
    log = ROOT / "build" / "gate" / "tier2-server.log"

    if args.attach:
        if not bridge.alive():
            print(f"tier 2: --attach, but nothing answers on {bridge.url}. Start the dev game (launch_game / gradlew runClient) first.")
            return 2
    else:
        if bridge.alive():
            print(f"tier 2: something already holds {bridge.url}. A previous gate server that did not stop? "
                  f"Stop it (or run with --attach --port {port} to use it).")
            return 2
        print(f"tier 2: starting a dedicated server on {bridge.url} (log: {log.relative_to(ROOT)})")
        server = start_server(port, log)
        try:
            bridge.wait(args.boot, server=True)
        except BridgeError as never:
            print(f"tier 2: {never}")
            stop_server(server, bridge)
            tail = log.read_text(encoding="utf-8", errors="replace").splitlines()[-30:]
            print("  the server's last lines:\n    " + "\n    ".join(tail))
            return 2

    try:
        instance = bridge.check_instance()
        print(f"tier 2: {instance}")
        game = Game(bridge)
        game.enter()
        try:
            results = run_scenarios(game, args.only)
        finally:
            game.leave()
    finally:
        if server is not None:
            stop_server(server, bridge)

    failed = [r for r in results if r["status"] != "pass"]
    print(f"tier 2: {len(results) - len(failed)}/{len(results)} passed")
    if args.json:
        out = Path(args.json)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps({"tier": 2, "instance": instance, "results": results}, indent=2), encoding="utf-8")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
