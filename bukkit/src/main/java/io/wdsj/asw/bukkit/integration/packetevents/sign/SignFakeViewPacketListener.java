package io.wdsj.asw.bukkit.integration.packetevents.sign;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityType;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SignFakeViewPacketListener extends PacketListenerAbstract {
    private static PacketListenerAbstract registeredListener;

    public static void register() {
        if (registeredListener != null) {
            return;
        }
        registeredListener = new SignFakeViewPacketListener();
        PacketEvents.getAPI().getEventManager().registerListener(registeredListener);
    }

    public static void unregister() {
        if (registeredListener == null) {
            return;
        }
        PacketEvents.getAPI().getEventManager().unregisterListener(registeredListener);
        registeredListener = null;
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (!SignFakeViewService.isOperational()) {
            return;
        }
        Object eventPlayer = event.getPlayer();
        if (!(eventPlayer instanceof Player player)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_ENTITY_DATA) {
            handleBlockEntityData(event, player);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            handleChunkData(event, player);
        }
    }

    private void handleBlockEntityData(PacketSendEvent event, Player player) {
        WrapperPlayServerBlockEntityData packet = new WrapperPlayServerBlockEntityData(event);
        if (!isSignType(packet.getBlockEntityType())) {
            return;
        }

        World world = player.getWorld();
        Vector3i position = packet.getPosition();
        SignFakeViewService.enqueueRefresh(player, world, List.of(new BlockKey(
                world.getUID(),
                position.getX(),
                position.getY(),
                position.getZ()
        )));
    }

    private void handleChunkData(PacketSendEvent event, Player player) {
        WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
        Column column = packet.getColumn();
        TileEntity[] tileEntities = column.getTileEntities();
        if (tileEntities.length == 0) {
            return;
        }

        int baseX = column.getX() << 4;
        int baseZ = column.getZ() << 4;
        World world = player.getWorld();
        UUID worldId = world.getUID();
        List<BlockKey> candidateKeys = new ArrayList<>(0);
        ClientVersion clientVersion = event.getUser().getClientVersion();
        for (TileEntity tileEntity : tileEntities) {
            BlockEntityType type = BlockEntityTypes.getById(clientVersion, tileEntity.getType());
            if (!isSignType(type)) {
                continue;
            }
            candidateKeys.add(new BlockKey(
                    worldId,
                    baseX + tileEntity.getX(),
                    tileEntity.getY(),
                    baseZ + tileEntity.getZ()
            ));
        }
        if (candidateKeys.isEmpty()) {
            return;
        }

        SignFakeViewService.enqueueRefresh(player, world, candidateKeys);
    }

    private boolean isSignType(BlockEntityType type) {
        return type == BlockEntityTypes.SIGN || type == BlockEntityTypes.HANGING_SIGN;
    }
}
