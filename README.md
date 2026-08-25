# MI Energy Tiers

A NeoForge 1.21.1 addon for Modern Industrialization 2.5.6 that enforces atomic EU/t draws and explicit voltage tiers.

By default, underpowered electric recipes preserve their progress but consume no energy, untyped FE/GrandPower sources cannot feed MI networks, and overvoltage is safely rejected. The server config can enable deferred destructive overload effects.

Build and verify with:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

Configuration is written to `serverconfig/mi_energy_tiers-server.toml` in each world.

JEI is included in the development client runtime for recipe inspection. Launch it with
`.\gradlew.bat runClient`; JEI is not bundled into or required by the release jar.
