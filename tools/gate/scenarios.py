"""Tier 2: what a Nijntje does in a RUNNING game, asked through the bridge.

Each scenario is a few bridge calls and what their answers have to say. They run against a
dedicated server (no client, no pictures) that `tier2.py` starts and stops - or, with `--attach`,
against whatever game is up, which is how a scenario is written.

The subject is driven through NBT rather than through a player: `/summon` with tags, `/data merge`
to set a stage or an owner (which calls the entity's own `load`, so what is being tested is the
save/load path the mod ships, not a test-only setter), and the entity's own tick to move it on.
There is no headless right-click on an entity, so FEEDING is not here - it is the one act only a
person or a `review_post` can settle, and `docs` in the gate's report says so.

Rules, each a scar:

  * **Say what is wrong, not that something is.** A failure here is read by somebody who cannot see
    the game; the game's own line goes into the message.
  * **Leave the world as you found it.** Every scenario kills what it summoned in a `finally`.
  * **`run_command` reports that a command PARSED.** Every act is followed by a read.
"""

from __future__ import annotations

import re
import time
from dataclasses import dataclass, field
from typing import Callable

from .bridge import Bridge, BridgeError

MOD = "nijntje"
BUNNY = f"{MOD}:nijntje"
SNUFFY = f"{MOD}:snuffy"
BARDING = f"{MOD}:bunny_barding"
EGG = f"{MOD}:nijntje_spawn_egg"

#: Every entity a scenario summons carries this tag, so cleanup can find them all.
TAG = "nijntje_gate"

#: A fake owner. TamableAnimal reads `Owner` as a UUID and sets tame from its presence alone; the
#: player need not exist for the bunny to believe it is owned.
OWNER = "[I;7,7,7,7]"

# Numbers the mod ships (NijntjeEntity). Pinned here so a retune fails a scenario BY NAME rather
# than silently moving a threshold; change both when you change the mod.
CARE_TO_ADVANCE = {0: 120, 1: 200, 2: 280}
HEALTH_BONUS = {0: 0.0, 1: 4.0, 2: 10.0, 3: 20.0}
ARMOR_BONUS = {0: 0.0, 1: 0.0, 2: 1.0, 3: 3.0}
SCALE = {0: 0.55, 1: 0.85, 2: 1.2, 3: 1.6}
BASE_HEALTH = 10.0
BARDING_ARMOR = 6.0
PASSIVE_CARE_PER_DAY = 5


class Failed(AssertionError):
    """A scenario's verdict: the message is the whole report."""


def expect(condition: bool, message: str) -> None:
    if not condition:
        raise Failed(message)


