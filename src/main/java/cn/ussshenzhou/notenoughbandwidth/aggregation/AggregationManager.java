package cn.ussshenzhou.notenoughbandwidth.aggregation;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthPlugin;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * 数据包聚合管理器
 * 负责缓存数据包并以固定间隔批量发送
 *
 * @author USS_Shenzhou
 */
public class AggregationManager {

    private static final WeakHashMap<Connection, ArrayList<AggregatedEncodePacket>> PACKET_BUFFER = new WeakHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "NEB-Flush-thread");
                t.setDaemon(true);
                return t;
            });
    private static final ArrayList<ScheduledFuture<?>> TASKS = new ArrayList<>();
    private static volatile boolean initialized = false;

    /**
     * 初始化聚合管理器
     */
    public synchronized static void init() {
        if (initialized) {
            return;
        }
        PACKET_BUFFER.clear();
        TASKS.forEach(task -> task.cancel(false));
        TASKS.clear();
        TASKS.add(TIMER.scheduleAtFixedRate(
                AggregationManager::flush,
                0,
                AggregationFlushHelper.getFlushPeriodInMilliseconds(),
                TimeUnit.MILLISECONDS));
        initialized = true;
    }

    /**
     * 关闭聚合管理器
     */
    public synchronized static void shutdown() {
        initialized = false;
        TASKS.forEach(task -> task.cancel(false));
        TASKS.clear();
        PACKET_BUFFER.clear();
        TIMER.shutdown();
    }

    /**
     * 接管数据包 - 将数据包加入缓冲区而不是直接发送
     *
     * @param packet     数据包
     * @param connection Netty连接
     * @return true 如果数据包被接管（已缓冲），false则正常发送
     */
    public synchronized static boolean takeOver(Packet<?> packet, Connection connection) {
        if (!initialized)
            return false;

        // 获取数据包类型标识符
        Identifier type = PacketTypeUtil.getPacketType(packet);
        if (type == null)
            return false;

        // 检查是否跳过聚合
        if (NotEnoughBandwidthConfig.skipType(type.toString())) {
            flushConnection(connection);
            return false;
        }

        // 加入缓冲区
        PACKET_BUFFER.computeIfAbsent(connection, k -> new ArrayList<>())
                .add(new AggregatedEncodePacket(packet, type));
        return true;
    }

    /**
     * 刷新指定连接上的缓冲数据包
     */
    public synchronized static void flushConnection(Connection connection) {
        TIMER.execute(() -> {
            PACKET_BUFFER.entrySet().removeIf(e -> !isConnected(e.getKey()));
            flushInternal(connection, PACKET_BUFFER.get(connection));
        });
    }

    /**
     * 定时刷新所有连接
     */
    private synchronized static void flush() {
        PACKET_BUFFER.entrySet().removeIf(e -> !isConnected(e.getKey()));
        PACKET_BUFFER.forEach(AggregationManager::flushInternal);
    }

    /**
     * 刷新指定连接的缓冲数据包
     */
    private synchronized static void flushInternal(Connection connection, ArrayList<AggregatedEncodePacket> packets) {
        try {
            if (packets == null || packets.isEmpty()) {
                return;
            }

            var sendPackets = new ArrayList<>(packets);
            Object flow = getSending(connection);

            // 创建聚合包
            PacketAggregationPacket aggPacket = new PacketAggregationPacket(sendPackets, connection);

            // 通过CustomPayload发送
            Packet<?> customPayload;
            if (isClientbound(flow)) {
                customPayload = new ClientboundCustomPayloadPacket(aggPacket);
            } else {
                customPayload = new ServerboundCustomPayloadPacket(aggPacket);
            }

            doSend(connection, customPayload);

            packets.clear();
        } catch (Exception e) {
            if (NotEnoughBandwidthPlugin.getInstance().getNebConfig().debugLog) {
                NotEnoughBandwidthPlugin.getInstance().getLogger()
                        .log(Level.WARNING, "跳过: 刷新数据包失败", e);
            }
        }
    }

    // --- Reflection helpers for Connection field access (Paper 1.21.11 compat) ---

    private static boolean isConnected(Connection conn) {
        try {
            return (boolean) conn.getClass().getMethod("isConnected").invoke(conn);
        } catch (Exception e) {
            return false;
        }
    }

    private static Object getSending(Connection conn) {
        try {
            try {
                Field f = Connection.class.getDeclaredField("sending");
                f.setAccessible(true);
                return f.get(conn);
            } catch (NoSuchFieldException e2) {
                return conn.getClass().getMethod("getSending").invoke(conn);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isClientbound(Object flow) {
        return flow != null && flow.toString().contains("CLIENTBOUND");
    }

    private static void doSend(Connection conn, Packet<?> packet) {
        try {
            conn.getClass().getMethod("send", Packet.class).invoke(conn, packet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
