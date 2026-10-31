package dev.skinoverride.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server to nearby clients: "this player (uuid) looks like this". */
public record CosmeticsDownPayload(String uuid, String data) implements CustomPayload {

    public static final CustomPayload.Id<CosmeticsDownPayload> ID =
            new CustomPayload.Id<>(Identifier.of("skinoverride", "cosmetics_down"));

    public static final PacketCodec<RegistryByteBuf, CosmeticsDownPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, CosmeticsDownPayload::uuid,
                    PacketCodecs.STRING, CosmeticsDownPayload::data, CosmeticsDownPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
