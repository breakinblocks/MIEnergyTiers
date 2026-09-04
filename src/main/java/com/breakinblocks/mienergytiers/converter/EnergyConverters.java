package com.breakinblocks.mienergytiers.converter;

import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.EnergyApi;
import aztech.modern_industrialization.machines.BEP;
import com.breakinblocks.mienergytiers.MIEnergyTiers;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public final class EnergyConverters {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MIEnergyTiers.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MIEnergyTiers.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MIEnergyTiers.MOD_ID);
    private static final Map<CableTier, Converter> CONVERTERS = new LinkedHashMap<>();
    private static final ResourceLocation MI_TAB = MI.id("general");

    public record Converter(CableTier tier, DeferredBlock<EnergyConverterBlock> block, DeferredItem<BlockItem> item,
            DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyConverterBlockEntity>> type) {}

    public static void register(IEventBus modBus) {
        for (CableTier tier : CableTier.allTiers()) {
            String name = name(tier);
            DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyConverterBlockEntity>> type =
                    DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, id(tier));
            DeferredBlock<EnergyConverterBlock> block = BLOCKS.register(name, () -> new EnergyConverterBlock(tier, type));
            DeferredItem<BlockItem> item = ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
            BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder
                    .of((pos, state) -> new EnergyConverterBlockEntity(new BEP(type.get(), pos, state), tier), block.get())
                    .build(null));
            CONVERTERS.put(tier, new Converter(tier, block, item, type));
        }
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(EnergyConverters::registerCapabilities);
        modBus.addListener(EnergyConverters::addToCreativeTab);
    }

    public static String name(CableTier tier) {
        return tier.name + "_fe_converter";
    }

    public static ResourceLocation id(CableTier tier) {
        return ResourceLocation.fromNamespaceAndPath(MIEnergyTiers.MOD_ID, name(tier));
    }

    public static Collection<Converter> all() {
        return Collections.unmodifiableCollection(CONVERTERS.values());
    }

    public static @Nullable Converter get(CableTier tier) {
        return CONVERTERS.get(tier);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (Converter converter : CONVERTERS.values()) {
            BlockEntityType<EnergyConverterBlockEntity> type = converter.type().get();
            event.registerBlockEntity(EnergyApi.SIDED, type, EnergyConverterBlockEntity::miStorage);
            event.registerBlockEntity(ILongEnergyStorage.BLOCK, type, EnergyConverterBlockEntity::feStorage);
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, EnergyConverterBlockEntity::feStorage);
        }
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(MI_TAB)) return;
        for (Converter converter : CONVERTERS.values()) {
            event.accept(converter.item().get());
        }
    }

    private EnergyConverters() {}
}
