package cn.ussshenzhou.notenoughbandwidth.aggregation;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 数据包类型工具类
 *
 * @author USS_Shenzhou
 */
public class PacketTypeUtil {

    /**
     * 获取数据包的真正类型标识符
     */
    public static Identifier getPacketType(Packet<?> packet) {
        if (packet instanceof ServerboundCustomPayloadPacket payload) {
            return payload.payload().type().id();
        } else if (packet instanceof ClientboundCustomPayloadPacket payload) {
            return payload.payload().type().id();
        } else {
            return packet.type().id();
        }
    }

    /**
     * 获取数据包的真实内容
     */
    public static Object getTruePacket(Packet<?> packet) {
        if (packet instanceof ServerboundCustomPayloadPacket payload) {
            return payload.payload();
        } else if (packet instanceof ClientboundCustomPayloadPacket payload) {
            return payload.payload();
        } else {
            return packet;
        }
    }
}
