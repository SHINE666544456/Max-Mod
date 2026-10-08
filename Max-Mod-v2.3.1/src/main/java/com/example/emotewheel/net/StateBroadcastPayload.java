package com.example.emotewheel.net;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> nearby clients: "this player's state changed". */
public record StateBroadcastPayload(UUID player, String state, boolean on) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StateBroadcastPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("emote_wheel", "state_broadcast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StateBroadcastPayload> CODEC =
        StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, StateBroadcastPayload::player,
            ByteBufCodecs.stringUtf8(32), StateBroadcastPayload::state,
            ByteBufCodecs.BOOL, StateBroadcastPayload::on,
            StateBroadcastPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
