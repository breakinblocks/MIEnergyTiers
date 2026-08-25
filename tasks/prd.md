# PRD: Hard Energy Tiering for Modern Industrialization

## Overview

Create a NeoForge addon that adds GregTech-style voltage enforcement to Modern Industrialization. Machines must draw their complete instantaneous EU/t requirement atomically; inadequate power produces no partial progress. Cable and machine tier mismatches are rejected by default, with optional destructive overload behavior.

This document is intended to be saved verbatim as `./tasks/prd.md`.

## Goals

- Prevent recipes from progressing on fractional EU/t.
- Enforce voltage tiers independently from total buffered energy.
- Cover single-block and multiblock crafting machines.
- Preserve MI’s existing energy capability contracts where possible.
- Reject untyped external FE-to-MI input by default.
- Report actionable power errors in machine interfaces.
- Support optional smoke/explosion behavior for overvoltage.

## Quality Gates

These commands must pass for every user story:

- `.\gradlew.bat build`
- `.\gradlew.bat runGameTestServer`

The equivalent `./gradlew` commands may be used on non-Windows systems.

## User Stories

### US-001: Scaffold the NeoForge addon

**Description:** As a modpack developer, I want a standalone MI addon so that hard energy rules can be installed without maintaining a complete MI fork.

**Acceptance Criteria:**

- [ ] Create a Minecraft 1.21.1/NeoForge addon project.
- [ ] Declare Modern Industrialization, GrandPower, and NeoForge version constraints.
- [ ] Register the addon’s common configuration and mixin configuration.
- [ ] Fail startup with an actionable message when the installed MI version is unsupported.
- [ ] Include a minimal GameTest namespace and a passing smoke test.

### US-002: Add tier-aware transfer context

**Description:** As an integration developer, I want energy transfers to retain their voltage tier so that receivers can enforce voltage without replacing MI’s public capability.

**Acceptance Criteria:**

- [ ] Add an addon-owned transfer context containing cable tier, source, destination, and game tick.
- [ ] Add an optional tier-aware endpoint interface or sidecar capability for custom adapters.
- [ ] Instrument `ElectricityNetwork.tick` so cable transfers carry the network’s `CableTier`.
- [ ] Instrument `EnergyHelper.autoOutput` so direct generator and hatch transfers carry their declared output tier.
- [ ] Ensure transfer context is cleared in `finally` blocks and cannot leak between server operations.
- [ ] Preserve the existing `MIEnergyStorage` and `ILongEnergyStorage` method signatures.

### US-003: Enforce atomic power draws for single-block machines

**Description:** As a player, I want an underpowered single-block machine to stop instead of accumulating fractional recipe progress.

**Acceptance Criteria:**

- [ ] Mixin `ElectricCraftingMachineBlockEntity.consumeEu`.
- [ ] Simulate the complete acting recipe draw before consuming energy.
- [ ] When stored energy is below the complete request, consume zero EU and add zero progress.
- [ ] Preserve existing recipe progress during an underpower event.
- [ ] Allow processing to resume when the complete requested EU/t becomes available.
- [ ] Leave steam machines, generators, transformers, storage units, and ordinary energy exports unchanged.
- [ ] Retain an internal policy hook for a future reset-and-waste-inputs mode without enabling it.

### US-004: Enforce recipe voltage tiers

**Description:** As a player, I want high-tier recipes to require an appropriate machine voltage rather than merely a sufficiently large buffer.

**Acceptance Criteria:**

- [ ] Derive the required recipe tier as the lowest registered `CableTier` whose nominal EU value covers the recipe’s base EU/t.
- [ ] Preserve all existing `CrafterComponent.Behavior.banRecipe` checks.
- [ ] Prevent a single-block machine from starting a recipe above its installed casing tier.
- [ ] Prevent overclocking from raising the instantaneous draw above the installed casing’s allowance.
- [ ] Permit higher-tier casings to process lower-tier recipes.
- [ ] Preserve EBF coil limits and other machine-specific recipe restrictions.
- [ ] Report wrong-tier recipes separately from temporary energy starvation.

### US-005: Enforce multiblock hatch voltage

**Description:** As a player, I want multiblocks to require valid energy hatch voltage instead of combining arbitrary low-tier buffers into high-tier power.

**Acceptance Criteria:**

- [ ] Mixin `AbstractElectricCraftingMultiblockBlockEntity.consumeEu`.
- [ ] Track each matched energy input hatch together with its `CableTier`.
- [ ] Require at least one input hatch at or above the recipe’s required voltage tier.
- [ ] Do not combine lower-tier hatch energy to satisfy a higher-voltage recipe.
- [ ] Allow multiple valid same-tier hatches to contribute aggregate throughput.
- [ ] Make the complete aggregate acting draw atomic across participating hatches.
- [ ] Rebuild tier metadata whenever the multiblock shape rematches.
- [ ] Handle mixed-tier hatch structures deterministically and expose an invalid/mixed-tier status when no valid power route exists.

### US-006: Restrict external energy ingress

**Description:** As a modpack developer, I want untyped FE sources excluded from strict MI networks so that generic bridges cannot bypass voltage progression.

**Acceptance Criteria:**

