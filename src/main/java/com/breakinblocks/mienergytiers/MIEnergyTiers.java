package com.breakinblocks.mienergytiers;

import com.mojang.logging.LogUtils;
import com.breakinblocks.mienergytiers.config.HardEnergyConfig;
import com.breakinblocks.mienergytiers.gametest.HardEnergyGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;
import net.neoforged.neoforge.common.NeoForge;
import com.breakinblocks.mienergytiers.overload.OverloadManager;

@Mod(MIEnergyTiers.MOD_ID)
public final class MIEnergyTiers {
    public static final String MOD_ID = "mi_energy_tiers";
    public static final String SUPPORTED_MI_VERSION = "2.5.6";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MIEnergyTiers(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, HardEnergyConfig.SPEC);
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(HardEnergyGameTests.class));
        NeoForge.EVENT_BUS.addListener(OverloadManager::afterServerTick);
        verifyModernIndustrializationVersion();
    }

    private static void verifyModernIndustrializationVersion() {
        String actual = aztech.modern_industrialization.MI.class.getPackage().getImplementationVersion();
        if (actual != null && !SUPPORTED_MI_VERSION.equals(actual)) {
            throw new IllegalStateException("MI Energy Tiers supports Modern Industrialization "
                    + SUPPORTED_MI_VERSION + " only, but found " + actual
                    + ". Install the supported MI build or a compatible MI Energy Tiers release.");
        }
    }
}
