package com.breakinblocks.mienergytiers.converter;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.CableTierHolder;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import aztech.modern_industrialization.api.machine.component.EnergyAccess;
import aztech.modern_industrialization.api.machine.holder.EnergyComponentHolder;
import aztech.modern_industrialization.config.MIServerConfig;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.components.OrientationComponent;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.machines.guicomponents.EnergyBar;
import aztech.modern_industrialization.machines.helper.EnergyHelper;
import aztech.modern_industrialization.machines.models.MachineModelClientData;
import aztech.modern_industrialization.util.Simulation;
import aztech.modern_industrialization.util.Tickable;
import com.breakinblocks.mienergytiers.energy.EnergyTransferContext;
import com.breakinblocks.mienergytiers.power.AmperageSource;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public final class EnergyConverterBlockEntity extends MachineBlockEntity
        implements Tickable, EnergyComponentHolder, CableTierHolder {
    public static final long AMPS = 4;
    private static final long BUFFER_TICKS = 20;

    private final CableTier tier;
    private final EnergyComponent energy;
    private final Output output;
    private final Sealed sealed = new Sealed();
    private final FeInput feInput = new FeInput();
    private long outputTick = Long.MIN_VALUE;
    private long outputThisTick;
    private long inputTick = Long.MIN_VALUE;
    private long inputThisTick;

    public EnergyConverterBlockEntity(BEP bep, CableTier tier) {
        super(bep, new MachineGuiParameters.Builder(EnergyConverters.id(tier), false).build(),
                OrientationComponent.Params.noFacing(false, false));
        this.tier = tier;
        this.energy = new EnergyComponent(this, capacity(tier));
        this.output = new Output(energy.buildExtractable(candidate -> candidate == tier));
        registerComponents(energy);
        registerGuiComponent(new EnergyBar(new EnergyBar.Params(76, 39), energy::getEu, energy::getCapacity));
    }

    public static long maxEuPerTick(CableTier tier) {
        long eu = tier.getEu();
        return eu > Long.MAX_VALUE / AMPS ? Long.MAX_VALUE : eu * AMPS;
    }

    private static long capacity(CableTier tier) {
        long perTick = maxEuPerTick(tier);
        return perTick > Long.MAX_VALUE / BUFFER_TICKS ? Long.MAX_VALUE : perTick * BUFFER_TICKS;
    }

    public static long forgeEnergyPerEu() {
        return Math.max(1, MIServerConfig.INSTANCE.forgeEnergyPerEu.getAsInt());
    }

    public CableTier tier() {
        return tier;
    }

    @Override
    public CableTier getCableTier() {
        return tier;
    }

    @Override
    public EnergyAccess getEnergyComponent() {
        return energy;
    }

    public EnergyComponent energy() {
        return energy;
    }

    public MIEnergyStorage miStorage(@Nullable Direction side) {
        return side != null && side == orientation.outputDirection ? output : sealed;
    }

    public @Nullable ILongEnergyStorage feStorage(@Nullable Direction side) {
        return side != null && side == orientation.outputDirection ? null : feInput;
    }

    @Override
    public MIInventory getInventory() {
        return MIInventory.EMPTY;
    }

    @Override
    public MachineModelClientData getMachineModelData() {
        MachineModelClientData data = new MachineModelClientData(tier.casing);
        orientation.writeModelData(data);
        return data;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide()) return;
        EnergyHelper.autoOutput(this, orientation, tier, output);
    }

    private long gameTime() {
        return level == null ? 0 : level.getGameTime();
    }

    private long outputBudget() {
        long tick = gameTime();
        if (tick != outputTick) {
            outputTick = tick;
            outputThisTick = 0;
        }
        return Math.max(0, maxEuPerTick(tier) - outputThisTick);
    }

    private long inputBudget() {
        long tick = gameTime();
        if (tick != inputTick) {
            inputTick = tick;
            inputThisTick = 0;
        }
        return Math.max(0, maxEuPerTick(tier) - inputThisTick);
    }

    private final class Output implements MIEnergyStorage, AmperageSource {
        private final MIEnergyStorage delegate;

        private Output(MIEnergyStorage delegate) {
            this.delegate = delegate;
        }

        @Override
        public long amperage() {
            return AMPS;
        }

        @Override
        public boolean canConnect(CableTier candidate) {
            return delegate.canConnect(candidate);
        }

        @Override
        public long receive(long maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public long extract(long maxExtract, boolean simulate) {
            long allowed = Math.min(maxExtract, outputBudget());
            if (allowed <= 0) return 0;
            long moved = delegate.extract(allowed, simulate);
            if (!simulate && moved > 0) outputThisTick += moved;
            return moved;
        }

        @Override
        public long getAmount() {
            return delegate.getAmount();
        }

        @Override
        public long getCapacity() {
            return delegate.getCapacity();
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        @Override
        public boolean canExtract() {
            return delegate.canExtract();
        }
    }

    private static final class Sealed implements MIEnergyStorage.NoInsert, MIEnergyStorage.NoExtract {
        @Override
        public boolean canConnect(CableTier candidate) {
            return false;
        }

        @Override
        public long getAmount() {
            return 0;
        }

        @Override
        public long getCapacity() {
            return 0;
        }
    }

    private final class FeInput implements ILongEnergyStorage {
        @Override
        public long receive(long maxReceive, boolean simulate) {
            if (EnergyTransferContext.current() != null) return 0;
            long ratio = forgeEnergyPerEu();
            long euBudget = Math.min(inputBudget(), energy.getRemainingCapacity());
            long eu = Math.min(maxReceive / ratio, euBudget);
            if (eu <= 0) return 0;
            if (!simulate) {
                long inserted = energy.insertEu(eu, Simulation.ACT);
                inputThisTick += inserted;
                if (inserted > 0) setChanged();
                return inserted * ratio;
            }
            return eu * ratio;
        }

        @Override
        public long extract(long maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public long getAmount() {
            return energy.getEu() * forgeEnergyPerEu();
        }

        @Override
        public long getCapacity() {
            return energy.getCapacity() * forgeEnergyPerEu();
        }

        @Override
        public boolean canReceive() {
            return EnergyTransferContext.current() == null;
        }

        @Override
        public boolean canExtract() {
            return false;
        }
    }
}
