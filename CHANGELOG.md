# Changelog

## 1.0.1

- The machine casing now sets a machine's voltage. An advanced hull runs MV recipes, a turbo hull runs HV, and so on, with no upgrades needed.
- Machine upgrades now buy amperage at the machine's own voltage instead of raising the voltage. They stop helping at 8 amps, which is everything a cable of that tier carries.
- Multiblock voltage and throughput are bounded by what the energy input hatches can actually deliver.
- Overclocking no longer costs a machine ticks for asking above its own tier.
- Fixed a multiblock that could stay stuck on a hatch tier it no longer needed after an overclock.
- Machine hulls and upgrades have tooltips for their voltage and amperage. Hold Shift on an upgrade for the per-voltage breakdown.

## 1.0.0

- Initial release.
