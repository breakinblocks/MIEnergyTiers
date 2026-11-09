package com.breakinblocks.mienergytiers.overload;

import aztech.modern_industrialization.api.energy.CableTier;
import com.breakinblocks.mienergytiers.MIEnergyTiers;
import com.breakinblocks.mienergytiers.config.HardEnergyConfig;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Queues destructive work so it is never performed while MI iterates a network. */
public final class OverloadManager {
    private static final Queue<Overload> PENDING = new ConcurrentLinkedQueue<>();
    private static final Set<DamageTarget> PENDING_TARGETS = ConcurrentHashMap.newKeySet();
    private static final Map<LogKey, Long> LAST_LOGGED = new ConcurrentHashMap<>();
    private static final long LOG_INTERVAL_TICKS = 6000L;

    public enum Outcome {
        REJECTED,
        DESTRUCTION_QUEUED
    }

    public static Outcome reject(ServerLevel level, BlockPos position, CableTier offered, CableTier accepted) {
        log(level, position, offered, accepted);
        if (HardEnergyConfig.OVERLOAD_POLICY.get() == HardEnergyConfig.OverloadPolicy.REJECT) {
            return Outcome.REJECTED;
        }
        DamageTarget target = new DamageTarget(level.dimension(), position.immutable());
        if (PENDING_TARGETS.add(target)) {
            PENDING.add(new Overload(target, offered, accepted));
        }
        return Outcome.DESTRUCTION_QUEUED;
    }

    public static void afterServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Overload overload;
        while ((overload = PENDING.poll()) != null) {
            PENDING_TARGETS.remove(overload.target);
            // A server owner may switch back to safe rejection before this deferred work runs.
            if (HardEnergyConfig.OVERLOAD_POLICY.get() != HardEnergyConfig.OverloadPolicy.DESTRUCTIVE) continue;
            ServerLevel level = server.getLevel(overload.target.dimension);
            BlockPos position = overload.target.position;
            if (level == null || !level.isLoaded(position)) continue;
            level.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    position.getX() + 0.5,
                    position.getY() + 0.5,
                    position.getZ() + 0.5,
                    12,
                    0.25,
                    0.25,
                    0.25,
                    0.02);
            level.playSound(null, position, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.6f, 1.4f);
            level.destroyBlock(position, true);
        }
    }

    private static void log(ServerLevel level, BlockPos position, CableTier offered, CableTier accepted) {
        if (HardEnergyConfig.DIAGNOSTIC_LEVEL.get() == HardEnergyConfig.DiagnosticLevel.OFF) return;
        long now = level.getGameTime();
        LogKey key = new LogKey(level.dimension(), position.immutable(), offered, accepted);
        Long last = LAST_LOGGED.get(key);
        if (last != null && now - last < LOG_INTERVAL_TICKS && now >= last) return;
        if (LAST_LOGGED.size() > 1024) {
            LAST_LOGGED.values().removeIf(time -> now - time >= LOG_INTERVAL_TICKS || now < time);
        }
        LAST_LOGGED.put(key, now);
        String message = "Rejected MI overvoltage in {} at {}: offered {}, accepted {}";
        switch (HardEnergyConfig.DIAGNOSTIC_LEVEL.get()) {
            case OFF -> {}
            case DEBUG ->
                MIEnergyTiers.LOGGER.debug(message, level.dimension().location(), position, offered, accepted);
            case INFO -> MIEnergyTiers.LOGGER.info(message, level.dimension().location(), position, offered, accepted);
            case WARN -> MIEnergyTiers.LOGGER.warn(message, level.dimension().location(), position, offered, accepted);
        }
    }

    private record DamageTarget(ResourceKey<Level> dimension, BlockPos position) {}

    private record LogKey(ResourceKey<Level> dimension, BlockPos position, CableTier offered, CableTier accepted) {}

    private record Overload(DamageTarget target, CableTier offered, CableTier accepted) {}

    private OverloadManager() {}
}
