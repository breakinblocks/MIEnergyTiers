package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.gui.GuiComponentServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MachineBlockEntity.class)
public interface MachineBlockEntityAccessor {
    @Invoker("registerGuiComponent")
    void miEnergyTiers$registerGuiComponent(GuiComponentServer<?, ?>... components);
}
