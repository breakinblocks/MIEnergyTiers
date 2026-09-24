package com.breakinblocks.mienergytiers.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Keeps the optional-target mixins out of the way when the mod they target at is not installed.
 
 */
public final class MIEnergyTiersMixinPlugin implements IMixinConfigPlugin {
    private static final String TESSERACT_PACKAGE = "com.breakinblocks.mienergytiers.mixin.tesseract.";
    private static final String TESSERACT_MOD_ID = "tesseract_api";

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
            tesseractPresent = LoadingModList.get().getModFileById(TESSERACT_MOD_ID) != null;
        }
        return tesseractPresent;
    }
}
