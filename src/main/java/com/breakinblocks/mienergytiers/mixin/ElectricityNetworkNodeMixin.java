package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.CableTierHolder;
import aztech.modern_industrialization.api.energy.EnergyApi;
import aztech.modern_industrialization.pipes.electricity.ElectricityNetworkNode;
import com.breakinblocks.mienergytiers.overload.OverloadManager;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import com.breakinblocks.mienergytiers.power.HardPowerStateHolder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElectricityNetworkNode.class)
abstract class ElectricityNetworkNodeMixin {
    @Shadow private List<Direction> connections;

    @Inject(method = "appendAttributes", at = @At("HEAD"))
    private void miEnergyTiers$detectDeferredOverload(ServerLevel world, BlockPos cablePos,
            CableTier offered, List<?> storages, CallbackInfo ci) {
        for (Direction direction : connections) {
            BlockPos endpointPos = cablePos.relative(direction);
            var storage = world.getCapability(EnergyApi.SIDED, endpointPos, direction.getOpposite());
            var endpoint = world.getBlockEntity(endpointPos);
            if (storage != null && !storage.canConnect(offered) && endpoint instanceof CableTierHolder holder
                    && offered.compareTo(holder.getCableTier()) > 0) {
                if (endpoint instanceof HardPowerStateHolder stateHolder) {
                    stateHolder.miEnergyTiers$getHardPowerState().update(0, 0, null, holder.getCableTier(),
                            HardPowerError.OVERVOLTAGE_REJECTED);
                }
                // Damage the cable only after the network tick has completed.
                OverloadManager.reject(world, cablePos, offered, holder.getCableTier());
            }
        }
    }
}