@dataclass
class Game:
    """One attached game and the handful of reads and acts the scenarios share."""

    bridge: Bridge
    summoned: list[str] = field(default_factory=list)
    claimed: bool = False

    # ---- the place -------------------------------------------------------------------------

    def enter(self) -> None:
        """Make the command source's chunk entity-ticking for the run.

        An entity lives in a chunk SECTION, and a selector (`@e`) sees a section only when its
        chunk is entity-ticking. The console source stands at world spawn, and on a dedicated
        server with no players nothing there ticks: a summon reports success and `@e` then finds
        nobody, which reads exactly like the mod losing its entity. `forceload` gives the one
        chunk a ticking ticket; `leave` takes it back.
        """
        self.bridge.command("forceload add ~ ~")
        self.claimed = True
        time.sleep(1.0)  # the ticket takes a tick or two to promote the chunk

    def leave(self) -> None:
        if self.claimed:
            try:
                self.bridge.command("forceload remove ~ ~")
            except BridgeError:
                pass
            self.claimed = False

    # ---- acts ------------------------------------------------------------------------------

    def summon(self, entity: str = BUNNY, nbt: str = "", *, plain: bool = False) -> str:
        """Summon at the command source. Returns the selector that finds this one subject; every
        subject is also tagged for the sweep.

        `plain` summons with NO NBT at all and tags the entity afterwards. The difference matters:
        `/summon` runs `finalizeSpawn` (the mod's "full health at the stage's max") only for a
        summon without NBT - with NBT the game treats the compound as the whole truth. So a subject
        that is meant to be what a spawn egg or a natural spawn produces is summoned plain, and one
        summoned with NBT starts at BASE health, whatever its stage.
        """
        name = f"{TAG}_{len(self.summoned)}_{int(time.time() * 1000) % 100000}"
        if plain:
            lines = self.bridge.command(f"summon {entity} ~ ~1 ~")
            expect(any("Summoned" in line for line in lines), f"summon {entity} did not report success: {lines}")
            time.sleep(0.2)
            tagged = self.bridge.say(f"tag @e[type={entity},tag=!{TAG},limit=1,sort=nearest] add {name}")
            expect("Added tag" in tagged, f"could not tag the plain summon: {tagged!r}")
            self.bridge.command(f"tag @e[tag={name}] add {TAG}")
            self.summoned.append(name)
            return f"@e[tag={name},limit=1]"
        nbt = nbt.strip()
        if nbt.startswith("{") and nbt.endswith("}"):
            nbt = nbt[1:-1]  # the caller's compound is merged into the one written here
        extra = f",{nbt}" if nbt else ""
        # Sitting: a tame bunny stays put, so `~ ~ ~` reads and distance selectors keep meaning
        # the same place. Every subject is also PersistenceRequired so a server without players
        # cannot despawn it between an act and its read.
        lines = self.bridge.command(
            f'summon {entity} ~ ~1 ~ {{Tags:["{TAG}","{name}"],PersistenceRequired:1b{extra}}}')
        expect(any("Summoned" in line for line in lines), f"summon {entity} did not report success: {lines}")
        self.summoned.append(name)
        return f"@e[tag={name},limit=1]"

    def merge(self, selector: str, nbt: str) -> None:
        """`/data merge entity`: calls the entity's own `load`, so the mod's read path runs."""
        lines = self.bridge.command(f"data merge entity {selector} {nbt}")
        expect(any("Modified" in line for line in lines), f"data merge {nbt} on {selector} was not applied: {lines}")

    def settle(self, ticks: int = 60) -> None:
        """Let the entity's own tick run: customServerAiStep acts every 40 ticks."""
        time.sleep(ticks / 20.0 + 0.3)

    def sweep(self) -> None:
        try:
            self.bridge.command(f"kill @e[tag={TAG}]")
            # Whatever a subject dropped (a killed bunny's saddlebag) or spawned (a Snuffy has no
            # gate tag: it was the MOD that summoned it) - anything of ours within reach.
            self.bridge.command(f"kill @e[type={SNUFFY},distance=..24]")
            self.bridge.command("kill @e[type=minecraft:item,distance=..24]")
        except BridgeError:
            pass
        self.summoned.clear()

    # ---- reads -----------------------------------------------------------------------------

    def data(self, selector: str, path: str) -> str:
        """`/data get entity <sel> <path>` - the value's SNBT as the game printed it."""
        text = self.bridge.say(f"data get entity {selector} {path}")
        match = re.search(r"has the following entity data: (.*)$", text, re.DOTALL)
        expect(match is not None, f"data get {selector} {path} answered: {text!r}")
        return match.group(1).strip()

    def number(self, selector: str, path: str) -> float:
        raw = self.data(selector, path)
        match = re.match(r"^-?[0-9.]+", raw)
        expect(match is not None, f"{path} of {selector} is not a number: {raw!r}")
        return float(match.group(0))

    def attribute(self, selector: str, attribute: str) -> float:
        text = self.bridge.say(f"attribute {selector} {attribute} get")
        match = re.search(r"is (-?[0-9.]+)\s*$", text)
        expect(match is not None, f"attribute {attribute} get on {selector} answered: {text!r}")
        return float(match.group(1))

    def count(self, selector: str) -> int:
        text = self.bridge.say(f"execute if entity {selector}")
        match = re.search(r"Count: (\d+)", text)
        return int(match.group(1)) if match else 0

    def registry(self, registry: str, **args) -> dict:
        return self.bridge.call("query_registry", registry=registry, **args)


# ---------------------------------------------------------------------------------------------
# The scenarios
# ---------------------------------------------------------------------------------------------

SCENARIOS: list[tuple[str, str, Callable[[Game], None]]] = []


def scenario(name: str, what: str):
    def register(fn: Callable[[Game], None]):
        SCENARIOS.append((name, what, fn))
        return fn
    return register


@scenario("registered", "the mod's ids are in the live registries, and its recipe LOADED")
def registered(game: Game) -> None:
    entities = game.registry("entity_type", namespace=MOD)["ids"]
    expect(BUNNY in entities and SNUFFY in entities, f"entity types under {MOD}: {entities}")
    items = game.registry("item", namespace=MOD)["ids"]
    expect(BARDING in items and EGG in items, f"items under {MOD}: {items}")
    sounds = game.registry("sound_event", namespace=MOD)["ids"]
    expect(f"{MOD}:nijntje_say" in sounds, f"sound events under {MOD}: {sounds}")
    # The pseudo-registry of what the server LOADED: a recipe file the loader stepped over with a
    # log line is absent here while present on disk.
    recipes = game.registry("recipe", namespace=MOD)["ids"]
    expect(BARDING in recipes, f"the server loaded these {MOD} recipes: {recipes} - not bunny_barding")
    saddle = game.registry("entity_type", tag="minecraft:can_equip_saddle")
    expect(BUNNY in saddle.get("ids", []),
           f"#minecraft:can_equip_saddle does not list {BUNNY} in the running game: {saddle}")


