package com.example.emotewheel.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: "switch this state on/off for me" (shadow clones, cat-girl transformation). */
public record StatePayload(String state, boolean on) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StatePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("emote_wheel", "state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), StatePayload::state,
            ByteBufCodecs.BOOL, StatePayload::on,
            StatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
