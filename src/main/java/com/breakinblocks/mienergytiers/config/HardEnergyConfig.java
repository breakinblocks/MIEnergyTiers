package com.breakinblocks.mienergytiers.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import com.breakinblocks.mienergytiers.power.UnderpowerPolicy;

public final class HardEnergyConfig {
    public enum OverloadPolicy { REJECT, DESTRUCTIVE }
    public enum DiagnosticLevel { OFF, INFO, WARN }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<OverloadPolicy> OVERLOAD_POLICY;
    public static final ModConfigSpec.EnumValue<UnderpowerPolicy> UNDERPOWER_POLICY;
    public static final ModConfigSpec.EnumValue<DiagnosticLevel> DIAGNOSTIC_LEVEL;
    public static final ModConfigSpec.BooleanValue REJECT_UNTYPED_EXTERNAL_INPUT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("strictEnergy");
        UNDERPOWER_POLICY = builder.comment(
                "Behavior when an active electric recipe cannot draw its complete instantaneous EU/t.",
                "PRESERVE_PROGRESS: pause without changing progress or inputs.",
                "DECAY_ONLY: reverse by one processing tick per underpowered tick, stopping at zero without canceling the craft.",
                "DECAY_AND_WASTE_INPUTS: reverse by one processing tick per underpowered tick; at zero, cancel without refunding inputs.")
                .defineEnum("underpowerPolicy", UnderpowerPolicy.PRESERVE_PROGRESS);
        OVERLOAD_POLICY = builder.comment(
                "Behavior when a typed source exceeds the receiver voltage.",
                "REJECT: cancel the transfer without changing blocks.",
                "DESTRUCTIVE: cancel the transfer, then emit smoke/sound and damage the overloaded cable or endpoint after network iteration.")
                .defineEnum("overloadPolicy", OverloadPolicy.REJECT);
        DIAGNOSTIC_LEVEL = builder.comment("Logging level for rejected overvoltage transfers.")
                .defineEnum("diagnosticLevel", DiagnosticLevel.INFO);
        REJECT_UNTYPED_EXTERNAL_INPUT = builder.comment("Reject FE/GrandPower input without an explicit voltage adapter.")
                .define("rejectUntypedExternalInput", true);
        builder.pop();
        SPEC = builder.build();
    }

    private HardEnergyConfig() {}
}
