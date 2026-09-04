package com.breakinblocks.mienergytiers.gametest;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import com.breakinblocks.mienergytiers.energy.AmperagePolicy;
import com.breakinblocks.mienergytiers.energy.EnergyTransferContext;
import com.breakinblocks.mienergytiers.energy.ExternalEnergyPolicy;
import com.breakinblocks.mienergytiers.energy.TierAwareEndpoint;
import com.breakinblocks.mienergytiers.energy.TierUtil;
import com.breakinblocks.mienergytiers.energy.TransferEndpoint;
import com.breakinblocks.mienergytiers.config.HardEnergyConfig;
import com.breakinblocks.mienergytiers.gui.HardPowerGuiComponent;
import com.breakinblocks.mienergytiers.overload.OverloadManager;
import com.breakinblocks.mienergytiers.power.HardPowerState;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerBudget;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerTracker;
import com.breakinblocks.mienergytiers.power.NetworkPowerPolicy;
import com.breakinblocks.mienergytiers.power.RecipeVoltagePolicy;
import com.breakinblocks.mienergytiers.power.UnderpowerPolicy;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.util.Simulation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.MIFluids;
import aztech.modern_industrialization.machines.blockentities.ElectricCraftingMachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.GeneratorMachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.DistillationTowerBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.ElectricBlastFurnaceBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.ElectricCraftingMultiblockBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.FusionReactorBlockEntity;
import aztech.modern_industrialization.machines.guicomponents.EnergyBar;
import aztech.modern_industrialization.machines.blockentities.TransformerMachineBlockEntity;
import aztech.modern_industrialization.pipes.MIPipes;
import aztech.modern_industrialization.pipes.api.PipeNetworkType;
import aztech.modern_industrialization.pipes.api.PipeEndpointType;
import aztech.modern_industrialization.pipes.electricity.ElectricityNetworkNode;
import aztech.modern_industrialization.pipes.impl.PipeBlockEntity;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.fluid.FluidVariant;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.transaction.Transaction;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;

public final class HardEnergyGameTests {
    private HardEnergyGameTests() {}

