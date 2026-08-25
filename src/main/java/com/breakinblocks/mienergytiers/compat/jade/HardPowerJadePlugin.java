package com.breakinblocks.mienergytiers.compat.jade;

import aztech.modern_industrialization.machines.MachineBlock;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.ElectricCraftingMachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.AbstractElectricCraftingMultiblockBlockEntity;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import com.breakinblocks.mienergytiers.MIEnergyTiers;
import com.breakinblocks.mienergytiers.power.HardPowerStateHolder;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerTracker;
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
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                MIEnergyTiers.MOD_ID, "power_input");
        private static final String INPUT_EU = "mi_energy_tiers_input_eu";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof HardPowerStateHolder)) return;
            data.putLong(INPUT_EU, receivedPower(accessor));
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof HardPowerStateHolder)) return;
            tooltip.add(Component.translatable("mi_energy_tiers.jade.input",
                    accessor.getServerData().getLong(INPUT_EU)), UID);
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
            if (accessor.getBlockEntity() instanceof AbstractElectricCraftingMultiblockBlockEntity multiblock) {
                long total = 0;
                for (EnergyComponent energy : multiblock.getEnergyComponents()) {
                    long received = InstantaneousPowerTracker.received(energy, tick);
                    total = received > Long.MAX_VALUE - total ? Long.MAX_VALUE : total + received;
                }
                return total;
            }
            return 0;
        }
    }
}
