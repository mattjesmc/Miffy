package com.mattmc.nijntje.menu;

import com.mattmc.nijntje.Nijntje;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C hint carrying the Nijntje's entity id, sent just before the menu-open packet so the
 * client menu factory can resolve which bunny's inventory to build (this Fabric build has no
 * ExtendedScreenHandlerType).
 */
public record OpenNijntjeMenuPayload(int entityId) implements CustomPacketPayload {
    public static final Type<OpenNijntjeMenuPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "open_nijntje_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNijntjeMenuPayload> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenNijntjeMenuPayload::entityId, OpenNijntjeMenuPayload::new);

    @Override
    public Type<OpenNijntjeMenuPayload> type() {
        return TYPE;
    }
}
