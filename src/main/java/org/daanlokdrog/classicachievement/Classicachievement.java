package org.daanlokdrog.classicachievement;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.daanlokdrog.classicachievement.network.InventoryOpenPayload;
import org.daanlokdrog.classicachievement.network.InventoryOpenHandler;

public class Classicachievement implements ModInitializer {

    @Override
    public void onInitialize() {
        ClassicAchievementConfig.load();
        PayloadTypeRegistry.playC2S().register(InventoryOpenPayload.ID, InventoryOpenPayload.CODEC);
        InventoryOpenHandler.registerReceiver();
    }
}