@scenario("summon_defaults", "a summoned Nijntje is an adult in an orange dress at full health")
def summon_defaults(game: Game) -> None:
    bunny = game.summon(plain=True)  # what an egg or a natural spawn produces: finalizeSpawn runs
    try:
        expect(game.data(bunny, "GrowthStage") == "2", f"GrowthStage of a fresh summon is {game.data(bunny, 'GrowthStage')}, not 2 (ADULT)")
        expect(game.data(bunny, "DressColor") == "1b", f"DressColor of a fresh summon is {game.data(bunny, 'DressColor')}, not 1b (orange)")
        want = BASE_HEALTH + HEALTH_BONUS[2]
        expect(game.attribute(bunny, "minecraft:max_health") == want,
               f"an ADULT's max health is {game.attribute(bunny, 'minecraft:max_health')}, not {want}")
        expect(game.number(bunny, "Health") == want, f"a fresh summon is not at full health: {game.data(bunny, 'Health')}")
    finally:
        game.sweep()


@scenario("stage_attributes", "each saved GrowthStage loads back as its own size and health")
def stage_attributes(game: Game) -> None:
    bunny = game.summon()
    try:
        for stage in (0, 1, 2, 3):
            game.merge(bunny, f"{{GrowthStage:{stage}}}")
            health = game.attribute(bunny, "minecraft:max_health")
            scale = game.attribute(bunny, "minecraft:scale")
            armor = game.attribute(bunny, "minecraft:armor")
            expect(health == BASE_HEALTH + HEALTH_BONUS[stage], f"stage {stage}: max health {health}, expected {BASE_HEALTH + HEALTH_BONUS[stage]}")
            expect(abs(scale - SCALE[stage]) < 1e-3, f"stage {stage}: scale {scale}, expected {SCALE[stage]}")
            expect(armor == ARMOR_BONUS[stage], f"stage {stage}: armor {armor}, expected {ARMOR_BONUS[stage]}")
        # An id past the ladder (an older or damaged save) clamps rather than crashing the load,
        # and what is written back is the clamped stage.
        game.merge(bunny, "{GrowthStage:99}")
        expect(game.data(bunny, "GrowthStage") == "3", f"GrowthStage:99 loaded back as {game.data(bunny, 'GrowthStage')}, not 3")
    finally:
        game.sweep()


@scenario("dress_persists", "a dyed dress survives the save/load round trip")
def dress_persists(game: Game) -> None:
    bunny = game.summon()
    try:
        game.merge(bunny, "{DressColor:11b}")  # blue
        expect(game.data(bunny, "DressColor") == "11b", f"DressColor read back as {game.data(bunny, 'DressColor')}, not 11b")
    finally:
        game.sweep()


@scenario("barding", "bunny barding goes into the BODY slot and is worth its armor")
def barding(game: Game) -> None:
    bunny = game.summon(nbt="{GrowthStage:0}")  # a baby: no stage armor, so the number is the barding's own
    try:
        lines = game.bridge.command(f"item replace entity {bunny} armor.body with {BARDING}")
        expect(not any("No targets accepted" in line for line in lines),
               f"the game refused {BARDING} in a Nijntje's body slot: {lines}")
        worn = game.data(bunny, "equipment.body.id")
        expect(worn == f'"{BARDING}"', f"the body slot holds {worn}, not {BARDING}")
        game.settle(ticks=10)  # equipment attribute modifiers are applied on the entity's next tick
        armor = game.attribute(bunny, "minecraft:armor")
        expect(armor == BARDING_ARMOR, f"a baby in barding has armor {armor}; the barding alone is worth {BARDING_ARMOR}")
    finally:
        game.sweep()


@scenario("saddle_when_grown", "a saddle is refused on a young Nijntje and accepted on a grown one")
def saddle_when_grown(game: Game) -> None:
    bunny = game.summon(nbt="{GrowthStage:2}")
    try:
        # `/item replace` asks the entity's own getEquipmentSlotForItem, which is canUseSlot plus
        # the saddle's #can_equip_saddle - so this reads both the tag and the mod's riding gate.
        lines = game.bridge.command(f"item replace entity {bunny} saddle with minecraft:saddle")
        expect(any("No targets accepted" in line for line in lines),
               f"an ADULT (not yet GROWN) Nijntje accepted a saddle; it must not be rideable yet: {lines}")
        game.merge(bunny, "{GrowthStage:3}")
        lines = game.bridge.command(f"item replace entity {bunny} saddle with minecraft:saddle")
        expect(not any("No targets accepted" in line for line in lines),
               f"a GROWN Nijntje refused a saddle: {lines}")
        expect(game.data(bunny, "equipment.saddle.id") == '"minecraft:saddle"',
               f"after the replace the saddle slot holds {game.data(bunny, 'equipment.saddle.id')}")
    finally:
        game.sweep()


