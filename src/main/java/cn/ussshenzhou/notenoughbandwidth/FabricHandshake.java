package cn.ussshenzhou.notenoughbandwidth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the Fabric NEB handshake so the Fabric client knows the server
 * supports NEB protocol (zstd compression, aggregated packets).
 * <p>
 * Fabric handshake flow:
 * <ol>
 * <li>Server sends DictionarySyncPayload (empty = no dictionary yet)</li>
 * <li>Server sends IndexSyncPayload (type index table)</li>
 * <li>Client sends NebAckPayload to confirm NEB capability</li>
 * <li>Server marks connection ready; aggregation + compression enabled</li>
 * </ol>
 */
public class FabricHandshake {

    private static final String IDX_SYNC = "index_sync";
    private static final String DICT_SYNC = "dictionary_sync";
    private static final String ACK = "ack";
    private static final String NEB_NS = "neb";

    /** Connections that have completed the NEB handshake. */
    private static final Set<Channel> nebEnabled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static NotEnoughBandwidthPlugin plugin;
    private static Field protocolField;

    static {
        try {
            protocolField = ServerGamePacketListenerImpl.class.getDeclaredField("protocol");
            protocolField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }
    }

    public static void init(NotEnoughBandwidthPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Called when a player joins (after Netty injection).
     * Sends the NEB handshake payloads so the Fabric client knows NEB is supported.
     */
    public static void sendHandshake(ServerGamePacketListenerImpl listener, Channel channel) {
        // 1. Send dictionary sync (empty — no dictionary yet)
        sendCustomPayload(channel, NEB_NS, DICT_SYNC, buf -> {
            buf.writeVarInt(0); // dictionary length = 0 (no dictionary)
        });

        // 2. Send index sync with empty type list + server UUID
        String serverId = plugin.getNebConfig().serverUUID;
        if (serverId == null || serverId.isEmpty()) {
            serverId = UUID.randomUUID().toString();
            plugin.getNebConfig().serverUUID = serverId;
            plugin.saveConfigToFile();
        }
        final String uuid = serverId;
        sendCustomPayload(channel, NEB_NS, IDX_SYNC, buf -> {
            buf.writeVarInt(0); // 0 types in the index
            buf.writeUtf(uuid); // server UUID for chunk cache namespacing
        });

        if (plugin.getNebConfig().debugLog) {
            plugin.getLogger().info("Sent NEB handshake to Fabric client (serverId=" + uuid + ")");
        }
    }

    /**
     * Called from the Netty pipeline when a ServerboundCustomPayloadPacket is
     * received. Checks if it's a NEB ack.
     */
    public static boolean handleIncoming(ServerboundCustomPayloadPacket packet, Channel channel) {
        CustomPacketPayload payload = packet.payload();
        Identifier id = payload.type().id();
        if (!NEB_NS.equals(id.namespace()))
            return false;

        if (ACK.equals(id.path())) {
            nebEnabled.add(channel);
            if (plugin.getNebConfig().debugLog) {
                plugin.getLogger().info("Received NEB ack from Fabric client — compression enabled");
            }
            return true;
        }
        return false;
    }

    public static boolean isNebEnabled(Channel channel) {
        return nebEnabled.contains(channel);
    }

    public static void removeChannel(Channel channel) {
        nebEnabled.remove(channel);
    }

    /** Write a custom payload packet and send it through the Netty channel. */
    private static void sendCustomPayload(Channel channel, String namespace, String path,
            java.util.function.Consumer<FriendlyByteBuf> writer) {
        try {
            // Prepare the packet payload using FriendlyByteBuf
            FriendlyByteBuf payloadBuf = new FriendlyByteBuf(Unpooled.buffer());
            writer.accept(payloadBuf);

            // Build the identifier and payload wrapper
            Identifier id = Identifier.of(namespace, path);
            // We send as a raw ClientboundCustomPayloadPacket with the payload data
            // wrapped in a generic custom payload
            var packet = new ClientboundCustomPayloadPacket(
                    new cn.ussshenzhou.notenoughbandwidth.GenericPayload(id, payloadBuf));
            channel.writeAndFlush(packet, channel.voidPromise());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send NEB handshake packet: " + e.getMessage());
        }
    }

    /**
     * Minimal CustomPacketPayload wrapper using only Identifier + raw bytes.
     * This avoids needing Fabric-specific classes on the Spigot classpath.
     */
    public static class GenericPayload implements CustomPacketPayload {
        private final Identifier id;
        private final FriendlyByteBuf data;

        public GenericPayload(Identifier id, FriendlyByteBuf data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return new Type<>(id);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBytes(data, data.readerIndex(), data.readableBytes());
            data.release();
        }
    }
}
