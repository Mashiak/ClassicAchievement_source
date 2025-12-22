package net.daanlokdrog.classicachievement.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "init", at = @At("HEAD"))
    private void nostalgicSupplement$sendInventoryOpenPacket(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        
        if (minecraft.player != null && minecraft.getConnection() != null) {
            
            net.daanlokdrog.classicachievement.ClassicAchievementMod.PACKET_HANDLER.sendToServer(
                new net.daanlokdrog.classicachievement.network.InventoryOpenPacket()
            );
        }
    }
}