package net.daanlokdrog.classicachievement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.daanlokdrog.classicachievement.LegacyAchievementScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "classic_achievement", value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof AdvancementsScreen && !(event.getScreen() instanceof LegacyAchievementScreen)) {
            Minecraft mc = Minecraft.getInstance();
            ClientPacketListener connection = mc.getConnection();
            if (connection != null) {
                event.setCanceled(true);
                mc.tell(() -> {
                    mc.setScreen(new LegacyAchievementScreen(connection.getAdvancements()));
                });
            }
        }
    }
}