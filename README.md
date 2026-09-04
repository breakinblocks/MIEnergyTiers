# MI Energy Tiers

MI Energy Tiers is a NeoForge 1.21.1 addon for Modern Industrialization 2.5.6. It makes MI's electric progression behave more like GregTech: voltage tier and available EU/t matter, not just how much energy happens to be stored in a machine.

## What changes

- Electric recipes draw their complete EU/t atomically. If a machine needs 128 EU/t but receives only 96 EU/t, it consumes nothing and cannot gain normal progress.
- The casing sets a single-block machine's voltage, and the casing alone is enough: an advanced machine hull runs MV recipes with no upgrades installed. Stock MI ignores the casing here and holds every single-block machine to 32 EU/t plus whatever upgrades are inside it.
- Recipes must fit that voltage. A large internal buffer cannot bypass an LV, MV, or HV requirement.
- Machine upgrades buy amperage at the machine's own voltage instead of raising the voltage. See the next section.
- Overclocking is bounded by the same ceiling. A machine speeds up until it draws its full amperage and no further, and it never stalls for asking beyond its tier.
- Multiblocks take voltage and throughput from their energy input hatches. Each hatch supplies up to two amps of its own tier, and 2 to 4 hatches from the tier directly below a recipe may bootstrap the next voltage tier. Promotion cannot cascade across several tiers, and a multiblock never promises more EU/t than its hatches can deliver.
- Transformers retain their destination voltage classification, while untyped FE/GrandPower input is rejected by default. MI-to-FE export remains available.
- Machine GUIs use a lightning indicator: red means usable power is arriving (or an active recipe has its full EU/t), while gray means disconnected, invalid, or underpowered. Jade also reports live input such as `Input: 32 EU/t`.

This is deliberately GregTech-inspired rather than a full packet-and-amperage simulation. MI keeps its existing EU storage and transfer APIs, recipes, cable values, and general machine behavior; the addon adds strict voltage provenance and all-or-nothing crafting draws around them.

## Machine upgrades and amperage

A machine's ceiling is its voltage times its amperage. Every machine starts at one amp, and upgrades add more:

```
amps = 1 + (total upgrade EU / voltage EU), up to 8 amps
ceiling = voltage EU * amps
```

Eight amps is where a cable of that tier tops out, so no amount of upgrades takes a machine past it.

| Casing | One amp | One more amp needs | Full 8 amps |
| --- | --- | --- | --- |
| LV | 32 EU/t | 32 EU of upgrades (2 advanced, or 16 basic) | 256 EU/t |
| MV (advanced hull) | 128 EU/t | 128 EU (2 turbo) | 1,024 EU/t |
| HV (turbo hull) | 1,024 EU/t | 1,024 EU (2 highly advanced) | 8,192 EU/t |
| EV (highly advanced hull) | 8,192 EU/t | 8,192 EU (16 highly advanced) | 65,536 EU/t |
| Superconductor (quantum hull) | 128,000,000 EU/t | a quantum upgrade | 1,024,000,000 EU/t |

Upgrade values are MI's own: basic 2, advanced 16, turbo 64, highly advanced 512, quantum 999,999,999, multiplied by the stack size in the slot.

Things worth knowing:

- Upgrades worth less than one amp at that voltage do nothing. A lone advanced upgrade adds 16 EU against an LV amp of 32, so it changes nothing until a second one joins it.
- Amps have to come from somewhere. Each energy source contributes at most one nominal packet of its tier per tick, so a machine pulling four amps needs several generators or buffers on the same network.
- Multiblock ceilings are also capped by hatch capacity. Two LV hatches promoted to MV supply exactly one MV amp, which is 128 EU/t.
- The fusion reactor asks for 128,000 EU/t, more than any route built from EV hatches can carry, so it needs at least one superconductor energy input hatch.
- MI's quantum armor recipes ask for 1,000,000 EU/t, so the packer running them needs a quantum machine hull.

## Configuration

World-specific settings are written to `serverconfig/mi_energy_tiers-server.toml`.

`strictEnergy.underpowerPolicy` controls what happens when full EU/t disappears during a craft:

- `PRESERVE_PROGRESS` pauses the recipe without consuming power or losing progress (default).
- `DECAY_ONLY` reverses progress toward zero but keeps the active craft and its consumed inputs indefinitely.
- `DECAY_AND_WASTE_INPUTS` reverses progress and cancels at zero without refunding consumed inputs.

`strictEnergy.overloadPolicy` controls overvoltage:

- `REJECT` safely cancels the transfer (default).
- `DESTRUCTIVE` permits the mismatched physical connection but blocks EU transfer. Once energized, it defers smoke, sound, and cable or endpoint damage until MI finishes iterating the network.

Diagnostic logging and rejection of untyped external input are also configurable. Restart the world after changing its server config.

## Development

JEI and Jade are included in the development runtime for recipe and power inspection, but are not bundled with the release jar.

```powershell
.\gradlew.bat runClient
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```
