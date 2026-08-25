package com.breakinblocks.mienergytiers.client;

import aztech.modern_industrialization.client.machines.gui.ClientComponentRenderer;
import aztech.modern_industrialization.client.machines.gui.GuiComponentClient;
import aztech.modern_industrialization.client.machines.gui.MachineScreen;
import com.breakinblocks.mienergytiers.gui.HardPowerGuiComponent;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;

public final class HardPowerGuiClient extends GuiComponentClient<Unit, HardPowerGuiComponent.Data> {
    public HardPowerGuiClient(Unit params, HardPowerGuiComponent.Data data) {
        super(params, data);
    }

    @Override
    public ClientComponentRenderer createRenderer(MachineScreen machineScreen) {
        return new Renderer();
    }

    private final class Renderer implements ClientComponentRenderer {
        @Override
        public void renderBackground(GuiGraphics graphics, int left, int top) {
            if (data.error() != HardPowerError.NONE) {
                graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, "⚡", left + 7, top + 7, 0xffff5555, false);
            }
        }

        @Override
        public boolean renderTooltip(MachineScreen screen, Font font, GuiGraphics graphics,
                int left, int top, int cursorX, int cursorY) {
            if (data.error() == HardPowerError.NONE || cursorX < left + 5 || cursorX >= left + 17
                    || cursorY < top + 5 || cursorY >= top + 17) return false;
            Component line = switch (data.error()) {
                case INSUFFICIENT_INSTANTANEOUS_POWER -> Component.translatable(
                        "mi_energy_tiers.power.insufficient", data.available(), data.requested());
                case WRONG_RECIPE_TIER -> Component.translatable(
                        "mi_energy_tiers.power.wrong_tier", data.recipeTier(), data.inputTier());
                case INVALID_HATCH_TIER -> Component.translatable("mi_energy_tiers.power.invalid_hatch");
                case OVERVOLTAGE_REJECTED -> Component.translatable("mi_energy_tiers.power.overvoltage");
                case NONE -> Component.empty();
            };
            graphics.renderComponentTooltip(font, List.of(line.copy().withStyle(ChatFormatting.RED)), cursorX, cursorY);
            return true;
        }
    }
}
