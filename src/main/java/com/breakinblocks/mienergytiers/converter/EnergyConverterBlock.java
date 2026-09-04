package com.breakinblocks.mienergytiers.converter;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineBlock;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

public final class EnergyConverterBlock extends MachineBlock {
    private final CableTier tier;

    public EnergyConverterBlock(CableTier tier, Supplier<BlockEntityType<EnergyConverterBlockEntity>> type) {
        super((pos, state) -> new EnergyConverterBlockEntity(new BEP(type.get(), pos, state), tier),
                Properties.of().destroyTime(4.0f).explosionResistance(6.0f).sound(SoundType.METAL).noLootTable());
        this.tier = tier;
    }

    public CableTier tier() {
        return tier;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("mi_energy_tiers.tooltip.converter.voltage",
                tier.shortEnglishName(), tier.getEu()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mi_energy_tiers.tooltip.converter.amps",
                EnergyConverterBlockEntity.AMPS, EnergyConverterBlockEntity.maxEuPerTick(tier))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mi_energy_tiers.tooltip.converter.faces").withStyle(ChatFormatting.GRAY));
    }
}