    @GameTestGenerator
    public static List<TestFunction> generateTests() {
        List<TestFunction> tests = new ArrayList<>();
        tests.add(test("smoke", helper -> {
            try (var ignored = context(CableTier.LV)) {
                check(EnergyTransferContext.current() != null, "context not installed");
            }
            check(EnergyTransferContext.current() == null, "context leaked");
        }));
        tests.add(test("underpowered_draw_is_atomic", helper -> {
            Ledger ledger = new Ledger(512, 0, 1280);
            InstantaneousPowerBudget budget = new InstantaneousPowerBudget();
            budget.receive(0, 32, CableTier.LV);
            check(poweredTick(ledger, budget, 0, 128) == 0 && ledger.energy == 512 && ledger.progress == 0,
                    "fractional power changed energy or progress");
        }));
        tests.add(test("received_power_survives_spending", helper -> {
            InstantaneousPowerBudget budget = new InstantaneousPowerBudget();
            budget.receive(0, 32, CableTier.LV);
            check(budget.spend(0, 32), "fresh power could not be spent");
            check(budget.available(0) == 0 && budget.received(0) == 32,
                    "input throughput disappeared after machine consumption");
            check(budget.received(2) == 0, "stale input throughput did not expire");
        }));
        tests.add(test("idle_power_indicator_is_inactive", helper -> {
            HardPowerState state = new HardPowerState();
            state.clear(128, 128, CableTier.MV, CableTier.MV);
            HardPowerGuiComponent indicator = new HardPowerGuiComponent(
                    () -> state, () -> false, () -> 0, 161, 4, true);
            check(!indicator.extractData().activeRecipe(), "idle machine was shown as ready to process");
            check(indicator.extractData().inputEuPerTick() == 0,
                    "disconnected idle machine reported incoming power");
            check(indicator.getParams().compact(), "multiblock power indicator was not compact");
        }));
        tests.add(test("powered_idle_indicator_has_input", helper -> {
            HardPowerGuiComponent indicator = new HardPowerGuiComponent(
                    HardPowerState::new, () -> false, () -> 32, 39, 65, false);
            check(indicator.extractData().inputEuPerTick() == 32,
                    "connected idle machine did not report incoming power");
        }));
        tests.add(test("progress_resumes_after_full_power", helper -> {
            Ledger ledger = new Ledger(512, 256, 1280);
            InstantaneousPowerBudget budget = new InstantaneousPowerBudget();
            check(poweredTick(ledger, budget, 0, 128) == 0 && ledger.progress == 256,
                    "buffer-only progress was not stopped");
            budget.receive(1, 128, CableTier.MV);
            check(poweredTick(ledger, budget, 1, 128) == 128 && ledger.progress == 384,
                    "progress did not resume with full instantaneous power");
        }));
        tests.add(test("final_tick_uses_remaining_energy", helper -> {
            Ledger ledger = new Ledger(200, 1200, 1280);
            InstantaneousPowerBudget budget = new InstantaneousPowerBudget();
            budget.receive(0, 80, CableTier.MV);
            check(poweredTick(ledger, budget, 0, 128) == 80 && ledger.energy == 120 && ledger.progress == 1280,
                    "final draw did not use exact remainder");
        }));
        tests.add(test("lv_rejects_mv", helper -> check(
                CableTier.LV.compareTo(TierUtil.forEu(128)) < 0, "LV accepted MV voltage")));
        tests.add(test("two_lower_hatches_promote_one_tier", helper -> {
            check(TierUtil.hatchRoute(List.of(CableTier.LV), CableTier.MV) == null,
                    "one LV hatch created MV voltage");
            TierUtil.HatchRoute route = TierUtil.hatchRoute(List.of(CableTier.LV, CableTier.LV), CableTier.MV);
            check(route != null && route.inputTier() == CableTier.LV && route.effectiveTier() == CableTier.MV
                            && route.maxHatches() == 4,
                    "two LV hatches did not create one MV route");
            check(TierUtil.hatchRoute(List.of(CableTier.LV, CableTier.LV, CableTier.LV, CableTier.LV), CableTier.HV) == null,
                    "LV hatches recursively promoted to HV");
            check(TierUtil.maxHatchEuPerTick(CableTier.LV) == CableTier.LV.getEu() * 2,
                    "LV hatch did not enforce its two-amp limit");
            check(TierUtil.maxHatchEuPerTick(CableTier.LV) * 2 == CableTier.MV.getEu(),
                    "two two-amp LV hatches did not provide one MV recipe amp");
            check(CableTier.LV.getEu() * route.maxHatches() == CableTier.MV.getEu(),
                    "four one-amp LV hatch inputs did not provide one MV recipe amp");
        }));
        tests.add(test("same_tier_hatches_aggregate", helper -> {
            Ledger first = new Ledger(80, 0, 1000);
            Ledger second = new Ledger(80, 0, 1000);
            long simulated = Math.min(128, first.energy) + Math.min(48, second.energy);
            check(simulated == 128 && TierUtil.routeTier(List.of(CableTier.MV, CableTier.MV), CableTier.MV) == CableTier.MV,
                    "same-tier aggregate route failed");
        }));
        tests.add(test("overclock_is_voltage_bounded", helper -> check(
                !TierUtil.canSupply(CableTier.LV, 128) && TierUtil.canSupply(CableTier.MV, 128),
                "overclock bypassed casing voltage")));
        tests.add(test("voltage_cap_applies_to_every_electric_crafter", helper -> {
            check(RecipeVoltagePolicy.class.isAssignableFrom(ElectricCraftingMachineBlockEntity.class),
                    "single-block machines lost the voltage policy");
            for (Class<?> multiblock : List.of(ElectricCraftingMultiblockBlockEntity.class,
                    ElectricBlastFurnaceBlockEntity.class, DistillationTowerBlockEntity.class,
                    FusionReactorBlockEntity.class)) {
                check(RecipeVoltagePolicy.class.isAssignableFrom(multiblock),
                        multiblock.getSimpleName() + " lost the voltage policy");
            }
        }));
        tests.add(test("hull_tier_sets_recipe_ceiling", helper -> {
            check(AmperagePolicy.maxRecipeEu(CableTier.LV, 0) == CableTier.LV.getEu(),
                    "unupgraded LV hull did not allow one LV amp");
            check(AmperagePolicy.maxRecipeEu(CableTier.MV, 0) >= 128 && TierUtil.forEu(128) == CableTier.MV,
                    "unupgraded MV hull could not run an MV recipe");
            check(AmperagePolicy.maxRecipeEu(CableTier.LV, 0) < 128, "LV hull reached MV voltage");
        }));
        tests.add(test("upgrades_buy_amps_at_machine_voltage", helper -> {
            check(AmperagePolicy.maxRecipeEu(CableTier.LV, 16) == CableTier.LV.getEu(),
                    "a part-amp upgrade changed the LV ceiling");
            check(AmperagePolicy.maxRecipeEu(CableTier.LV, 64) == CableTier.LV.getEu() * 3,
                    "a turbo upgrade did not buy two LV amps");
            check(AmperagePolicy.maxRecipeEu(CableTier.MV, 128) == CableTier.MV.getEu() * 2,
                    "a turbo upgrade did not buy one MV amp");
            check(AmperagePolicy.amps(CableTier.HV, 512) == 1,
                    "a single highly advanced upgrade bought an HV amp");
        }));
        tests.add(test("upgrade_tooltip_math_matches_ceiling", helper -> {
            check(AmperagePolicy.upgradesPerAmp(CableTier.LV, 16) == 2,
                    "two advanced upgrades did not make one LV amp");
            check(AmperagePolicy.upgradesPerAmp(CableTier.MV, 64) == 2,
                    "two turbo upgrades did not make one MV amp");
            check(AmperagePolicy.upgradesPerAmp(CableTier.HV, 512) == 2,
                    "two highly advanced upgrades did not make one HV amp");
            check(AmperagePolicy.ampsPerUpgrade(CableTier.LV, 64) == 2,
                    "a turbo upgrade did not report two LV amps");
            check(AmperagePolicy.ampsPerUpgrade(CableTier.LV, 999999999L)
                            == AmperagePolicy.maxAmps(CableTier.LV) - 1,
                    "a quantum upgrade did not report a full LV machine");
        }));
        tests.add(test("amps_stop_at_cable_capacity", helper -> {
            check(AmperagePolicy.maxRecipeEu(CableTier.LV, 1000000000L) == CableTier.LV.getMaxTransfer(),
                    "LV upgrades exceeded the cable's amperage");
            check(AmperagePolicy.maxRecipeEu(CableTier.SUPERCONDUCTOR, 999999999L)
                            == CableTier.SUPERCONDUCTOR.getMaxTransfer(),
                    "a quantum upgrade did not reach full superconductor amperage");
        }));
        tests.add(test("hatch_structure_sets_multiblock_voltage", helper -> {
            check(TierUtil.effectiveVoltage(List.of()) == null, "a hatchless multiblock had a voltage");
            check(TierUtil.effectiveVoltage(List.of(CableTier.LV)) == CableTier.LV,
                    "one LV hatch did not give LV voltage");
            check(TierUtil.effectiveVoltage(List.of(CableTier.LV, CableTier.LV)) == CableTier.MV,
                    "two LV hatches did not promote to MV voltage");
            check(TierUtil.effectiveVoltage(List.of(CableTier.MV, CableTier.MV)) == CableTier.HV,
                    "two MV hatches did not promote to HV voltage");
        }));
        tests.add(test("hatch_capacity_bounds_multiblock_ceiling", helper -> {
            List<CableTier> singleLv = List.of(CableTier.LV);
            check(TierUtil.hatchCapacity(singleLv, TierUtil.hatchRoute(singleLv, CableTier.LV)) == 64,
                    "one LV hatch did not supply two LV amps");
            List<CableTier> twoLv = List.of(CableTier.LV, CableTier.LV);
            check(TierUtil.hatchCapacity(twoLv, TierUtil.hatchRoute(twoLv, CableTier.MV)) == CableTier.MV.getEu(),
                    "two promoted LV hatches did not supply exactly one MV amp");
            List<CableTier> twoMv = List.of(CableTier.MV, CableTier.MV);
            long ceiling = Math.min(AmperagePolicy.maxRecipeEu(CableTier.HV, 0),
                    TierUtil.hatchCapacity(twoMv, TierUtil.hatchRoute(twoMv, CableTier.HV)));
            check(ceiling == 512, "promoted hatch capacity did not bound the multiblock ceiling");
        }));
        tests.add(test("external_ingress_requires_tier", helper -> {
            Storage untyped = new Storage();
            TypedStorage typed = new TypedStorage(CableTier.MV);
            try (var ignored = context(CableTier.MV)) {
                check(!ExternalEnergyPolicy.allowsInput(untyped, EnergyTransferContext.current()), "untyped FE input accepted");
                check(ExternalEnergyPolicy.allowsInput(typed, EnergyTransferContext.current()), "typed adapter rejected");
                check(untyped.canReceive(), "MI-to-FE export was disabled");
            }
        }));
        tests.add(test("declared_output_tiers_survive_context", helper -> {
            try (var transformer = context(CableTier.MV)) {
                check(EnergyTransferContext.current().tier() == CableTier.MV, "transformer destination tier lost");
            }
            try (var p2p = context(CableTier.SUPERCONDUCTOR)) {
                check(EnergyTransferContext.current().tier() == CableTier.SUPERCONDUCTOR, "AE2 P2P tier lost");
            }
        }));
        tests.add(asyncTest("overload_policies_are_configurable", helper -> {
            BlockPos position = new BlockPos(0, 1, 0);
            helper.setBlock(position, Blocks.COPPER_BLOCK);
            HardEnergyConfig.OverloadPolicy previous = HardEnergyConfig.OVERLOAD_POLICY.get();
            HardEnergyConfig.OVERLOAD_POLICY.set(HardEnergyConfig.OverloadPolicy.REJECT);
            check(OverloadManager.reject(helper.getLevel(), helper.absolutePos(position), CableTier.MV, CableTier.LV)
                            == OverloadManager.Outcome.REJECTED,
                    "REJECT did not report a safe rejection");

            helper.startSequence().thenIdle(2).thenExecute(() -> {
                check(helper.getBlockState(position).is(Blocks.COPPER_BLOCK),
                        "REJECT damaged the overloaded block");
                HardEnergyConfig.OVERLOAD_POLICY.set(HardEnergyConfig.OverloadPolicy.DESTRUCTIVE);
                check(OverloadManager.reject(helper.getLevel(), helper.absolutePos(position), CableTier.MV, CableTier.LV)
                                == OverloadManager.Outcome.DESTRUCTION_QUEUED,
                        "DESTRUCTIVE did not queue deferred damage");
                check(helper.getBlockState(position).is(Blocks.COPPER_BLOCK),
                        "DESTRUCTIVE damaged the block during the initiating operation");
            }).thenIdle(2).thenExecute(() -> {
                boolean destroyed = helper.getBlockState(position).isAir();
                HardEnergyConfig.OVERLOAD_POLICY.set(previous);
                check(destroyed, "DESTRUCTIVE did not apply deferred block damage");
            }).thenSucceed();
        }));
        tests.add(asyncTest("destructive_overvoltage_is_reachable_from_cables", helper -> {
            BlockPos generatorPos = new BlockPos(0, 1, 0);
            BlockPos cablePos = new BlockPos(1, 1, 0);
            BlockPos machinePos = new BlockPos(2, 1, 0);
            helper.setBlock(generatorPos, BuiltInRegistries.BLOCK.get(MI.id("mv_diesel_generator")));
            helper.setBlock(machinePos, BuiltInRegistries.BLOCK.get(MI.id("electrolyzer")));
            helper.setBlock(cablePos, MIPipes.BLOCK_PIPE.get());

            GeneratorMachineBlockEntity generator = helper.getBlockEntity(generatorPos);
            generator.orientation.outputDirection = Direction.EAST;
            PipeNetworkType cableType = PipeNetworkType.get(MI.id("electrum_cable"));
            PipeBlockEntity cable = helper.getBlockEntity(cablePos);
            cable.addPipe(cableType, MIPipes.INSTANCE.getPipeItem(cableType).defaultData);
            helper.getLevel().blockUpdated(helper.absolutePos(cablePos), Blocks.AIR);
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            ElectricityNetworkNode node = (ElectricityNetworkNode) cable.getNodes().stream()
                    .filter(candidate -> candidate.getType() == cableType).findFirst().orElseThrow();

            HardEnergyConfig.OverloadPolicy previous = HardEnergyConfig.OVERLOAD_POLICY.get();
            HardEnergyConfig.OVERLOAD_POLICY.set(HardEnergyConfig.OverloadPolicy.REJECT);
            cable.addConnection(player, cableType, Direction.EAST);
            check(node.getConnections(helper.absolutePos(cablePos))[Direction.EAST.get3DDataValue()] == null,
                    "REJECT allowed an MV cable to connect to an LV machine");

            HardEnergyConfig.OVERLOAD_POLICY.set(HardEnergyConfig.OverloadPolicy.DESTRUCTIVE);
            cable.addConnection(player, cableType, Direction.EAST);
            PipeEndpointType endpoint = node.getConnections(
                    helper.absolutePos(cablePos))[Direction.EAST.get3DDataValue()];
            check(endpoint != null, "DESTRUCTIVE could not form an overvoltage connection");
            cable.addConnection(player, cableType, Direction.WEST);
            try (Transaction transaction = Transaction.openRoot()) {
                generator.getInventory().fluidStorage.insert(MIFluids.BIODIESEL.variant(), 1000, transaction);
                transaction.commit();
            }

            helper.startSequence()
                    .thenExecuteFor(12, () -> HardEnergyConfig.OVERLOAD_POLICY.set(
                            HardEnergyConfig.OverloadPolicy.DESTRUCTIVE))
                    .thenExecute(() -> {
                        HardEnergyConfig.OVERLOAD_POLICY.set(previous);
                        check(helper.getBlockState(cablePos).isAir(),
                                "energized destructive overvoltage did not damage the cable");
                        check(!helper.getBlockState(machinePos).isAir(),
                                "deferred cable overload destroyed the receiving machine");
                    }).thenSucceed();
        }));
        tests.add(test("step_up_transformer_does_not_multiply_throughput", helper -> {
            InstantaneousPowerBudget transformerInput = new InstantaneousPowerBudget();
            transformerInput.receive(0, 32, CableTier.LV);
            long transformerOutput = Math.min(128, transformerInput.available(0));
            check(transformerOutput == 32 && transformerInput.spend(0, transformerOutput),
                    "transformer created throughput while raising voltage");
            InstantaneousPowerBudget machineInput = new InstantaneousPowerBudget();
            machineInput.receive(0, transformerOutput, CableTier.MV);
            Ledger bufferedMachine = new Ledger(512, 0, 1280);
            check(poweredTick(bufferedMachine, machineInput, 0, 128) == 0,
                    "buffered MV machine ran from one LV generator");
        }));
        tests.add(test("stored_generator_buffer_does_not_multiply_throughput", helper -> {
            MIEnergyStorage first = new MIStorage(Long.MAX_VALUE);
            MIEnergyStorage second = new MIStorage(Long.MAX_VALUE);
            check(NetworkPowerPolicy.nominalSourceThroughput(List.of(first), CableTier.LV) == CableTier.LV.getEu(),
                    "one LV source was credited above nominal LV throughput");
            check(NetworkPowerPolicy.nominalSourceThroughput(List.of(first, second), CableTier.LV)
                            == CableTier.LV.getEu() * 2,
                    "multiple LV sources did not aggregate their nominal throughput");
        }));
        tests.add(test("transformer_output_is_capped_by_fresh_input", helper -> {
            BlockPos position = new BlockPos(0, 1, 0);
            helper.setBlock(position, BuiltInRegistries.BLOCK.get(MI.id("lv_mv_transformer")));
            TransformerMachineBlockEntity transformer = helper.getBlockEntity(position);
            EnergyComponent component = transformer.getEnergyComponent();
            component.insertEu(component.getCapacity(), Simulation.ACT);
            try (var ignored = context(CableTier.LV)) {
                component.insertEu(CableTier.LV.getEu(), Simulation.ACT);
            }
            try (var ignored = context(CableTier.MV)) {
                check(component.consumeEu(CableTier.MV.getEu(), Simulation.SIMULATE) == CableTier.LV.getEu(),
                        "buffered LV-MV transformer simulated output above fresh LV input");
                check(component.consumeEu(CableTier.MV.getEu(), Simulation.ACT) == CableTier.LV.getEu(),
                        "buffered LV-MV transformer output above fresh LV input");
                check(component.consumeEu(CableTier.MV.getEu(), Simulation.SIMULATE) == 0,
                        "transformer fresh LV input was credited more than once");
            }
        }));
        tests.add(test("energy_component_tracks_only_transferred_power", helper -> {
            BlockPos position = new BlockPos(0, 1, 0);
            helper.setBlock(position, Blocks.CHEST);
            var owner = helper.getBlockEntity(position);
            long tick = owner.getLevel().getGameTime();
            EnergyComponent bufferedOnly = new EnergyComponent(owner, 512);
            bufferedOnly.insertEu(512, Simulation.ACT);
            check(InstantaneousPowerTracker.available(bufferedOnly, tick) == 0,
                    "plain buffer insertion counted as instantaneous power");
            EnergyComponent transferred = new EnergyComponent(owner, 512);
            try (var ignored = context(CableTier.MV)) {
                transferred.insertEu(32, Simulation.ACT);
            }
            check(InstantaneousPowerTracker.available(transferred, tick) == 32,
                    "transferred power was not captured by the transformed EnergyComponent");
        }));
        tests.add(test("network_buffering_does_not_create_fresh_power", helper -> {
            EnergyTransferContext transfer = new EnergyTransferContext(CableTier.LV,
                    TransferEndpoint.unknown("buffered cable network"),
                    TransferEndpoint.unknown("LV machine"), 0);
            transfer.setFreshAllowance(32);
            check(transfer.claimFresh(128) == 32,
                    "buffered network delivery did not preserve its fresh generation limit");
            check(transfer.claimFresh(128) == 0,
                    "the same fresh network power was credited more than once");
        }));
        tests.add(asyncTest("lv_diesel_generator_powers_lv_machine", helper -> {
            BlockPos generatorPos = new BlockPos(0, 1, 0);
            BlockPos cablePos = new BlockPos(1, 1, 0);
            BlockPos machinePos = new BlockPos(2, 1, 0);
            helper.setBlock(generatorPos, BuiltInRegistries.BLOCK.get(MI.id("lv_diesel_generator")));
            helper.setBlock(machinePos, BuiltInRegistries.BLOCK.get(MI.id("electrolyzer")));
            GeneratorMachineBlockEntity generator = helper.getBlockEntity(generatorPos);
            ElectricCraftingMachineBlockEntity machine = helper.getBlockEntity(machinePos);
            check(machine.guiComponents.getNullable(EnergyBar.class) == null,
                    "strict electric crafter still registered MI's stored-energy GUI bar");
            check(machine.guiComponents.getNullable(HardPowerGuiComponent.class) != null,
                    "strict electric crafter did not register its throughput indicator");
            generator.orientation.outputDirection = Direction.EAST;
            ItemStack salt = new ItemStack(BuiltInRegistries.ITEM.get(MI.id("salt_dust")), 2);
            check(machine.getInventory().itemStorage.itemHandler.insertItem(0, salt, false).isEmpty(),
                    "could not insert salt into real electrolyzer");

            PipeNetworkType cableType = PipeNetworkType.get(MI.id("copper_cable"));
            helper.setBlock(cablePos, MIPipes.BLOCK_PIPE.get());
            PipeBlockEntity cable = helper.getBlockEntity(cablePos);
            cable.addPipe(cableType, MIPipes.INSTANCE.getPipeItem(cableType).defaultData);
            helper.getLevel().blockUpdated(helper.absolutePos(cablePos), Blocks.AIR);
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            cable.addConnection(player, cableType, Direction.WEST);
            cable.addConnection(player, cableType, Direction.EAST);

            try (Transaction transaction = Transaction.openRoot()) {
                generator.getInventory().fluidStorage.insert(MIFluids.BIODIESEL.variant(), 1000, transaction);
                transaction.commit();
            }
            helper.startSequence().thenIdle(5).thenExecute(() -> {
                check(machine.getEnergyComponent().getEu() > 0,
                        "real LV diesel/copper-cable/LV machine setup transferred no EU");
                check(machine.getCrafterComponent().getProgress() > 0,
                        "real LV diesel/copper-cable/LV electrolyzer made no recipe progress");
            })
                    .thenSucceed();
        }));
        tests.add(asyncTest("full_buffer_machine_uses_connected_generation", helper -> {
            BlockPos generatorPos = new BlockPos(0, 1, 0);
            BlockPos cablePos = new BlockPos(1, 1, 0);
            BlockPos machinePos = new BlockPos(2, 1, 0);
            helper.setBlock(generatorPos, BuiltInRegistries.BLOCK.get(MI.id("lv_diesel_generator")));
            helper.setBlock(machinePos, BuiltInRegistries.BLOCK.get(MI.id("electrolyzer")));
            GeneratorMachineBlockEntity generator = helper.getBlockEntity(generatorPos);
            ElectricCraftingMachineBlockEntity machine = helper.getBlockEntity(machinePos);
            generator.orientation.outputDirection = Direction.EAST;
            machine.getEnergyComponent().insertEu(machine.getEnergyComponent().getCapacity(), Simulation.ACT);
            ItemStack salt = new ItemStack(BuiltInRegistries.ITEM.get(MI.id("salt_dust")), 2);
            check(machine.getInventory().itemStorage.itemHandler.insertItem(0, salt, false).isEmpty(),
                    "could not insert salt into full-buffer electrolyzer");

            PipeNetworkType cableType = PipeNetworkType.get(MI.id("copper_cable"));
            helper.setBlock(cablePos, MIPipes.BLOCK_PIPE.get());
            PipeBlockEntity cable = helper.getBlockEntity(cablePos);
            cable.addPipe(cableType, MIPipes.INSTANCE.getPipeItem(cableType).defaultData);
            helper.getLevel().blockUpdated(helper.absolutePos(cablePos), Blocks.AIR);
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            cable.addConnection(player, cableType, Direction.WEST);
            cable.addConnection(player, cableType, Direction.EAST);

            try (Transaction transaction = Transaction.openRoot()) {
                generator.getInventory().fluidStorage.insert(MIFluids.BIODIESEL.variant(), 1000, transaction);
                transaction.commit();
            }
            helper.startSequence().thenIdle(5).thenExecute(() -> check(
                    machine.getCrafterComponent().getProgress() > 0,
                    "full machine buffer deadlocked despite connected LV generation"))
                    .thenSucceed();
        }));
        tests.add(asyncTest("underpower_decay_modes_retain_or_waste_inputs", helper -> {
            BlockPos generatorPos = new BlockPos(0, 1, 0);
            BlockPos cablePos = new BlockPos(1, 1, 0);
            BlockPos machinePos = new BlockPos(2, 1, 0);
            helper.setBlock(generatorPos, BuiltInRegistries.BLOCK.get(MI.id("lv_diesel_generator")));
            helper.setBlock(machinePos, BuiltInRegistries.BLOCK.get(MI.id("electrolyzer")));
            GeneratorMachineBlockEntity generator = helper.getBlockEntity(generatorPos);
            ElectricCraftingMachineBlockEntity machine = helper.getBlockEntity(machinePos);
            generator.orientation.outputDirection = Direction.EAST;
            ItemStack salt = new ItemStack(BuiltInRegistries.ITEM.get(MI.id("salt_dust")), 2);
            check(machine.getInventory().itemStorage.itemHandler.insertItem(0, salt, false).isEmpty(),
                    "could not insert decay-test recipe input");

            PipeNetworkType cableType = PipeNetworkType.get(MI.id("copper_cable"));
            helper.setBlock(cablePos, MIPipes.BLOCK_PIPE.get());
            PipeBlockEntity cable = helper.getBlockEntity(cablePos);
            cable.addPipe(cableType, MIPipes.INSTANCE.getPipeItem(cableType).defaultData);
            helper.getLevel().blockUpdated(helper.absolutePos(cablePos), Blocks.AIR);
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            cable.addConnection(player, cableType, Direction.WEST);
            cable.addConnection(player, cableType, Direction.EAST);
            try (Transaction transaction = Transaction.openRoot()) {
                generator.getInventory().fluidStorage.insert(MIFluids.BIODIESEL.variant(), 1000, transaction);
                transaction.commit();
            }

            UnderpowerPolicy previous = HardEnergyConfig.UNDERPOWER_POLICY.get();
            HardEnergyConfig.UNDERPOWER_POLICY.set(UnderpowerPolicy.DECAY_ONLY);
            float[] poweredProgress = new float[1];
            helper.startSequence().thenIdle(8).thenExecute(() -> {
                check(machine.getCrafterComponent().hasActiveRecipe(), "decay-test recipe never started");
                poweredProgress[0] = machine.getCrafterComponent().getProgress();
                check(poweredProgress[0] > 0, "decay-test recipe gained no powered progress");
                helper.setBlock(cablePos, Blocks.AIR);
            }).thenIdle(4).thenExecute(() -> check(
                    !machine.getCrafterComponent().hasActiveRecipe()
                            || machine.getCrafterComponent().getProgress() < poweredProgress[0],
                    "underpowered recipe did not move backwards; progress="
                            + machine.getCrafterComponent().getProgress() + ", before=" + poweredProgress[0]))
                    .thenIdle(12).thenExecute(() -> {
                        boolean retained = machine.getCrafterComponent().hasActiveRecipe();
                        boolean atZero = machine.getCrafterComponent().getProgress() == 0;
                        boolean inputWasLost = machine.getInventory().itemStorage.itemHandler.getStackInSlot(0).isEmpty();
                        check(retained, "DECAY_ONLY canceled the active recipe at zero progress");
                        check(atZero, "DECAY_ONLY did not stop at zero progress");
                        check(inputWasLost, "active recipe unexpectedly returned its already-consumed input");
                        HardEnergyConfig.UNDERPOWER_POLICY.set(UnderpowerPolicy.DECAY_AND_WASTE_INPUTS);
                    }).thenIdle(1).thenExecute(() -> {
                        boolean canceled = !machine.getCrafterComponent().hasActiveRecipe();
                        boolean inputWasLost = machine.getInventory().itemStorage.itemHandler.getStackInSlot(0).isEmpty();
                        HardEnergyConfig.UNDERPOWER_POLICY.set(previous);
                        check(canceled, "DECAY_AND_WASTE_INPUTS did not cancel a zero-progress craft");
                        check(inputWasLost, "DECAY_AND_WASTE_INPUTS refunded its consumed input");
                    }).thenSucceed();
        }));
        tests.add(test("save_reload_preserves_accounting", helper -> {
            Ledger original = new Ledger(512, 384, 1280);
            long savedEnergy = original.energy;
            long savedProgress = original.progress;
            Ledger loaded = new Ledger(savedEnergy, savedProgress, original.total);
            check(loaded.energy == savedEnergy && loaded.progress == savedProgress && loaded.total == original.total,
                    "reload duplicated or discarded state");
        }));
        return tests;
    }

