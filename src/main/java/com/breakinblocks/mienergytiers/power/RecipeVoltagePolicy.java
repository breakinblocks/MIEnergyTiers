package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.machines.recipe.MachineRecipe;

public interface RecipeVoltagePolicy {
    boolean miEnergyTiers$isVoltageAllowed(MachineRecipe recipe);

    long miEnergyTiers$maxRecipeEu();
}
