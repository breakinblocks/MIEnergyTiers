package com.breakinblocks.mienergytiers.overload;

import aztech.modern_industrialization.api.energy.CableTier;
import com.breakinblocks.mienergytiers.MIEnergyTiers;
import com.breakinblocks.mienergytiers.config.HardEnergyConfig;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Queues destructive work so it is never performed while MI iterates a network. */
public final class OverloadManager {
    private static final Queue<Overload> PENDING = new ConcurrentLinkedQueue<>();

    public static void reject(ServerLevel level, BlockPos position, CableTier offered, CableTier accepted) {
        log(level, position, offered, accepted);
        if (HardEnergyConfig.OVERLOAD_POLICY.get() == HardEnergyConfig.OverloadPolicy.DESTRUCTIVE) {
            PENDING.add(new Overload(level.dimension(), position.immutable(), offered, accepted));
        }
    }

    public static void afterServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Overload overload;
        while ((overload = PENDING.poll()) != null) {
            ServerLevel level = server.getLevel(overload.dimension);
            if (level == null || !level.isLoaded(overload.position)) continue;
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    overload.position.getX() + 0.5, overload.position.getY() + 0.5,
                    overload.position.getZ() + 0.5, 12, 0.25, 0.25, 0.25, 0.02);
            level.playSound(null, overload.position, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.6f, 1.4f);
            level.destroyBlock(overload.position, true);
        }
    }

    private static void log(ServerLevel level, BlockPos position, CableTier offered, CableTier accepted) {
        String message = "Rejected MI overvoltage in {} at {}: offered {}, accepted {}";
        switch (HardEnergyConfig.DIAGNOSTIC_LEVEL.get()) {
            case OFF -> { }
            case INFO -> MIEnergyTiers.LOGGER.info(message, level.dimension().location(), position, offered, accepted);
            case WARN -> MIEnergyTiers.LOGGER.warn(message, level.dimension().location(), position, offered, accepted);
        }
    }

    private record Overload(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            BlockPos position, CableTier offered, CableTier accepted) {}

    private OverloadManager() {}
}