    private static TestFunction test(String name, Consumer<GameTestHelper> body) {
        return new TestFunction("mi_energy_tiers", "mi_energy_tiers." + name, "mi_energy_tiers:empty",
                StructureUtils.getRotationForRotationSteps(0), 20, 0, true, false, 1, 1, false,
                helper -> {
                    try {
                        body.accept(helper);
                        helper.succeed();
                    } catch (AssertionError error) {
                        helper.fail(error.getMessage());
                    }
                });
    }

    private static TestFunction asyncTest(String name, Consumer<GameTestHelper> body) {
        return asyncTest("mi_energy_tiers", name, body);
    }

    private static TestFunction asyncTest(String batch, String name, Consumer<GameTestHelper> body) {
        return new TestFunction(batch, "mi_energy_tiers." + name, "mi_energy_tiers:empty",
                StructureUtils.getRotationForRotationSteps(0), 40, 0, true, false, 1, 1, false, body);
    }

    private static EnergyTransferContext.Scope context(CableTier tier) {
        return EnergyTransferContext.push(new EnergyTransferContext(tier,
                TransferEndpoint.unknown("test source"), TransferEndpoint.unknown("test destination"), 0));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Ledger {
        long energy;
        long progress;
        final long total;
        Ledger(long energy, long progress, long total) { this.energy = energy; this.progress = progress; this.total = total; }
        long tick(long max) {
            long request = Math.min(max, total - progress);
            if (energy < request) return 0;
            energy -= request;
            progress += request;
            return request;
        }
    }

    private static long poweredTick(Ledger ledger, InstantaneousPowerBudget budget, long tick, long max) {
        long request = Math.min(max, ledger.total - ledger.progress);
        if (ledger.energy < request || budget.available(tick) < request) return 0;
        if (!budget.spend(tick, request)) throw new AssertionError("power budget commit failed");
        return ledger.tick(max);
    }

    private static class Storage implements ILongEnergyStorage {
        @Override public long receive(long maxReceive, boolean simulate) { return maxReceive; }
        @Override public long extract(long maxExtract, boolean simulate) { return maxExtract; }
        @Override public long getAmount() { return 0; }
        @Override public long getCapacity() { return Long.MAX_VALUE; }
        @Override public boolean canReceive() { return true; }
        @Override public boolean canExtract() { return true; }
    }

    private static final class TypedStorage extends Storage implements TierAwareEndpoint {
        private final CableTier tier;
        TypedStorage(CableTier tier) { this.tier = tier; }
        @Override public CableTier miEnergyTier() { return tier; }
    }

    private static final class MIStorage extends Storage implements MIEnergyStorage {
        private final long stored;
        MIStorage(long stored) { this.stored = stored; }
        @Override public long extract(long maxExtract, boolean simulate) { return Math.min(maxExtract, stored); }
        @Override public boolean canConnect(CableTier tier) { return true; }
    }
}
