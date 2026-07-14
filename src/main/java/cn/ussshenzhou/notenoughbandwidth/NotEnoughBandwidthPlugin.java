package cn.ussshenzhou.notenoughbandwidth;

import cn.ussshenzhou.notenoughbandwidth.aggregation.AggregationManager;
import cn.ussshenzhou.notenoughbandwidth.chunk.ChunkTrackingManager;
import cn.ussshenzhou.notenoughbandwidth.packet.PacketInterceptor;
import cn.ussshenzhou.notenoughbandwidth.stat.StatCommand;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * NotEnoughBandwidth - Spigot/Paper 插件主类
 * 原 NeoForge Mod 的 Spigot 移植版
 *
 * @author USS_Shenzhou
 */
public final class NotEnoughBandwidthPlugin extends JavaPlugin {

    private static NotEnoughBandwidthPlugin instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private NotEnoughBandwidthConfig configInstance;

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();

        // 初始化聚合管理器
        AggregationManager.init();

        // 初始化数据包拦截器 - 注入Netty
        PacketInterceptor.init(this);

        // 初始化区块跟踪管理器
        ChunkTrackingManager.init(this);

        // 注册命令
        if (getCommand("neb") != null) {
            getCommand("neb").setExecutor(new StatCommand());
        }

        // 注册事件监听
        getServer().getPluginManager().registerEvents(new PluginListener(), this);

        getLogger().info("NotEnoughBandwidth 已启用！");
        getLogger().info("数据包聚合与压缩已激活。");
    }

    @Override
    public void onDisable() {
        PacketInterceptor.shutdown();
        AggregationManager.shutdown();
        getLogger().info("NotEnoughBandwidth 已禁用。");
    }

    public void loadConfig() {
        File configFile = new File(getDataFolder(), "config.json");
        if (!configFile.exists()) {
            // 首次运行，从 jar 中复制带中文注释的配置模板
            saveResource("config.json", false);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile, StandardCharsets.UTF_8))) {
            // 读取全部内容并去除 // 注释，因为标准 JSON 不支持注释
            String rawJson = reader.lines().collect(Collectors.joining("\n"));
            String cleanJson = rawJson.replaceAll("//[^\n]*", "");
            configInstance = GSON.fromJson(cleanJson, NotEnoughBandwidthConfig.class);
            if (configInstance == null) {
                configInstance = new NotEnoughBandwidthConfig();
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "无法加载配置文件", e);
            configInstance = new NotEnoughBandwidthConfig();
        }
    }

    public void saveConfigToFile() {
        saveConfigToFile(new File(getDataFolder(), "config.json"));
    }

    private void saveConfigToFile(File configFile) {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(configInstance, writer);
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "无法保存配置文件", e);
        }
    }

    public static NotEnoughBandwidthPlugin getInstance() {
        return instance;
    }

    public NotEnoughBandwidthConfig getNebConfig() {
        return configInstance;
    }

    public void reloadNebConfig() {
        loadConfig();
        getLogger().info("配置已重载。");
    }
}
