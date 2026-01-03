package org.daanlokdrog.classicachievement.mixins;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {

    @Redirect(
            method = "updateTreeVisibility",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator;evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)V"
            )
    )
    private void redirectEvaluateVisibility(AdvancementNode root, Predicate<AdvancementNode> isDonePredicate, AdvancementVisibilityEvaluator.Output output) {
        AdvancementVisibilityEvaluator.evaluateVisibility(root, (node) -> true, output);
    }
}