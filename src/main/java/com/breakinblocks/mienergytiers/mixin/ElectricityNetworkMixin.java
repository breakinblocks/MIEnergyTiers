package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import aztech.modern_industrialization.pipes.electricity.ElectricityNetwork;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.breakinblocks.mienergytiers.energy.EnergyTransferContext;
import com.breakinblocks.mienergytiers.energy.TransferEndpoint;
import com.breakinblocks.mienergytiers.power.NetworkPowerPolicy;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ElectricityNetwork.class)
abstract class ElectricityNetworkMixin {
    @Shadow @Final private static List<MIEnergyStorage> STORAGES_CACHE;
    @Shadow @Final private CableTier tier;

    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
            target = "Laztech/modern_industrialization/pipes/electricity/ElectricityNetwork;transferForTargets(Laztech/modern_industrialization/pipes/electricity/ElectricityNetwork$TransferOperation;Ljava/util/List;J)J",
            ordinal = 0), index = 2)
    private long miEnergyTiers$captureNominalSourceThroughput(long maximum) {
        EnergyTransferContext context = EnergyTransferContext.current();
        if (context != null) {
            // Probe before MI drains source buffers. Each source may contribute at most one
            // nominal tier packet per tick; cable packet capacity must not turn stored generator
            // EU into fresh throughput. Multiple real sources may still aggregate normally.
            context.setFreshAllowance(NetworkPowerPolicy.nominalSourceThroughput(STORAGES_CACHE, tier));
        }
        return maximum;
    }

    @WrapMethod(method = "tick")
    private void miEnergyTiers$carryNetworkTier(ServerLevel world, Operation<Void> original) {
        var context = new EnergyTransferContext(tier,
                TransferEndpoint.unknown("MI electricity network"),
                TransferEndpoint.unknown("MI electricity endpoint"), world.getGameTime());
        context.setFreshAllowance(0);
        try (var ignored = EnergyTransferContext.push(context)) {
            original.call(world);
        }
    }
}
