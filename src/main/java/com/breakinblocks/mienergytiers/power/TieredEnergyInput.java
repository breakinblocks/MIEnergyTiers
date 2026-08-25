package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.machines.components.EnergyComponent;

public record TieredEnergyInput(EnergyComponent energy, CableTier tier) {}
