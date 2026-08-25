package com.breakinblocks.mienergytiers.gui;

import aztech.modern_industrialization.machines.gui.GuiComponentServer;
import com.breakinblocks.mienergytiers.MIEnergyTiers;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import com.breakinblocks.mienergytiers.power.HardPowerState;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

public final class HardPowerGuiComponent implements GuiComponentServer<Unit, HardPowerGuiComponent.Data> {
    public static final Type<Unit, Data> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MIEnergyTiers.MOD_ID, "hard_power_state"),
            StreamCodec.unit(Unit.INSTANCE), Data.STREAM_CODEC);

    private final Supplier<HardPowerState> state;

    public HardPowerGuiComponent(Supplier<HardPowerState> state) {
        this.state = state;
    }

    @Override public Unit getParams() { return Unit.INSTANCE; }

    @Override
    public Data extractData() {
        HardPowerState value = state.get();
        return new Data(value.requestedEuPerTick(), value.availableEuPerTick(),
                value.recipeTier() == null ? "" : value.recipeTier().name,
                value.inputTier() == null ? "" : value.inputTier().name,
                value.error());
    }

    @Override public Type<Unit, Data> getType() { return TYPE; }

    public record Data(long requested, long available, String recipeTier, String inputTier, HardPowerError error) {
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Data decode(ByteBuf buffer) {
                return new Data(ByteBufCodecs.VAR_LONG.decode(buffer), ByteBufCodecs.VAR_LONG.decode(buffer),
                        ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                        HardPowerError.values()[ByteBufCodecs.VAR_INT.decode(buffer)]);
            }

            @Override
            public void encode(ByteBuf buffer, Data value) {
                ByteBufCodecs.VAR_LONG.encode(buffer, value.requested);
                ByteBufCodecs.VAR_LONG.encode(buffer, value.available);
                ByteBufCodecs.STRING_UTF8.encode(buffer, value.recipeTier);
                ByteBufCodecs.STRING_UTF8.encode(buffer, value.inputTier);
                ByteBufCodecs.VAR_INT.encode(buffer, value.error.ordinal());
            }
        };
    }
}
