package com.breakinblocks.mienergytiers.client;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.machines.components.CasingComponent;
import aztech.modern_industrialization.machines.components.UpgradeComponent;
import com.breakinblocks.mienergytiers.MIEnergyTiers;
import com.breakinblocks.mienergytiers.energy.AmperagePolicy;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = MIEnergyTiers.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class VoltageTooltips {
    @SubscribeEvent
    public static void appendVoltageInfo(ItemTooltipEvent event) {
        CableTier casing = CasingComponent.getCasingTier(event.getItemStack().getItem());
        if (casing != null) {
            appendCasing(event.getToolTip(), casing);
            return;
        }
        long upgradeEu = UpgradeComponent.getExtraEu(event.getItemStack().getItem());
        if (upgradeEu > 0) appendUpgrade(event.getToolTip(), upgradeEu);
    }

    private static void appendCasing(List<Component> tooltip, CableTier tier) {
        long maxAmps = AmperagePolicy.maxAmps(tier);
        tooltip.add(Component.translatable("mi_energy_tiers.tooltip.casing.voltage",
                tier.shortEnglishName(), amount(tier.getEu())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mi_energy_tiers.tooltip.casing.recipes",
                amount(tier.getEu())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mi_energy_tiers.tooltip.casing.amps",
                amount(AmperagePolicy.maxRecipeEu(tier, Long.MAX_VALUE)), maxAmps).withStyle(ChatFormatting.GRAY));
    }

    private static void appendUpgrade(List<Component> tooltip, long upgradeEu) {
        tooltip.add(Component.translatable("mi_energy_tiers.tooltip.upgrade.amperage",
                amount(upgradeEu)).withStyle(ChatFormatting.AQUA));
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("mi_energy_tiers.tooltip.shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        for (CableTier tier : CableTier.allTiers()) {
            Component line = upgradeLine(tier, upgradeEu);
            if (line != null) tooltip.add(line);
        }
    }

    private static @Nullable Component upgradeLine(CableTier tier, long upgradeEu) {
        long maxAmps = AmperagePolicy.maxAmps(tier);
        long perUpgrade = AmperagePolicy.ampsPerUpgrade(tier, upgradeEu);
        if (perUpgrade >= maxAmps - 1) {
            return Component.translatable("mi_energy_tiers.tooltip.upgrade.full",
                    tier.shortEnglishName(), maxAmps).withStyle(ChatFormatting.GRAY);
        }
        if (perUpgrade >= 1) {
            return Component.translatable("mi_energy_tiers.tooltip.upgrade.per_item",
                    tier.shortEnglishName(), perUpgrade).withStyle(ChatFormatting.GRAY);
        }
        long needed = AmperagePolicy.upgradesPerAmp(tier, upgradeEu);
        if (needed > 64) return null;
        return Component.translatable("mi_energy_tiers.tooltip.upgrade.per_amp",
                tier.shortEnglishName(), needed).withStyle(ChatFormatting.GRAY);
    }

    private static String amount(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private VoltageTooltips() {}
}
