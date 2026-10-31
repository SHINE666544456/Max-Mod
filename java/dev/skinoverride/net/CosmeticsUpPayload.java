package dev.skinoverride.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client to server: "this is my cape, wings and Weapon Master loadout". */
public record CosmeticsUpPayload(String data) implements CustomPayload {

    public static final CustomPayload.Id<CosmeticsUpPayload> ID =
            new CustomPayload.Id<>(Identifier.of("skinoverride", "cosmetics_up"));

    public static final PacketCodec<RegistryByteBuf, CosmeticsUpPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, CosmeticsUpPayload::data, CosmeticsUpPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
