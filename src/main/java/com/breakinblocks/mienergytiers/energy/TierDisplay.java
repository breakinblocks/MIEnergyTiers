package com.breakinblocks.mienergytiers.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

public final class TierDisplay {
    private static final List<ChatFormatting> LIGHT_BACKGROUND = List.of(
            ChatFormatting.DARK_GREEN,
            ChatFormatting.DARK_AQUA,
            ChatFormatting.GOLD,
            ChatFormatting.DARK_PURPLE,
            ChatFormatting.DARK_BLUE,
            ChatFormatting.DARK_RED);

    private static final List<ChatFormatting> DARK_BACKGROUND = List.of(
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.GOLD,
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.BLUE,
            ChatFormatting.RED);

    public static MutableComponent name(CableTier tier, boolean darkBackground) {
        return tier.shortEnglishName().copy().withStyle(color(tier, darkBackground));
    }

    public static MutableComponent forEu(long euPerTick, boolean darkBackground) {
        return name(TierUtil.forEu(euPerTick), darkBackground);
    }

    public static @Nullable CableTier byName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (CableTier tier : CableTier.allTiers()) {
            if (tier.name.equals(name)) return tier;
        }
        return null;
    }

    public static Component nameOrRaw(String name, boolean darkBackground) {
        CableTier tier = byName(name);
        return tier == null ? Component.literal(name) : name(tier, darkBackground);
    }

    private static ChatFormatting color(CableTier tier, boolean darkBackground) {
        List<ChatFormatting> palette = darkBackground ? DARK_BACKGROUND : LIGHT_BACKGROUND;
        int index = CableTier.allTiers().indexOf(tier);
        if (index < 0) return palette.getLast();
        return palette.get(Math.min(index, palette.size() - 1));
    }

    private TierDisplay() {}
}
