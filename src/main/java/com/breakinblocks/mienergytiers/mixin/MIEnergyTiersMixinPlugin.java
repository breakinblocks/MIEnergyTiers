package com.breakinblocks.mienergytiers.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Keeps the optional-target mixins out of the way when the mod they aim at is not installed.
 *
 * Everything under {@code mixin.tesseract} targets classes shipped by the Tesseract API, which
 * arrives with MI Tweaks rather than with Modern Industrialization. Those mixins are declared with
 * the same {@code defaultRequire} as the rest, so without this they would fail the pack on any
 * install that does not carry Tesseract.
 */
public final class MIEnergyTiersMixinPlugin implements IMixinConfigPlugin {
    private static final String TESSERACT_PACKAGE = "com.breakinblocks.mienergytiers.mixin.tesseract.";
    private static final String TESSERACT_PROBE =
            "net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied."
                    + "AbstractElectricMultipliedCraftingMultiblockBlockEntity";

    private Boolean tesseractPresent;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(TESSERACT_PACKAGE)) {
            return tesseractPresent();
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) {
    }

    private boolean tesseractPresent() {
        if (tesseractPresent == null) {
            boolean found;
            try {
                Class.forName(TESSERACT_PROBE, false, MIEnergyTiersMixinPlugin.class.getClassLoader());
                found = true;
            } catch (ClassNotFoundException | LinkageError e) {
                found = false;
            }
            tesseractPresent = found;
        }
        return tesseractPresent;
    }
}
