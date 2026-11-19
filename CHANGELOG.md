# Changelog

## 1.0.12

### Fixed

- A machine whose overclock asked for more EU/t than its line supplied ate a set of inputs every tick and made no
  progress, so a run of crafts lost most of its items. The overclock now runs on the power that arrives, as long as
  that covers the recipe's own EU/t, and settles back to what the line can keep up with.

## 1.0.11

### Changed

- The lightning bolt on an idle machine now lights only when the line has power left over for it. Before, it lit
  whenever the line carried any power, so a machine waiting behind others that used the whole supply looked ready
  and gave no reason for sitting idle.
- Hovering the bolt on an idle machine shows the line's supply and how much of it is not in use, or says that no
  power is arriving.

## 1.0.10

### Fixed

- The recipe viewer listed "Requires at least +N EU/t of upgrades" on every recipe above 32 EU/t and showed the
  upgrade items beside it. That is the stock rule, which this mod replaces: the machine hull or the energy hatches
  set the voltage, and a recipe runs at one amp with no upgrades. The line and the upgrade items are gone, and the
  voltage line already there says what the recipe needs.

### Changed

- Built against Modern Industrialization 2.5.12, which is now the minimum, along with NeoForge 21.1.238.

## 1.0.9

### Changed

- Energy input hatches hold what they can pass in one tick, two amps of their tier, instead of thirty seconds of
  power. A multiblock now runs on the power that arrives each tick, the same as a single-block machine, and an idle
  one no longer fills its hatches.
- A cable network takes from generators and storage only what the machines on it will accept that tick, so cables
  no longer fill up with stored EU.
- Jade no longer shows a stored EU bar on cables. It still shows the cable's voltage tier and the EU/t moving
  through it.
- Jade shows the live input on an energy input hatch in place of its buffer.

## 1.0.8

### Fixed

- A machine whose fresh power fell short of a recipe's EU/t still started the recipe and took its inputs, then
  failed the first draw, dropped the recipe and started it again, using up a set of inputs every tick. A recipe now
  starts only once at least its EU/t has arrived as fresh power, and the machine shows the shortfall until it does.

## 1.0.7

### Fixed

- A multiblock whose energy hatch was swapped for a different tier kept using the old hatch's tier until the
  controller was broken and replaced, so an MV hatch that replaced an LV one was still treated as LV and capped
  the machine at the LV draw. Hatch tiers are now read again before every draw.

## 1.0.6

### Added

- Every electric MI recipe in JEI, EMI and REI shows its voltage tier after the EU/t, colored by tier, and its tooltip
  names the tier and its EU/t per amp.
- The power indicator on a machine screen shows the recipe and supply voltage when hovered, not only when something is
  wrong.
- Jade shows the supply tier next to a machine's input and the voltage of the recipe it is running.

### Fixed

- A Tesseract batch multiblock, such as the MI Tweaks Large Macerator, stopped for good when a batch needed more EU/t
  than its cable delivered, since a batch's EU/t is fixed when it starts. It now draws for as many whole recipes of the
  batch as the fresh power covers and runs slower, so each recipe still gets its full EU/t at its own tier.
- Tesseract batch multiblocks show the power indicator on their screen, and Jade shows their incoming power instead of
  0 EU/t.

## 1.0.5

### Fixed

- Recipes carrying MIEI's `miei:no_energy` condition never started on a multiblock with no energy hatch, because the
  voltage check and the recipe EU limit both rejected them. They are now exempt from both, since they draw no EU.
- A converter feeding a machine above its tier logged "Rejected MI overvoltage" every tick. It is now logged at debug
  level by default, and each position and tier pair at most once every five minutes. `diagnosticLevel` takes a new
  `DEBUG` value, which is the new default; `INFO` and `WARN` still log at those levels. (FTBTesting/Testing-Issues#4939)

## 1.0.4

### Fixed

- A multiblock with two or more energy hatches on the same cable network could crash the server with "MI hatch energy
  changed between atomic simulation and commit" when the network supplied less than the recipe needed. Each hatch was
  being credited the whole network's power while planning the draw. (breakinblocks/MIEnergyTiers#1)
- The machine screen and Jade showed incoming power multiplied by the number of hatches on one network.

## 1.0.3

### Fixed

- Tesseract API detection now checks the loaded mod list so the Tesseract mixins are applied or skipped based on whether the mod
  is actually installed.

## 1.0.2

### Added

- Voltage enforcement now covers Tesseract API multiplied ("batch") multiblocks, which MI Tweaks
  registers.
- A mixin config plugin, so the Tesseract mixins are skipped when that API is not installed.

## 1.0.1

- Added FE Converters, one per cable tier. FE goes in on any face except the output face and comes out as EU of that tier, up to four amps per tick. They are crafted like the transformer of the same tier, with a redstone block in place of the second cable.
- An energy source can now declare its amperage (`AmperageSource`); cable networks and direct output honour it instead of assuming one packet per source.
- The machine casing now sets a machine's voltage. An advanced hull runs MV recipes, a turbo hull runs HV, and so on, with no upgrades needed.
- Machine upgrades now buy amperage at the machine's own voltage instead of raising the voltage. They stop helping at 8 amps, which is everything a cable of that tier carries.
- Multiblock voltage and throughput are bounded by what the energy input hatches can actually deliver.
- Overclocking no longer costs a machine ticks for asking above its own tier.
- Fixed a multiblock that could stay stuck on a hatch tier it no longer needed after an overclock.
- Machine hulls and upgrades have tooltips for their voltage and amperage. Hold Shift on an upgrade for the per-voltage breakdown.

## 1.0.0

- Initial release.
