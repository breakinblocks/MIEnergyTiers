# MI Energy Tiers

A NeoForge 1.21.1 addon for Modern Industrialization 2.5.6 that enforces atomic EU/t draws and explicit voltage tiers.

By default, underpowered electric recipes preserve their progress but consume no energy, untyped FE/GrandPower sources cannot feed MI networks, and overvoltage is safely rejected. The server config can enable deferred destructive overload effects.

Build and verify with:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

Configuration is written to `serverconfig/mi_energy_tiers-server.toml` in each world.

Set `strictEnergy.overloadPolicy` to `REJECT` (the default) for safe transfer cancellation, or
`DESTRUCTIVE` to cancel the transfer and defer smoke, explosion sound, and cable/endpoint damage
until after MI's network iteration. Restart the world after editing its server config.

JEI is included in the development client runtime for recipe inspection. Launch it with
`.\gradlew.bat runClient`; JEI is not bundled into or required by the release jar.
