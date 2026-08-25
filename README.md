# MI Energy Tiers

MI Energy Tiers is a NeoForge 1.21.1 addon for Modern Industrialization 2.5.6. It makes MI's electric progression behave more like GregTech: voltage tier and available EU/t matter, not just how much energy happens to be stored in a machine.

## What changes

- Electric recipes draw their complete EU/t atomically. If a machine needs 128 EU/t but receives only 96 EU/t, it consumes nothing and cannot gain normal progress.
- Recipes must fit the voltage tier of a single-block machine. A large internal buffer cannot bypass an LV, MV, or HV requirement.
- Overclocking remains limited by the machine's installed voltage tier.
- Multiblocks derive voltage and throughput from their energy input hatches. Each hatch accepts up to two amps, and 2–4 hatches from the tier directly below a recipe may bootstrap the next voltage tier. Promotion cannot cascade across several tiers.
- Transformers retain their destination voltage classification, while untyped FE/GrandPower input is rejected by default. MI-to-FE export remains available.
- Machine GUIs use a lightning indicator: red means usable power is arriving (or an active recipe has its full EU/t), while gray means disconnected, invalid, or underpowered. Jade also reports live input such as `Input: 32 EU/t`.

This is deliberately GregTech-inspired rather than a full packet-and-amperage simulation. MI keeps its existing EU storage and transfer APIs, recipes, cable values, and general machine behavior; the addon adds strict voltage provenance and all-or-nothing crafting draws around them.

## Configuration

World-specific settings are written to `serverconfig/mi_energy_tiers-server.toml`.

`strictEnergy.underpowerPolicy` controls what happens when full EU/t disappears during a craft:

- `PRESERVE_PROGRESS` pauses the recipe without consuming power or losing progress (default).
- `DECAY_ONLY` reverses progress toward zero but keeps the active craft and its consumed inputs indefinitely.
- `DECAY_AND_WASTE_INPUTS` reverses progress and cancels at zero without refunding consumed inputs.

`strictEnergy.overloadPolicy` controls overvoltage:

- `REJECT` safely cancels the transfer (default).
- `DESTRUCTIVE` cancels it, then defers smoke, sound, and cable or endpoint damage until MI finishes iterating the network.

Diagnostic logging and rejection of untyped external input are also configurable. Restart the world after changing its server config.

## Development

JEI and Jade are included in the development runtime for recipe and power inspection, but are not bundled with the release jar.

```powershell
.\gradlew.bat runClient
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```
