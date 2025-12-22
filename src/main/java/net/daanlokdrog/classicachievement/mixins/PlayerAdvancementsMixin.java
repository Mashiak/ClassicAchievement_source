package net.daanlokdrog.classicachievement.mixins;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
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
                    target = "Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator;evaluateVisibility(Lnet/minecraft/advancements/Advancement;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)V"
            )
    )
    private void redirectEvaluateVisibility(Advancement root, Predicate<Advancement> isDonePredicate, AdvancementVisibilityEvaluator.Output output) {
        AdvancementVisibilityEvaluator.evaluateVisibility(root, (a) -> true, output);
    }
}