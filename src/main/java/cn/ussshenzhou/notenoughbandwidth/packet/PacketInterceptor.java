package cn.ussshenzhou.notenoughbandwidth.packet;

import cn.ussshenzhou.notenoughbandwidth.FabricHandshake;
import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthPlugin;
import cn.ussshenzhou.notenoughbandwidth.aggregation.AggregationManager;
import io.netty.channel.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 数据包拦截器 - 通过Netty管道注入实现数据包聚合
 *
 * <p>
 * 使用Netty ChannelDuplexHandler在数据包编解码阶段进行拦截，
 * 将小数据包缓存到缓冲区，定期合并为一个压缩的大数据包发送。
 * 同时处理Fabric NEB握手协议。
 * </p>
 *
 * @author USS_Shenzhou
 */
public class PacketInterceptor {

    private static final String NEB_HANDLER_NAME = "neb_packet_handler";
    private static NotEnoughBandwidthPlugin plugin;
    private static Field connectionField;
    private static Field channelField;
    private static Field protocolField;
    private static Field isConnectedField;
    private static Field sendingField;

    static {
        try {
            connectionField = ServerGamePacketListenerImpl.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            channelField = Connection.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            try {
                protocolField = Connection.class.getDeclaredField("protocol");
            } catch (NoSuchFieldException e2) {
                // Older mapping might use getProtocol() method
                protocolField = null;
            }
            if (protocolField != null)
                protocolField.setAccessible(true);
            try {
                isConnectedField = Connection.class.getDeclaredField("connected");
            } catch (NoSuchFieldException e3) {
                isConnectedField = null;
            }
            if (isConnectedField != null)
                isConnectedField.setAccessible(true);
            try {
                sendingField = Connection.class.getDeclaredField("sending");
                sendingField.setAccessible(true);
            } catch (NoSuchFieldException e4) {
                sendingField = null;
            }
        } catch (NoSuchFieldException e) {
            for (Field f : ServerGamePacketListenerImpl.class.getDeclaredFields()) {
                if (Connection.class.isAssignableFrom(f.getType())) {
                    connectionField = f;
                    connectionField.setAccessible(true);
                    break;
                }
            }
        }
    }

    public static void init(NotEnoughBandwidthPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * 向玩家的Netty管道注入NEB处理器
     */
    public static void injectPlayer(Player player) {
        if (!player.isOnline())
            return;

        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            if (!(handle instanceof ServerGamePacketListenerImpl listener))
                return;

            Connection connection = (Connection) connectionField.get(listener);
            if (connection == null || !isConnected(connection))
                return;

            Channel channel = getChannel(connection);
            if (channel == null)
                return;

            if (channel.pipeline().get(NEB_HANDLER_NAME) != null) {
                channel.pipeline().remove(NEB_HANDLER_NAME);
            }

            channel.pipeline().addBefore("packet_handler", NEB_HANDLER_NAME, new NebPacketHandler());

            // Send Fabric NEB handshake after a short delay (wait for PLAY protocol)
            ServerGamePacketListenerImpl listenerRef = listener;
            Channel channelRef = channel;
            new ScheduledThreadPoolExecutor(1).schedule(() -> {
                try {
                    FabricHandshake.sendHandshake(listenerRef, channelRef);
                } catch (Exception ignored) {
                }
            }, 500, TimeUnit.MILLISECONDS);

            if (plugin.getNebConfig().debugLog) {
                plugin.getLogger().info("已注入 " + player.getName() + " 的数据包处理器");
            }
        } catch (Exception e) {
            if (plugin.getNebConfig().debugLog) {
                plugin.getLogger().log(Level.WARNING, "无法注入 " + player.getName() + " 的数据包处理器", e);
            }
        }
    }

    /**
     * 移除玩家管道上的NEB处理器
     */
    public static void removePlayer(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            if (!(handle instanceof ServerGamePacketListenerImpl listener))
                return;

            Connection connection = (Connection) connectionField.get(listener);
            if (connection == null)
                return;

            Channel channel = getChannel(connection);
            if (channel != null) {
                FabricHandshake.removeChannel(channel);
                if (channel.pipeline().get(NEB_HANDLER_NAME) != null) {
                    channel.pipeline().remove(NEB_HANDLER_NAME);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 为所有在线玩家注入处理器
     */
    public static void injectAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            injectPlayer(player);
        }
    }

    public static void shutdown() {
        // 清理工作由玩家退出事件处理
    }

    private static boolean isConnected(Connection conn) {
        try {
            if (isConnectedField != null)
                return isConnectedField.getBoolean(conn);
            return (boolean) conn.getClass().getMethod("isConnected").invoke(conn);
        } catch (Exception e) {
            return false;
        }
    }

    private static Channel getChannel(Connection conn) {
        try {
            if (channelField != null)
                return (Channel) channelField.get(conn);
            return (Channel) conn.getClass().getMethod("channel").invoke(conn);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isPlayProtocol(Connection conn) {
        try {
            Object protocol = getProtocol(conn);
            // Compare name instead of enum reference for compatibility
            return protocol != null && protocol.toString().contains("PLAY");
        } catch (Exception e) {
            return false;
        }
    }

    private static Object getProtocol(Connection conn) {
        try {
            if (protocolField != null)
                return protocolField.get(conn);
            return conn.getClass().getMethod("getProtocol").invoke(conn);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * NEB Netty数据包处理器
     */
    private static class NebPacketHandler extends ChannelDuplexHandler {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof Packet<?> packet) {
                Connection connection = findConnection(ctx);
                if (connection != null && isPlayProtocol(connection)
                        && FabricHandshake.isNebEnabled(ctx.channel())) {
                    if (AggregationManager.takeOver(packet, connection)) {
                        return;
                    }
                }
            }
            super.write(ctx, msg, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // Handle Fabric NEB ack packet
            if (msg instanceof ServerboundCustomPayloadPacket payload) {
                FabricHandshake.handleIncoming(payload, ctx.channel());
            }
            super.channelRead(ctx, msg);
        }

        private Connection findConnection(ChannelHandlerContext ctx) {
            try {
                for (String name : ctx.pipeline().names()) {
                    Object handler = ctx.pipeline().get(name);
                    if (handler instanceof Connection conn) {
                        return conn;
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }
}
