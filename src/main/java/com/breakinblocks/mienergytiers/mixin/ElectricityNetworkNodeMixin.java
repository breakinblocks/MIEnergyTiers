package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.CableTierHolder;
import aztech.modern_industrialization.api.energy.EnergyApi;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import aztech.modern_industrialization.pipes.electricity.ElectricityNetworkNode;
import com.breakinblocks.mienergytiers.config.HardEnergyConfig;
import com.breakinblocks.mienergytiers.energy.EnergyTransferContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElectricityNetworkNode.class)
abstract class ElectricityNetworkNodeMixin {
    @Shadow private List<Direction> connections;

    @Redirect(method = "canConnect", at = @At(value = "INVOKE",
            target = "Laztech/modern_industrialization/api/energy/MIEnergyStorage;canConnect(Laztech/modern_industrialization/api/energy/CableTier;)Z"))
    private boolean miEnergyTiers$allowDestructiveOvervoltageConnection(MIEnergyStorage storage, CableTier offered,
            Level world, BlockPos cablePos, Direction direction) {
        boolean connects = storage.canConnect(offered);
        if (connects || HardEnergyConfig.OVERLOAD_POLICY.get() != HardEnergyConfig.OverloadPolicy.DESTRUCTIVE) {
            return connects;
        }
        var endpoint = world.getBlockEntity(cablePos.relative(direction));
        return endpoint instanceof CableTierHolder holder
                && offered.compareTo(holder.getCableTier()) > 0;
    }

    @Inject(method = "appendAttributes", at = @At("HEAD"))
    private void miEnergyTiers$detectDeferredOverload(ServerLevel world, BlockPos cablePos,
            CableTier offered, List<?> storages, CallbackInfo ci) {
        for (Direction direction : connections) {
            BlockPos endpointPos = cablePos.relative(direction);
            var storage = world.getCapability(EnergyApi.SIDED, endpointPos, direction.getOpposite());
            var endpoint = world.getBlockEntity(endpointPos);
            if (storage != null && !storage.canConnect(offered) && endpoint instanceof CableTierHolder holder
                    && offered.compareTo(holder.getCableTier()) > 0) {
                EnergyTransferContext context = EnergyTransferContext.current();
                if (context != null) {
                    context.recordOverload(cablePos, endpointPos, holder.getCableTier());
                }
            }
        }
    }
}
