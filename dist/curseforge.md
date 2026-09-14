![Nijntje](https://raw.githubusercontent.com/mattjesmc/Miffy/main/assets/banners/header.png)

*A tamable bunny that grows into a rideable, armoured battle companion - and brings a puppy.*

[![GitHub release](https://img.shields.io/github/v/release/mattjesmc/Miffy?style=for-the-badge&logo=github&logoColor=white&label=Release&color=f0a830)](https://github.com/mattjesmc/Miffy/releases/latest) ![Loaders](https://img.shields.io/badge/Loader-Fabric-f0a830?style=for-the-badge) ![Minecraft versions](https://img.shields.io/badge/Minecraft-26.2-f0a830?style=for-the-badge) ![License](https://img.shields.io/badge/License-MIT-f0a830?style=for-the-badge)

**Loaders:** Fabric • **Minecraft:** 26.2 • **Side:** Client & Server

[GitHub](https://github.com/mattjesmc/Miffy) • [Issues](https://github.com/mattjesmc/Miffy/issues)

---

## Contents

- About
- Features
- Growing up
- Recipes
- Dependencies
- Incompatibilities
- Installation
- Changelog
- FAQ
- Credits

---

## About

**Nijntje** adds a small white bunny to the overworld. Tame one with a carrot and it
follows you around like a wolf would; keep feeding it and it grows - through four sizes,
from a baby you could hold in one hand to a bunny big enough to saddle and ride into a fight.
A fully grown Nijntje brings a friend: **Snuffy**, a little brown puppy who guards her.

It is one bunny, one puppy, one piece of armour and one spawn egg. No config, no world data
to migrate, nothing else in the game changes.

> An unofficial fan project. Nijntje (Miffy) and Snuffie were created by Dick Bruna and belong
> to Mercis bv; this mod is not affiliated with, endorsed by, or associated with them, or with
> Mojang or Microsoft. See *License* below.

---

## Features

- **Tame with a carrot** — Wild Nijntje spawn in small groups across overworld grassland. Hold out a carrot; roughly one in three lands, like a bone on a wolf. Sneak + right-click tells a tame bunny to sit or stand.
- **Care makes her grow** — Feeding counts toward growth once a minute - a carrot is a meal, a golden carrot a big one, a dandelion a snack - and a healthy bunny earns a little more each day just for being looked after. About thirty carrots of steady care take her from Baby to Grown. Neglect her for three days and she starts to shrink again.
- **Four sizes, four stat lines** — Baby, Young, Adult and Grown each scale the model and hitbox and add health, attack, speed and armour. Babies and youngsters flee from danger; from Adult on she fights for you, attacking whatever you hit and whatever hits you.
- **Saddle up** — A Grown Nijntje takes a saddle. Right-click to hop on; steer as you would a horse, and hold jump to charge a bunny-sized hop. She sits between the ears.
- **Bunny Barding** — A craftable body armour only a Nijntje can wear (+6 armour, +2 toughness). Right-click her with it, or drop it in from a dispenser.
- **A dress in any colour** — Her dress is orange to begin with. Right-click with any dye to recolour it - all sixteen colours, saved with the bunny.
- **Saddlebag** — Right-click an unsaddled or sitting Nijntje to open her nine-slot bag, with bars for how full she is and how far she is from her next size. Everything in it drops if she dies.
- **Snuffy** — The moment a tame Nijntje reaches Grown, Snuffy arrives at her side - a puppy who follows her, leaps at anything that threatens her, and fights beside her. One per bunny, and not for sale.
- **Breeding and a voice** — Two tame Nijntje breed on carrots and the kit is born a Baby, tame to the same owner. She says hello now and then, with subtitles.

---

## Growing up

Growth is care, not age. Each stage needs a set amount of care to leave; a Grown Nijntje
stays Grown as long as she is fed.

| Stage | Size | Health | Attack | Armour | What she does |
| --- | --- | --- | --- | --- | --- |
| Baby | ×0.55 | 10 | 2 | 0 | Flees from everything |
| Young | ×0.85 | 14 | 3 | 0 | Still flees; starts to keep up |
| Adult | ×1.2 | 20 | 5 | 1 | Fights for her owner |
| Grown | ×1.6 | 30 | 8 | 3 | Rideable with a saddle; Snuffy arrives |

---

## Recipes

Rendered from the mod's own recipe file at build time.

| ![Crafting recipe for Bunny Barding](https://raw.githubusercontent.com/mattjesmc/Miffy/main/assets/recipes/nijntje__bunny_barding.png) |
| :---: |
| Bunny Barding |

---

## Dependencies

**Required**

| Mod | Version | Notes |
| --- | --- | --- |
| [Fabric Loader](https://fabricmc.net/use/) | >=0.19.3 |  |
| [Fabric API](https://modrinth.com/mod/fabric-api) | — | Any build for Minecraft 26.2 |
| [Java](https://adoptium.net/) | 25+ |  |

---

## Incompatibilities

**None known.** No conflicts have been reported.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2 on Java 25 or newer.
2. Drop `nijntje-<version>.jar` and [Fabric API](https://modrinth.com/mod/fabric-api) into `mods/`.
3. Launch. Nijntje spawn in overworld grassland; a spawn egg is in the **Nijntje** creative tab.

Install on both sides of a server: the bunny and the puppy are entities, the barding is an
item, and each side needs to know them.

---

## Changelog

Read from `CHANGELOG.md` at build time; the [releases page](https://github.com/mattjesmc/Miffy/releases) carries the jars.

**[0.1.0](https://github.com/mattjesmc/Miffy/releases/tag/v0.1.0)** · 2026-09-14
First release, for Minecraft 26.2 on Fabric.

*Added*

- Nijntje, a tamable bunny that spawns in overworld grassland. Tame with a carrot; sneak + right-click to sit.
- Four growth stages - Baby, Young, Adult, Grown - driven by care rather than age: carrots, golden carrots and dandelions feed her, a healthy day earns a little on its own, and three days without food starts to wind her back.
- Per-stage size, health, attack, speed and armour. Adult and Grown bunnies fight for their owner.
- Riding: a Grown Nijntje takes a saddle, steers like a horse and charges a hop on the jump key.
- Bunny Barding, a craftable body armour (+6 armour, +2 toughness) only a Nijntje can wear.
- A dyeable dress, sixteen colours, saved with the bunny.
- A nine-slot saddlebag with fullness and growth bars, dropped on death.
- Snuffy, the puppy who arrives when a tame Nijntje reaches Grown and defends her.
- Breeding between tame Nijntje; kits are born Baby and tame to the same owner.
- Three ambient voice lines with subtitles, a spawn egg, and a creative tab.

---

## FAQ

**She won't let me ride her.**

Only a **Grown** Nijntje takes a saddle, and a saddled one still has to be standing: sneak + right-click a sitting bunny first. A plain right-click on a bunny that is not rideable opens her saddlebag instead.

**Feeding her does nothing.**

It does, once a minute. A meal fed inside the sixty-second cooldown heals her but does not count toward growth. The green bar in her saddlebag screen shows the progress.

**Where is Snuffy?**

Snuffy arrives when a **tame** Nijntje grows into her final size, and only once per bunny. A Nijntje spawned already Grown never had that moment.

**Can I tame Snuffy, or feed him?**

No. Snuffy belongs to his Nijntje; he follows her, not you, and eats nothing.

**Is there a config file?**

There is not. The mod ships one set of numbers, listed in the growth table above.

---

## Credits

Nijntje and Snuffie are Dick Bruna's; this mod is a fan's attempt to bring them into
Minecraft and claims nothing about them. Models were built in
[Blockbench](https://www.blockbench.net/) (the sources are in `blockbench_sources/`).
Built on [Fabric](https://fabricmc.net/) and Fabric API.

---

## License

The code and build files are under the [MIT License](LICENSE).

The characters are not: Nijntje (Miffy) and Snuffie were created by Dick Bruna and are the
property of Mercis bv. The textures and models here that depict them are fan art made for this
mod; no right in the characters is granted or implied, and this project is not affiliated
with, endorsed by, sponsored by or otherwise associated with Mercis bv, the estate of Dick
Bruna or any of their licensees, nor with Mojang AB or Microsoft. The full notice is in
`LICENSE`.

---

Issues and pull requests are welcome at [github.com/mattjesmc/Miffy](https://github.com/mattjesmc/Miffy).