@scenario("grows_and_gains_snuffy", "a day of good care takes a tame adult to GROWN, and Snuffy arrives")
def grows_and_gains_snuffy(game: Game) -> None:
    before = game.count(f"@e[type={SNUFFY},distance=..24]")
    # Tame (Owner), sitting, ADULT one meal short of the threshold, and a LastProcessedDay in the
    # past: the next 40-tick step sees a new day, awards PASSIVE_CARE_PER_DAY (health is full), and
    # crosses into GROWN, which is the moment the mod spawns the companion.
    short = CARE_TO_ADVANCE[2] - PASSIVE_CARE_PER_DAY + 1
    bunny = game.summon(nbt=f"{{Owner:{OWNER},Sitting:1b,GrowthStage:2,CareProgress:{short},"
                            f"LastProcessedDay:-5L,LastFedGameTime:0L,HasSnuffy:0b}}")
    try:
        # A summon with NBT starts at base health (see Game.summon); passive care needs >= 75% of max.
        game.merge(bunny, f"{{Health:{BASE_HEALTH + HEALTH_BONUS[2]}f}}")
        game.settle(ticks=90)
        stage = game.data(bunny, "GrowthStage")
        expect(stage == "3", f"after a processed day the stage is {stage}, not 3 (GROWN): passive care did not advance it "
                             f"(CareProgress now {game.data(bunny, 'CareProgress')})")
        expect(game.attribute(bunny, "minecraft:max_health") == BASE_HEALTH + HEALTH_BONUS[3],
               "GROWN but the attributes were not re-applied on the stage change")
        expect(game.data(bunny, "HasSnuffy") == "1b", "GROWN, but the bunny does not record a Snuffy")
        after = game.count(f"@e[type={SNUFFY},distance=..24]")
        expect(after == before + 1, f"expected one new {SNUFFY} near the bunny, found {after - before}")
        # It is the bunny's own: tame, and owned by the bunny rather than by the player.
        expect(game.count(f"@e[type={SNUFFY},distance=..24,nbt={{Owner:{OWNER}}}]") == 0,
               "the Snuffy is owned by the PLAYER; the mod says it follows the bunny")
    finally:
        game.sweep()


@scenario("care_below_threshold_holds", "a day of care that does not reach the threshold changes nothing but the count")
def care_below_threshold_holds(game: Game) -> None:
    # The falsifier for the scenario above: if stages advanced on every processed day, this would
    # advance too. It must not.
    bunny = game.summon(nbt=f"{{Owner:{OWNER},Sitting:1b,GrowthStage:1,CareProgress:10,"
                            f"LastProcessedDay:-5L,LastFedGameTime:0L}}")
    try:
        game.merge(bunny, f"{{Health:{BASE_HEALTH + HEALTH_BONUS[1]}f}}")
        game.settle(ticks=90)
        expect(game.data(bunny, "GrowthStage") == "1", f"a YOUNG bunny at 10 care advanced to {game.data(bunny, 'GrowthStage')}")
        care = game.number(bunny, "CareProgress")
        expect(care == 10 + PASSIVE_CARE_PER_DAY, f"CareProgress is {care}; one processed day is worth {PASSIVE_CARE_PER_DAY}")
    finally:
        game.sweep()


@scenario("saddlebag_drops", "a Nijntje that dies drops what was in its saddlebag")
def saddlebag_drops(game: Game) -> None:
    bunny = game.summon(nbt='{Items:[{Slot:0b,id:"minecraft:carrot",count:7}]}')
    try:
        expect(game.count("@e[type=minecraft:item,distance=..24,nbt={Item:{id:\"minecraft:carrot\"}}]") == 0,
               "carrot items already lying near spawn; the world is not clean")
        game.bridge.command(f"kill {bunny}")
        time.sleep(0.5)
        dropped = game.count("@e[type=minecraft:item,distance=..24,nbt={Item:{id:\"minecraft:carrot\"}}]")
        expect(dropped >= 1, "the bunny died and no carrot item entity appeared: die() did not drop the saddlebag")
    finally:
        game.sweep()


@scenario("quiet_log", "the mod logged nothing at WARN or worse while the scenarios ran")
def quiet_log(game: Game) -> None:
    noisy = [e for e in game.bridge.log(level="warn", limit=200)
             if MOD in (e.get("logger") or "").lower() or MOD in (e.get("message") or "").lower()
             or MOD in (e.get("thrown") or "").lower()]
    expect(not noisy, "the game logged, about this mod:\n  " + "\n  ".join(
        f"[{e.get('level')}] {e.get('logger')}: {e.get('message')}" for e in noisy[:10]))
