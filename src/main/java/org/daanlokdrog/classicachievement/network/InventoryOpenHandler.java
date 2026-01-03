package org.daanlokdrog.classicachievement.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class InventoryOpenHandler {
    private static final ResourceLocation ROOT_ID = ResourceLocation.withDefaultNamespace("story/root");

    public static void registerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(InventoryOpenPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                AdvancementHolder advancement = player.server.getAdvancements().get(ROOT_ID);
                if (advancement != null) {
                    AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                    if (!progress.isDone()) {
                        for (String criterion : progress.getRemainingCriteria()) {
                            player.getAdvancements().award(advancement, criterion);
                        }
                    }
                }
            });
        });
    }
}