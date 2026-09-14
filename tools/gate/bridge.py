"""Speak to the mcp-toolkit bridge in a running game.

The bridge is a plain HTTP endpoint (`POST /cmd`, `GET /tools`) the toolkit opens inside a dev
game, and it is the whole of the gate's tier 2 and tier 3: everything the mod does that needs a
world - a bunny growing, a barding equipped, a picture of it - is asked through it. The MCP shim
beside it is for an agent session; a suite talks HTTP.

Three rules, each a scar somewhere in the workbench:

  * **Prove the build before believing a reply.** A second game cannot bind the port and the bridge
    answers from the OLDER JVM without saying so, so a run against a stale instance passes tests for
    code that is not loaded. `ping.build` names what the game is running; `check_instance` refuses
    one without this mod or one the toolkit calls stale.
  * **Send from Python, never from a shell.** A Windows path inside a `curl -d` JSON literal breaks
    the shell's escaping in a way that looks like a bridge error.
  * **`run_command` reports that a command PARSED.** `ok:true` is not "it did what you meant"; a
    scenario that depends on a command's effect reads the world back.
"""

from __future__ import annotations

import base64
import json
import re
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
MOD_ID = "nijntje"


def project_port() -> int:
    """`mcmod.port` from gradle.properties: the one number the game and every registration share."""
    text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    match = re.search(r"^\s*mcmod\.port\s*=\s*(\d+)", text, re.MULTILINE)
    if not match:
        raise SystemExit("gradle.properties has no mcmod.port; the gate cannot know which bridge is this mod's")
    return int(match.group(1))


class BridgeError(RuntimeError):
    """The bridge answered, and the answer was no."""


@dataclass
class Bridge:
    url: str
    timeout: int = 60

    @classmethod
    def on(cls, port: int) -> "Bridge":
        return cls(f"http://127.0.0.1:{port}")

    # ---- the wire ------------------------------------------------------------------------

    def call(self, tool: str, /, **args) -> dict:
        """One tool call. Returns its `result`; raises BridgeError if the tool refused."""
        request = urllib.request.Request(
            f"{self.url}/cmd",
            data=json.dumps({"tool": tool, "args": args}).encode("utf-8"),
            headers={"Content-Type": "application/json"},
        )
        with urllib.request.urlopen(request, timeout=self.timeout) as response:
            envelope = json.load(response)
        if not envelope.get("ok"):
            raise BridgeError(f"{tool}: {envelope.get('error') or envelope}")
        result = envelope.get("result")
        return result if isinstance(result, dict) else {"value": result}

    def tools(self) -> list[dict]:
        with urllib.request.urlopen(f"{self.url}/tools", timeout=self.timeout) as response:
            return json.load(response)

    def alive(self) -> bool:
        try:
            self.call("ping")
            return True
        except (urllib.error.URLError, OSError, BridgeError):
            return False

    def wait(self, seconds: int = 300, *, server: bool = False, tool: str | None = None) -> dict:
        """Block until the bridge answers a ping - and, if asked, until a world is loaded and a
        given tool is offered. Those are three different moments: the bridge opens while the game
        is still starting, the world comes later, and a CLIENT's own tools (all of tier 3) later
        still. A run that trusts the first pong reads its own impatience as a failure."""
        deadline = time.monotonic() + seconds
        last = None
        while time.monotonic() < deadline:
            try:
                last = self.call("ping")
                if (not server or last.get("serverRunning")) and (
                        tool is None or any(t.get("name") == tool for t in self.tools())):
                    return last
            except (urllib.error.URLError, OSError, BridgeError, ValueError):
                pass
            time.sleep(2)
        if last is None:
            raise BridgeError(f"no game answered on {self.url} within {seconds}s")
        raise BridgeError(f"the bridge on {self.url} answered but "
                          + ("no world had loaded" if server and not last.get("serverRunning")
                             else f"never offered `{tool}`") + f" within {seconds}s: {last}")

    # ---- the game ------------------------------------------------------------------------

    def command(self, command: str) -> list[str]:
        """A server command at full permission; the game's own chat feedback, line by line."""
        result = self.call("run_command", command=command)
        lines = result.get("output") or result.get("feedback") or []
        return [lines] if isinstance(lines, str) else [str(line) for line in lines]

    def say(self, command: str) -> str:
        return "\n".join(self.command(command))

    def log(self, **filters) -> list[dict]:
        return self.call("get_log", **filters).get("entries", [])

    def push_data(self, path: str, text: str, reload: bool = True) -> dict:
        return self.call("push_data", path=path,
                         base64=base64.b64encode(text.encode("utf-8")).decode("ascii"), reload=reload)

    def clear_data(self, path: str | None = None, reload: bool = True) -> dict:
        args: dict = {"reload": reload}
        if path:
            args["path"] = path
        return self.call("clear_data", **args)

    # ---- the instance --------------------------------------------------------------------

    def check_instance(self, *, need_client: bool = False) -> str:
        """Refuse a game that is not this mod, or that the toolkit says is running old code.

        Returns a one-line description of what answered, for the report."""
        ping = self.call("ping")
        build = ping.get("build") or {}
        mods = {m.get("id"): m for m in build.get("mods") or [] if isinstance(m, dict)}
        if MOD_ID not in mods:
            raise SystemExit(
                f"the game on {self.url} is not running {MOD_ID}: {sorted(mods) or 'no mod list at all'}. "
                "A second game cannot bind the port, so this is another instance answering - stop it.")
        if build.get("stale"):
            raise SystemExit(
                "the attached game is running code older than the tree (ping.build says stale). "
                "Rebuild and restart it; a green run against a stale instance is worse than no run.")
        if not ping.get("serverRunning"):
            raise SystemExit(f"the bridge on {self.url} answered but no world is loaded behind it")
        if need_client and not ping.get("clientPresent"):
            raise SystemExit(f"the game on {self.url} has no client; tier 3 needs one (a dedicated server cannot render)")
        return f"{MOD_ID} {mods[MOD_ID].get('version')} ({ping.get('env')}, port {ping.get('port')}, " \
               f"{'client' if ping.get('clientPresent') else 'server'})"