- [ ] Patch the external wrapper creation path or `EnergyApi$WrappedExternalStorage.canConnect`.
- [ ] Reject untyped external FE-to-MI input by default.
- [ ] Preserve MI-to-FE export using `forgeEnergyPerEu`.
- [ ] Allow external input only through an explicit tier-aware adapter.
- [ ] Do not infer voltage from transfer rate or stored energy.
- [ ] Preserve AE2 energy P2P’s declared superconductor classification.
- [ ] Preserve transformer output classification as the transformer’s destination tier.

### US-007: Add configurable overload handling

**Description:** As a modpack developer, I want to choose between safe rejection and destructive overvoltage behavior.

**Acceptance Criteria:**

- [ ] Add `REJECT` and `DESTRUCTIVE` server-config policies.
- [ ] Default to `REJECT`.
- [ ] Cancel mismatched transfers before any EU changes hands.
- [ ] In destructive mode, schedule smoke, sound, and cable damage after network iteration completes.
- [ ] Never remove or explode network blocks while `ElectricityNetwork.tick` is iterating its nodes.
- [ ] Ensure untyped external endpoints cannot trigger destructive effects.
- [ ] Log overload events with dimension, position, offered tier, and accepted tier at a configurable diagnostic level.

### US-008: Expose machine power errors

**Description:** As a player, I want to know why a machine is not progressing so that voltage and generation problems can be diagnosed.

**Acceptance Criteria:**

- [ ] Add an addon-owned `HardPowerState` to electric crafting machines.
- [ ] Track requested EU/t, available EU/t, recipe tier, input tier, and current error.
- [ ] Register a synchronized GUI component for single-block and crafting multiblock machines.
- [ ] Distinguish insufficient instantaneous power, wrong voltage tier, invalid hatch tier, and overvoltage rejection.
- [ ] Do not repurpose MI’s existing multiple-recipe/output-slot warning.
- [ ] Clear temporary errors after valid power is restored.

### US-009: Add regression GameTests

**Description:** As a maintainer, I want automated coverage for strict energy behavior so that MI updates cannot silently restore fractional processing.

**Acceptance Criteria:**

- [ ] Verify a 128 EU/t recipe supplied with only 32 EU/t consumes zero EU and gains zero progress.
- [ ] Verify preserved progress resumes after full power returns.
- [ ] Verify the final processing tick consumes only the remaining recipe energy.
- [ ] Verify LV endpoints reject MV and higher voltage.
- [ ] Verify several LV hatches cannot satisfy an MV-voltage recipe.
- [ ] Verify multiple valid same-tier hatches can satisfy allowed aggregate demand.
- [ ] Verify casing upgrades and overclocking remain bounded by input voltage.
- [ ] Verify FE-to-MI input is rejected while MI-to-FE output remains functional.
- [ ] Verify transformers and AE2 P2P preserve their output tier.
- [ ] Verify save/reload does not duplicate inputs, outputs, progress, or stored EU.

## Functional Requirements

- FR-1: Acting recipe draws must be all-or-nothing.
- FR-2: Failed draws must consume zero energy and add zero progress.
- FR-3: Default underpower behavior must preserve progress and show an error.
- FR-4: Recipe voltage eligibility must be evaluated separately from total stored EU.
- FR-5: Native MI cables, casings, generators, and hatches must retain exact endpoint-tier connectivity.
- FR-6: Multiblock voltage must be derived from participating energy hatches, not the sum of all buffers.
- FR-7: Existing MI and GrandPower capability signatures must remain available.
- FR-8: Untyped external energy must not enter MI strict-mode networks by default.
- FR-9: Overvoltage handling must default to nondestructive rejection.
- FR-10: Destructive effects must be deferred until cable-network iteration is complete.
- FR-11: Steam processing and non-crafting energy consumers must retain existing behavior.
- FR-12: Every blocked machine must expose a specific power failure reason.

## Non-Goals

- Rewriting MI’s cable network into a complete packet/amperage simulator.
- Energy loss over cable distance.
- Refunding recipe inputs after a reset.
- Changing steam machine processing.
- Inferring external voltage from FE/t.
- Rebalancing MI recipes, generators, or cable tier values.
- Supporting arbitrary MI versions without explicit compatibility validation.

## Technical Considerations

- Baseline MI source: `1.21.x`, commit `3bd41dd1`.
- Central processing path: `CrafterComponent.tickRecipe`.
- Primary mixin targets:
  - `ElectricCraftingMachineBlockEntity.consumeEu`
  - `AbstractElectricCraftingMultiblockBlockEntity.consumeEu`
  - `CrafterComponent.canStartRecipe` or its recipe-admission call site
  - `ElectricityNetwork.tick`
  - `EnergyHelper.autoOutput`
  - `EnergyApi$WrappedExternalStorage`
- Native tier definitions are in `CableTier`; cable networks use `tier.eu * 8` as aggregate transfer and node capacity.
- Recipe inputs are consumed when processing starts. The retained future reset policy should therefore explicitly mean reset-and-waste-inputs.
- Mixin injection points must fail loudly when upstream MI bytecode changes.

## Success Metrics

- No electric crafting recipe gains fractional progress from an incomplete EU/t draw.
- No high-voltage recipe runs solely from aggregated lower-tier buffers.
- Existing MI-to-FE compatibility continues to function.
- Every strict-power failure is visible to the player.
- All build and GameTest quality gates pass.

## Open Questions

- None for the initial implementation.
