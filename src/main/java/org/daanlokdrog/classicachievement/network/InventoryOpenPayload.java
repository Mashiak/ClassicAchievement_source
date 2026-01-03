package org.daanlokdrog.classicachievement.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record InventoryOpenPayload() implements CustomPacketPayload {
    public static final Type<InventoryOpenPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("classicachievement", "inventory_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryOpenPayload> CODEC = StreamCodec.unit(new InventoryOpenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}