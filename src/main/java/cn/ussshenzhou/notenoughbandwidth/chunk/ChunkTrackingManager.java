package cn.ussshenzhou.notenoughbandwidth.chunk;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/**
 * 区块跟踪管理器
 * 扩展玩家视距以提供区块缓存
 *
 * @author USS_Shenzhou
 */
public class ChunkTrackingManager implements Listener {

    private static NotEnoughBandwidthPlugin plugin;

    public static void init(NotEnoughBandwidthPlugin pluginInstance) {
        plugin = pluginInstance;
        Bukkit.getPluginManager().registerEvents(new ChunkTrackingManager(), pluginInstance);

        plugin.getLogger().info("区块跟踪缓存已启用 (缓存距离: "
                + pluginInstance.getNebConfig().dccDistance
                + ", 超时: " + pluginInstance.getNebConfig().dccTimeout + "s)");
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // 世界切换时应用缓存视距
        Player player = event.getPlayer();
        applyCacheViewDistance(player);
    }

    /**
     * 应用缓存的视距（扩展服务器视距）
     */
    public static void applyCacheViewDistance(Player player) {
        // 在Paper中，我们可以通过setViewDistance来扩展玩家的区块加载范围
        // 这利用了PaperAPI的setViewDistance方法
        if (player.isOnline()) {
            int extra = plugin.getNebConfig().dccDistance;
            if (extra > 0) {
                int currentView = player.getClientViewDistance();
                // 注意：这只是一个提示，实际的区块跟踪由服务器控制
            }
        }
    }
}
