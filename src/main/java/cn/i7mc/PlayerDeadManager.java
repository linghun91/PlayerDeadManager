package cn.i7mc;

import cn.i7mc.commands.PDMCommand;
import cn.i7mc.listeners.InventoryClickListener;
import cn.i7mc.listeners.PlayerDeathListener;
import cn.i7mc.listeners.PlayerInteractListener;
import cn.i7mc.listeners.TombstoneProtectionListener;
import cn.i7mc.abstracts.AbstractDataManager;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.DataManager;
import cn.i7mc.managers.GUIManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.MySQLDataManager;
import cn.i7mc.managers.TombstoneManager;
import cn.i7mc.metrics.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * PlayerDeadManager 主插件类
 *
 * @author saga
 * @version 1.0.0
 */
public class PlayerDeadManager extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private AbstractDataManager dataManager;
    private TombstoneManager tombstoneManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        // 初始化管理器
        initializeManagers();

        // 注册事件监听器
        registerListeners();

        // 注册命令
        registerCommands();

        // 初始化bStats统计
        initializeMetrics();

        // 发送启用消息
        Map<String, String> placeholders = messageManager.createPlaceholders();
        placeholders.put("version", getDescription().getVersion());
        messageManager.sendMessage(getServer().getConsoleSender(), "plugin.enabled", placeholders);
    }

    @Override
    public void onDisable() {
        // 关闭墓碑管理器
        if (tombstoneManager != null) {
            tombstoneManager.shutdown();
        }

        // 发送禁用消息
        if (messageManager != null) {
            messageManager.sendMessage(getServer().getConsoleSender(), "plugin.disabled");
        }

        getLogger().info("PlayerDeadManager 插件已禁用!");
    }

    /**
     * 初始化管理器
     * 统一的管理器初始化方法
     */
    private void initializeManagers() {
        // 初始化配置管理器
        configManager = new ConfigManager(this);

        // 初始化消息管理器
        messageManager = new MessageManager(this, configManager);

        // 初始化数据管理器
        dataManager = createDataManager();

        // 初始化GUI管理器
        guiManager = new GUIManager();

        // 初始化墓碑管理器
        tombstoneManager = new TombstoneManager(this, configManager, messageManager, dataManager);
        tombstoneManager.initialize();

        getLogger().info("管理器初始化完成");
    }

    /**
     * 创建数据管理器
     * 根据配置选择数据库类型
     *
     * @return 数据管理器实例
     */
    private AbstractDataManager createDataManager() {
        String databaseType = getConfig().getString("database.type", "sqlite").toLowerCase();

        switch (databaseType) {
            case "mysql":
                getLogger().info("使用MySQL数据库");
                return new MySQLDataManager(this);
            case "sqlite":
            default:
                getLogger().info("使用SQLite数据库");
                return new DataManager(this);
        }
    }

    /**
     * 注册事件监听器
     * 统一的监听器注册方法
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new TombstoneProtectionListener(this, configManager, messageManager, tombstoneManager), this);

        getLogger().info("事件监听器注册完成");
    }

    /**
     * 注册命令
     * 统一的命令注册方法
     */
    private void registerCommands() {
        PDMCommand commandExecutor = new PDMCommand(this);

        // 注册主命令
        getCommand("pdm").setExecutor(commandExecutor);
        getCommand("pdm").setTabCompleter(commandExecutor);

        // 注册别名命令
        getCommand("playerdeadmanager").setExecutor(commandExecutor);
        getCommand("playerdeadmanager").setTabCompleter(commandExecutor);

        getLogger().info("命令注册完成");
    }

    /**
     * 重新加载插件配置
     * 统一的重载方法
     */
    public void reloadPlugin() {
        if (configManager != null) {
            configManager.reloadConfigs();
        }

        if (messageManager != null) {
            messageManager.sendMessage(getServer().getConsoleSender(), "plugin.reload");
        }
    }

    /**
     * 获取配置管理器
     *
     * @return 配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 获取消息管理器
     *
     * @return 消息管理器
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * 获取数据管理器
     * 统一的管理器获取方法
     *
     * @return 数据管理器
     */
    public AbstractDataManager getDataManager() {
        return dataManager;
    }

    /**
     * 获取墓碑管理器
     * 统一的管理器获取方法
     *
     * @return 墓碑管理器
     */
    public TombstoneManager getTombstoneManager() {
        return tombstoneManager;
    }

    /**
     * 获取GUI管理器
     * 统一的管理器获取方法
     *
     * @return GUI管理器
     */
    public GUIManager getGUIManager() {
        return guiManager;
    }

    /**
     * 初始化bStats统计
     * 统一的统计初始化方法
     */
    private void initializeMetrics() {
        try {
            // bStats插件ID: 26074
            int pluginId = 26074;
            Metrics metrics = new Metrics(this, pluginId);

            // 添加自定义统计图表
            addCustomCharts(metrics);

            getLogger().info("bStats统计已启用");
        } catch (Exception e) {
            getLogger().warning("bStats统计初始化失败: " + e.getMessage());
        }
    }

    /**
     * 添加自定义统计图表
     * 统一的图表添加方法
     *
     * @param metrics bStats实例
     */
    private void addCustomCharts(Metrics metrics) {
        // 服务器类型统计
        metrics.addCustomChart(new Metrics.SimplePie("server_type", () -> {
            String serverVersion = getServer().getVersion();
            if (serverVersion.contains("Paper")) {
                return "Paper";
            } else if (serverVersion.contains("Spigot")) {
                return "Spigot";
            } else if (serverVersion.contains("Bukkit")) {
                return "Bukkit";
            } else {
                return "Other";
            }
        }));

        // 保存图腾功能启用状态
        metrics.addCustomChart(new Metrics.SimplePie("totem_enabled", () -> {
            if (configManager != null) {
                return configManager.getConfig().getBoolean("totem.enabled", true) ? "Enabled" : "Disabled";
            }
            return "Unknown";
        }));

        // 全息图功能启用状态
        metrics.addCustomChart(new Metrics.SimplePie("hologram_enabled", () -> {
            if (configManager != null) {
                return configManager.getConfig().getBoolean("hologram.enabled", true) ? "Enabled" : "Disabled";
            }
            return "Unknown";
        }));

        // 粒子效果启用状态
        metrics.addCustomChart(new Metrics.SimplePie("particles_enabled", () -> {
            if (configManager != null) {
                return configManager.getConfig().getBoolean("particles.enabled", true) ? "Enabled" : "Disabled";
            }
            return "Unknown";
        }));

        // 传送费用启用状态
        metrics.addCustomChart(new Metrics.SimplePie("teleport_cost_enabled", () -> {
            if (configManager != null) {
                return configManager.getConfig().getBoolean("teleport-gui.cost-enabled", false) ? "Enabled" : "Disabled";
            }
            return "Unknown";
        }));

        // 墓碑保护时间统计
        metrics.addCustomChart(new Metrics.SimplePie("protection_time", () -> {
            if (configManager != null) {
                int protectionTime = configManager.getConfig().getInt("tombstone.protection-time", 60);
                if (protectionTime <= 30) {
                    return "≤30 minutes";
                } else if (protectionTime <= 60) {
                    return "31-60 minutes";
                } else if (protectionTime <= 120) {
                    return "61-120 minutes";
                } else {
                    return ">120 minutes";
                }
            }
            return "Unknown";
        }));

        // 墓碑最大数量限制统计
        metrics.addCustomChart(new Metrics.SimplePie("max_tombstones", () -> {
            if (configManager != null) {
                int maxTombstones = configManager.getConfig().getInt("tombstone.max-tombstones", 3);
                return String.valueOf(maxTombstones);
            }
            return "Unknown";
        }));

        // 数据库类型统计
        metrics.addCustomChart(new Metrics.SimplePie("database_type", () -> {
            if (configManager != null) {
                String databaseType = configManager.getConfig().getString("database.type", "sqlite");
                return databaseType.toUpperCase();
            }
            return "Unknown";
        }));
    }
}
