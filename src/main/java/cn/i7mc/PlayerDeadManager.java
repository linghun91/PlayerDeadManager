package cn.i7mc;

import cn.i7mc.commands.PDMCommand;
import cn.i7mc.listeners.InventoryClickListener;
import cn.i7mc.listeners.PlayerDeathListener;
import cn.i7mc.listeners.PlayerInteractListener;
import cn.i7mc.listeners.TombstoneProtectionListener;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.DataManager;
import cn.i7mc.managers.EconomyManager;
import cn.i7mc.managers.GUIManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.TombstoneManager;
import cn.i7mc.managers.VipExemptionManager;
import cn.i7mc.managers.VipTimeManager;
import cn.i7mc.managers.WorldConfigManager;
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
    private DataManager dataManager;
    private TombstoneManager tombstoneManager;
    private GUIManager guiManager;
    private EconomyManager economyManager;
    private VipTimeManager vipTimeManager;
    private VipExemptionManager vipExemptionManager;
    private WorldConfigManager worldConfigManager;

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
        dataManager = new DataManager(this);

        // 初始化GUI管理器
        guiManager = new GUIManager();

        // 初始化经济管理器
        economyManager = new EconomyManager(this, configManager);

        // 初始化VIP时间管理器
        vipTimeManager = new VipTimeManager(this, configManager);

        // 初始化VIP豁免管理器
        vipExemptionManager = new VipExemptionManager(this);

        // 初始化世界配置管理器
        worldConfigManager = new WorldConfigManager(this, configManager);

        // 初始化墓碑管理器
        tombstoneManager = new TombstoneManager(this, configManager, messageManager, dataManager);
        tombstoneManager.initialize();

        // 启动VIP豁免清理任务
        startVipExemptionCleanupTask();

        getLogger().info("管理器初始化完成");
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
    public DataManager getDataManager() {
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
    }



    /**
     * 获取经济管理器
     *
     * @return 经济管理器实例
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    /**
     * 获取VIP时间管理器
     *
     * @return VIP时间管理器实例
     */
    public VipTimeManager getVipTimeManager() {
        return vipTimeManager;
    }

    /**
     * 获取VIP豁免管理器
     *
     * @return VIP豁免管理器实例
     */
    public VipExemptionManager getVipExemptionManager() {
        return vipExemptionManager;
    }

    /**
     * 获取世界配置管理器
     *
     * @return 世界配置管理器实例
     */
    public WorldConfigManager getWorldConfigManager() {
        return worldConfigManager;
    }

    /**
     * 启动VIP豁免清理任务
     * 统一的清理任务启动方法
     */
    private void startVipExemptionCleanupTask() {
        if (vipExemptionManager != null && vipExemptionManager.isExemptionEnabled()) {
            // 每天清理一次过期的豁免记录 (24小时 = 24 * 60 * 60 * 20 ticks)
            long cleanupInterval = 24 * 60 * 60 * 20L;

            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                vipExemptionManager.cleanupExpiredExemptions();
            }, cleanupInterval, cleanupInterval);

            getLogger().info("VIP豁免清理任务已启动");
        }
    }
}
