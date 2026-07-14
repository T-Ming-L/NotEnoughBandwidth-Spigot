package cn.ussshenzhou.notenoughbandwidth.aggregation;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 待编码的聚合子数据包
 *
 * @author USS_Shenzhou
 */
public class AggregatedEncodePacket {
    public final Identifier type;
    private final boolean isMinecraft;
    private final Packet<?> packet;
    private final CustomPacketPayload payload;

    public AggregatedEncodePacket(Packet<?> p, Identifier type) {
        if (p instanceof ServerboundCustomPayloadPacket pld) {
            this.isMinecraft = false;
            this.packet = null;
            this.payload = pld.payload();
        } else if (p instanceof ClientboundCustomPayloadPacket pld) {
            this.isMinecraft = false;
            this.packet = null;
            this.payload = pld.payload();
        } else {
            this.isMinecraft = true;
            this.packet = p;
            this.payload = null;
        }
        this.type = type;
    }

    public boolean isMinecraft() {
        return isMinecraft;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public CustomPacketPayload getPayload() {
        return payload;
    }
}
