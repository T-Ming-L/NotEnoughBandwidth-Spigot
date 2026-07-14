package cn.ussshenzhou.notenoughbandwidth;

import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * 插件配置类
 *
 * @author USS_Shenzhou
 */
public class NotEnoughBandwidthConfig {

    // [兼容模式] 启用后使用较保守的压缩策略，避免与部分客户端/服务端MOD不兼容
    public boolean compatibleMode = false;

    // [数据包黑名单] 不参与聚合与压缩的数据包类型（Minecraft 协议名称）
    public HashSet<String> blackList = new HashSet<>() {
        {
        
                add("minecraft:command_suggestio
                add("minecraft:command_suggestion
                add("minecraft:command
                add("minecraft:chat_comman
                add("minecraft:chat_command_signe
                add("minecraft:player_info_updat
                add("minecraft:player_info_remov
         
           add("minecraft:keep_alive");
        }
    };

    // [调试日志] 开启后将输出详细的数据包聚合与压缩日志，用于排查问题
    public boolean debugLog = false;

    // [压缩级别] Zstd 压缩级别（21~25），数值越高压缩率越大但更消耗CPU，默认23
        
            ic int contextLevel = 23;
        
    
    // [DCC触发大小] 数据包大小超过此值（KB）时启用字典压缩（Delta Context Compression）
    public int dccSizeLimit = 60;

    // [DCC窗口距离] 向前回溯多少个数据包作为字典压缩的参考窗口
        
            ic int dccDistance = 5;
            
            DCC超时] 字典缓存的有效时间（秒），超过此
            ic int dccTimeout = 60;
            
            最大数据包大小] 单个聚合包的最大体积，超过此大小会拆分发送（支持 B / KB / MB
            ic String maxPacketSize = "4MB";
            
            服务器UUID] 用于Fabric客户端识别服务器 (chunk cache n
            ic String serverUUID = "";
        
    
    // [免上下文压缩玩家] 列表中的玩家UUID将不使用上下文压缩（DCC），仅使用普通Zstd压缩
    public HashSet<String> playersDoNotUseContext = new HashSet<>() {
        {
            add("00000000-0000-0000-0000-000000000000");
        }
    };

    // [内置黑名单] 登录/握手阶段的数据包始终不参与聚合（硬编码，不可配置）
    @Expose(serialize = false, deserialize = false)
    public static final HashSet<String> COMMON_BLACK_LIST = new HashSet<>() {
        {
            add("minecraft:finish_configuration");
            add("minecraft:login");
        }
    };

    // 将 maxPacketSize 字符串（如 "4MB"）解析为字节数
    public int getMaxPacketSizeBytes() {
        return parseByteSize(maxPacketSize);
    }

    // 获取有效压缩级别，限制在 21~25 范围内
    public int getContextLevel() {
        return Math.max(21, Math.min(25, contextLevel));
    }

    // 解析带单位的大小字符串（B/KB/MB）为字节数
    private static int parseByteSize(String s) {
        var matcher = Pattern.compile("^([\\d.]+)\\s*(B|KB|MB)?$", Pattern.CASE_INSENSITIVE).matcher(s.trim());
        if (!matcher.matches()) {
            return parseByteSize("4MB");
        }
        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);
        if (unit == null || "B".equalsIgnoreCase(unit)) {
            return (int) value;
        }
        return (int) switch (unit.toUpperCase()) {
            case "KB" -> value * 1024;
            case "MB" -> value * 1024 * 1024;
            default -> parseByteSize("4MB");
        };
    }

    public static boolean skipType(String type) {
        return COMMON_BLACK_LIST.contains(type);
    }
}
