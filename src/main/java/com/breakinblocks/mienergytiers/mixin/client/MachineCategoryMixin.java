package com.breakinblocks.mienergytiers.mixin.client;

import aztech.modern_industrialization.client.compat.viewer.usage.MachineCategory;
import aztech.modern_industrialization.compat.rei.machines.MachineCategoryParams;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import com.breakinblocks.mienergytiers.energy.TierDisplay;
import com.breakinblocks.mienergytiers.energy.TierUtil;
import com.breakinblocks.mienergytiers.power.EnergyFreeRecipes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MachineCategory.class, remap = false)
abstract class MachineCategoryMixin {
    @Shadow
    @Final
    private MachineCategoryParams params;

    @WrapOperation(
            method = "buildWidgets",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Laztech/modern_industrialization/util/TextHelper;getEuTextTick(J)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent miEnergyTiers$appendTier(
            long eu, Operation<MutableComponent> original, @Local MachineRecipe recipe) {
        MutableComponent text = original.call(eu);
        if (!miEnergyTiers$showsTier(recipe)) return text;
        return text.append(" ").append(TierDisplay.forEu(recipe.eu, false));
    }

    @WrapOperation(
            method = "buildWidgets",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Laztech/modern_industrialization/compat/rei/machines/MachineCategoryParams;getRecipeTooltip(Laztech/modern_industrialization/machines/recipe/MachineRecipe;)Ljava/util/List;"))
    private List<Component> miEnergyTiers$addTierTooltip(
            MachineCategoryParams instance, MachineRecipe recipe, Operation<List<Component>> original) {
        List<Component> tooltips = original.call(instance, recipe);
        if (!miEnergyTiers$showsTier(recipe)) return tooltips;
        List<Component> lines = new ArrayList<>(tooltips);
        lines.add(Component.translatable(
                        "mi_energy_tiers.viewer.voltage",
                        TierDisplay.forEu(recipe.eu, true),
                        TierUtil.forEu(recipe.eu).getEu())
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Unique
    private boolean miEnergyTiers$showsTier(MachineRecipe recipe) {
        return params.steamMode.electric && recipe.eu > 0 && !EnergyFreeRecipes.isFree(recipe);
    }
}
