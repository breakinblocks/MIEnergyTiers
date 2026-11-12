package com.breakinblocks.mienergytiers.client;

import aztech.modern_industrialization.client.machines.gui.ClientComponentRenderer;
import aztech.modern_industrialization.client.machines.gui.GuiComponentClient;
import aztech.modern_industrialization.client.machines.gui.MachineScreen;
import aztech.modern_industrialization.client.machines.guicomponents.EnergyBarClient;
import com.breakinblocks.mienergytiers.energy.TierDisplay;
import com.breakinblocks.mienergytiers.gui.HardPowerGuiComponent;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class HardPowerGuiClient
        extends GuiComponentClient<HardPowerGuiComponent.Params, HardPowerGuiComponent.Data> {
    public HardPowerGuiClient(HardPowerGuiComponent.Params params, HardPowerGuiComponent.Data data) {
        super(params, data);
    }

    @Override
    public ClientComponentRenderer createRenderer(MachineScreen machineScreen) {
        return new Renderer();
    }

    private final class Renderer implements ClientComponentRenderer {
        @Override
        public void renderBackground(GuiGraphics graphics, int left, int top) {
            float scale = params.compact() ? 0.75F : 1.0F;
            boolean hasEnoughPower = data.error() == HardPowerError.NONE
                    && (data.activeRecipe()
                            ? data.requested() > 0 && data.available() >= data.requested()
                            : data.inputEuPerTick() > 0);
            graphics.pose().pushPose();
            graphics.pose().translate(left + params.renderX(), top + params.renderY(), 0);
            graphics.pose().scale(scale, scale, 1.0F);
            EnergyBarClient.Renderer.renderEnergy(graphics, 0, 0, hasEnoughPower ? 1.0F : 0.0F);
            graphics.pose().popPose();
        }

        @Override
        public boolean renderTooltip(
                MachineScreen screen, Font font, GuiGraphics graphics, int left, int top, int cursorX, int cursorY) {
            int x = left + params.renderX();
            int y = top + params.renderY();
            int width = params.compact() ? 10 : 13;
            int height = params.compact() ? 14 : 18;
            if (cursorX < x || cursorX >= x + width || cursorY < y || cursorY >= y + height) return false;
            List<Component> lines = new ArrayList<>();
            if (!data.recipeTier().isEmpty()) {
                lines.add(Component.translatable(
                                "mi_energy_tiers.power.recipe_voltage", TierDisplay.nameOrRaw(data.recipeTier(), true))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (!data.inputTier().isEmpty()) {
                lines.add(Component.translatable(
                                "mi_energy_tiers.power.supply_voltage", TierDisplay.nameOrRaw(data.inputTier(), true))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (data.error() == HardPowerError.NONE) {
                if (lines.isEmpty()) return false;
                graphics.renderComponentTooltip(font, lines, cursorX, cursorY);
                return true;
            }
            Component line =
                    switch (data.error()) {
                        case INSUFFICIENT_INSTANTANEOUS_POWER ->
                            Component.translatable(
                                    "mi_energy_tiers.power.insufficient", data.available(), data.requested());
                        case WRONG_RECIPE_TIER ->
                            Component.translatable(
                                    "mi_energy_tiers.power.wrong_tier",
                                    TierDisplay.nameOrRaw(data.recipeTier(), true),
                                    TierDisplay.nameOrRaw(data.inputTier(), true));
                        case INVALID_HATCH_TIER -> Component.translatable("mi_energy_tiers.power.invalid_hatch");
                        case OVERVOLTAGE_REJECTED -> Component.translatable("mi_energy_tiers.power.overvoltage");
                        case NONE -> Component.empty();
                    };
            lines.add(line.copy().withStyle(ChatFormatting.RED));
            graphics.renderComponentTooltip(font, lines, cursorX, cursorY);
            return true;
        }
    }
}
