# MI Energy Tiers

A NeoForge 1.21.1 addon for Modern Industrialization 2.5.6 that enforces atomic EU/t draws and explicit voltage tiers.

By default, underpowered electric recipes preserve their progress but consume no energy, untyped FE/GrandPower sources cannot feed MI networks, and overvoltage is safely rejected. The server config can enable deferred destructive overload effects.

Build and verify with:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

Configuration is written to `serverconfig/mi_energy_tiers-server.toml` in each world.

Set `strictEnergy.underpowerPolicy` to `PRESERVE_PROGRESS` (the default) to pause an underpowered
recipe, `DECAY_ONLY` to reverse it to zero while retaining the active craft and its consumed inputs,
or `DECAY_AND_WASTE_INPUTS` to reverse it by one processing tick per underpowered tick and cancel it
at zero without refunding its consumed inputs. A `DECAY_ONLY` craft resumes when full power returns.

Set `strictEnergy.overloadPolicy` to `REJECT` (the default) for safe transfer cancellation, or
`DESTRUCTIVE` to cancel the transfer and defer smoke, explosion sound, and cable/endpoint damage
until after MI's network iteration. Restart the world after editing its server config.

JEI is included in the development client runtime for recipe inspection. Launch it with
`.\gradlew.bat runClient`; JEI is not bundled into or required by the release jar.

Multiblock energy input hatches accept up to two amps each. A recipe may use a native-tier hatch,
or exactly two hatches from the immediately lower voltage tier (for example, two LV hatches for an
MV recipe). Promotion does not cascade across multiple tiers.
