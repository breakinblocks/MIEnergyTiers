package com.breakinblocks.mienergytiers.gui;

import aztech.modern_industrialization.machines.gui.GuiComponentServer;
import com.breakinblocks.mienergytiers.MIEnergyTiers;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import com.breakinblocks.mienergytiers.power.HardPowerState;
import io.netty.buffer.ByteBuf;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class HardPowerGuiComponent implements GuiComponentServer<HardPowerGuiComponent.Params, HardPowerGuiComponent.Data> {
    public static final Type<Params, Data> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MIEnergyTiers.MOD_ID, "hard_power_state"),
            Params.STREAM_CODEC, Data.STREAM_CODEC);

    private final Supplier<HardPowerState> state;
    private final BooleanSupplier activeRecipe;
    private final LongSupplier inputEuPerTick;
    private final Params params;

    public HardPowerGuiComponent(Supplier<HardPowerState> state, int renderX, int renderY) {
        this(state, () -> true, () -> 0, renderX, renderY, false);
    }

    public HardPowerGuiComponent(Supplier<HardPowerState> state, BooleanSupplier activeRecipe,
            LongSupplier inputEuPerTick, int renderX, int renderY, boolean compact) {
        this.state = state;
        this.activeRecipe = activeRecipe;
        this.inputEuPerTick = inputEuPerTick;
        this.params = new Params(renderX, renderY, compact);
    }

    @Override public Params getParams() { return params; }

    @Override
    public Data extractData() {
        HardPowerState value = state.get();
        return new Data(value.requestedEuPerTick(), value.availableEuPerTick(), inputEuPerTick.getAsLong(),
                activeRecipe.getAsBoolean(),
                value.recipeTier() == null ? "" : value.recipeTier().name,
                value.inputTier() == null ? "" : value.inputTier().name,
                value.error());
    }

    @Override public Type<Params, Data> getType() { return TYPE; }

    public record Params(int renderX, int renderY, boolean compact) {
        public static final StreamCodec<ByteBuf, Params> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Params decode(ByteBuf buffer) {
                return new Params(ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                        ByteBufCodecs.BOOL.decode(buffer));
            }

            @Override
            public void encode(ByteBuf buffer, Params value) {
                ByteBufCodecs.VAR_INT.encode(buffer, value.renderX);
                ByteBufCodecs.VAR_INT.encode(buffer, value.renderY);
                ByteBufCodecs.BOOL.encode(buffer, value.compact);
            }
        };
    }

    public record Data(long requested, long available, long inputEuPerTick, boolean activeRecipe,
            String recipeTier, String inputTier, HardPowerError error) {
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Data decode(ByteBuf buffer) {
                return new Data(ByteBufCodecs.VAR_LONG.decode(buffer), ByteBufCodecs.VAR_LONG.decode(buffer),
                        ByteBufCodecs.VAR_LONG.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                        ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                        HardPowerError.values()[ByteBufCodecs.VAR_INT.decode(buffer)]);
            }

            @Override
            public void encode(ByteBuf buffer, Data value) {
                ByteBufCodecs.VAR_LONG.encode(buffer, value.requested);
                ByteBufCodecs.VAR_LONG.encode(buffer, value.available);
                ByteBufCodecs.VAR_LONG.encode(buffer, value.inputEuPerTick);
                ByteBufCodecs.BOOL.encode(buffer, value.activeRecipe);
                ByteBufCodecs.STRING_UTF8.encode(buffer, value.recipeTier);
                ByteBufCodecs.STRING_UTF8.encode(buffer, value.inputTier);
                ByteBufCodecs.VAR_INT.encode(buffer, value.error.ordinal());
            }
        };
    }
}
