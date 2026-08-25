package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import aztech.modern_industrialization.api.energy.CableTierHolder;
import aztech.modern_industrialization.api.energy.EnergyApi;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.TransformerMachineBlockEntity;
import aztech.modern_industrialization.api.machine.holder.EnergyComponentHolder;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.helper.EnergyHelper;
import dev.technici4n.grandpower.api.EnergyStorageUtil;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.breakinblocks.mienergytiers.energy.EnergyTransferContext;
import com.breakinblocks.mienergytiers.energy.TransferEndpoint;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import com.breakinblocks.mienergytiers.overload.OverloadManager;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerTracker;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnergyHelper.class)
abstract class EnergyHelperMixin {
    @WrapMethod(method = "autoOutput(Laztech/modern_industrialization/machines/MachineBlockEntity;Lnet/minecraft/core/Direction;Laztech/modern_industrialization/api/energy/CableTier;Laztech/modern_industrialization/api/energy/MIEnergyStorage;)V")
    private static void miEnergyTiers$carryDeclaredOutputTier(MachineBlockEntity machine, Direction side,
            CableTier output, MIEnergyStorage source, Operation<Void> original) {
        var level = machine.getLevel();
        var destinationPos = machine.getBlockPos().relative(side);
        var destinationStorage = level.getCapability(EnergyApi.SIDED, destinationPos, side.getOpposite());
        var destinationBlockEntity = level.getBlockEntity(destinationPos);
        if (level instanceof ServerLevel serverLevel && destinationStorage != null
                && !destinationStorage.canConnect(output)
                && destinationBlockEntity instanceof CableTierHolder holder
                && output.compareTo(holder.getCableTier()) > 0) {
            OverloadManager.reject(serverLevel, destinationPos, output, holder.getCableTier());
        }
        var sourceEndpoint = new TransferEndpoint(level.dimension(), machine.getBlockPos(), "MI machine output");
        var destinationEndpoint = new TransferEndpoint(level.dimension(), destinationPos, "adjacent energy endpoint");
        var transferContext = new EnergyTransferContext(
                output, sourceEndpoint, destinationEndpoint, level.getGameTime());
        transferContext.setFreshAllowance(Math.max(0, source.extract(output.getEu(), true)));
        try (var ignored = EnergyTransferContext.push(transferContext)) {
            if (machine instanceof TransformerMachineBlockEntity
                    && machine instanceof EnergyComponentHolder holder
                    && holder.getEnergyComponent() instanceof EnergyComponent component) {
                long freshInput = InstantaneousPowerTracker.available(component, level.getGameTime());
                if (destinationStorage != null && destinationStorage.canConnect(output) && freshInput > 0) {
                    long moved = EnergyStorageUtil.move(source, destinationStorage, freshInput);
                    if (moved > 0) {
                        machine.setChanged();
                    }
                }
                return;
            }
            original.call(machine, side, output, source);
        }
    }
}
