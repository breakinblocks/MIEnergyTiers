package com.breakinblocks.mienergytiers.client;

import aztech.modern_industrialization.client.machines.GuiComponentsClient;
import com.breakinblocks.mienergytiers.MIEnergyTiers;
import com.breakinblocks.mienergytiers.gui.HardPowerGuiComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = MIEnergyTiers.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientRegistration {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> GuiComponentsClient.register(HardPowerGuiComponent.TYPE, HardPowerGuiClient::new));
    }

    private ClientRegistration() {}
}
