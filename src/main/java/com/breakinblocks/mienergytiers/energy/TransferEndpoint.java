package com.breakinblocks.mienergytiers.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record TransferEndpoint(@Nullable ResourceKey<Level> dimension, @Nullable BlockPos position, String description) {
    public static TransferEndpoint unknown(String description) {
        return new TransferEndpoint(null, null, description);
    }
}
