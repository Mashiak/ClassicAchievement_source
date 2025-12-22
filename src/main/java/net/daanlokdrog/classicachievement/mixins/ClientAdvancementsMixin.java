package net.daanlokdrog.classicachievement.mixins;

import net.minecraft.advancements.Advancement;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientAdvancements.class)
public class ClientAdvancementsMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
    }
}