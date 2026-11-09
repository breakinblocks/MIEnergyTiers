package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.machines.recipe.condition.MachineProcessCondition;
import aztech.modern_industrialization.machines.recipe.condition.MachineProcessConditions;
import net.minecraft.resources.ResourceLocation;

public final class EnergyFreeRecipes {
    private static final ResourceLocation NO_ENERGY = ResourceLocation.fromNamespaceAndPath("miei", "no_energy");

    private EnergyFreeRecipes() {}

    public static boolean isFree(MachineRecipe recipe) {
        for (MachineProcessCondition condition : recipe.conditions) {
            if (NO_ENERGY.equals(MachineProcessConditions.getId(condition.codec()))) {
                return true;
            }
        }
        return false;
    }
}
