package com.breakinblocks.mienergytiers.compat.jade;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.machine.component.CrafterAccess;
import aztech.modern_industrialization.api.machine.component.EnergyAccess;
import aztech.modern_industrialization.api.machine.holder.CrafterComponentHolder;
import aztech.modern_industrialization.api.machine.holder.EnergyListComponentHolder;
import aztech.modern_industrialization.machines.MachineBlock;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.ElectricCraftingMachineBlockEntity;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import com.breakinblocks.mienergytiers.MIEnergyTiers;
import com.breakinblocks.mienergytiers.energy.TierDisplay;
import com.breakinblocks.mienergytiers.power.HardPowerStateHolder;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerTracker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Optional Jade integration. This class is only discovered when Jade is installed. */
@WailaPlugin
public final class HardPowerJadePlugin implements IWailaPlugin {
    private static final InputProvider INPUT = new InputProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(INPUT, MachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(INPUT, MachineBlock.class);
    }

    private static final class InputProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(MIEnergyTiers.MOD_ID, "power_input");
        private static final String INPUT_EU = "mi_energy_tiers_input_eu";
        private static final String INPUT_TIER = "mi_energy_tiers_input_tier";
        private static final String RECIPE_EU = "mi_energy_tiers_recipe_eu";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof HardPowerStateHolder holder)) return;
            long received = receivedPower(accessor);
            data.putLong(INPUT_EU, received);
            CableTier inputTier = holder.miEnergyTiers$getHardPowerState().inputTier();
            if (received > 0 && inputTier != null) data.putString(INPUT_TIER, inputTier.name);
            if (accessor.getBlockEntity() instanceof CrafterComponentHolder crafterHolder) {
                CrafterAccess crafter = crafterHolder.getCrafterComponent();
                if (crafter.hasActiveRecipe() && crafter.getBaseRecipeEu() > 0) {
                    data.putLong(RECIPE_EU, crafter.getBaseRecipeEu());
                }
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof HardPowerStateHolder)) return;
            CompoundTag data = accessor.getServerData();
            long input = data.getLong(INPUT_EU);
            String inputTier = data.getString(INPUT_TIER);
            tooltip.add(
                    inputTier.isEmpty()
                            ? Component.translatable("mi_energy_tiers.jade.input", input)
                            : Component.translatable(
                                    "mi_energy_tiers.jade.input_tier", input, TierDisplay.nameOrRaw(inputTier, true)),
                    UID);
            long recipeEu = data.getLong(RECIPE_EU);
            if (recipeEu > 0) {
                tooltip.add(
                        Component.translatable("mi_energy_tiers.jade.recipe", TierDisplay.forEu(recipeEu, true)), UID);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        private static long receivedPower(BlockAccessor accessor) {
            long tick = accessor.getLevel().getGameTime();
            if (accessor.getBlockEntity() instanceof ElectricCraftingMachineBlockEntity machine) {
                return InstantaneousPowerTracker.received(machine.getEnergyComponent(), tick);
            }
            if (accessor.getBlockEntity() instanceof EnergyListComponentHolder multiblock) {
                List<EnergyComponent> inputs = new ArrayList<>();
                for (EnergyAccess access : multiblock.getEnergyComponents()) {
                    if (access instanceof EnergyComponent component) inputs.add(component);
                }
                return InstantaneousPowerTracker.received(inputs, tick);
            }
            return 0;
        }
    }
}
