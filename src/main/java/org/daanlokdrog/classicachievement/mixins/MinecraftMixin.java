package org.daanlokdrog.classicachievement.mixins;

import org.daanlokdrog.classicachievement.screens.LegacyAchievementScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen replaceAdvancementsScreen(Screen screen) {
        if (screen instanceof AdvancementsScreen && !(screen instanceof LegacyAchievementScreen)) {
            Minecraft mc = Minecraft.getInstance();
            ClientPacketListener connection = mc.getConnection();

            if (connection != null) {
                return new LegacyAchievementScreen(connection.getAdvancements());
            }
        }
        return screen;
    }
}