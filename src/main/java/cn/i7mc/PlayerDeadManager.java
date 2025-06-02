package cn.i7mc;

import cn.i7mc.commands.PDMCommand;
import cn.i7mc.listeners.InventoryClickListener;
import cn.i7mc.listeners.PlayerDeathListener;
import cn.i7mc.listeners.PlayerInteractListener;
import cn.i7mc.listeners.TombstoneProtectionListener;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.DataManager;
import cn.i7mc.managers.GUIManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.TombstoneManager;
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

    @Override
    public void onEnable() {
        // 初始化管理器
        initializeManagers();

        // 注册事件监听器
        registerListeners();

        // 注册命令
        registerCommands();

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

        // 初始化墓碑管理器
        tombstoneManager = new TombstoneManager(this, configManager, messageManager, dataManager);
        tombstoneManager.initialize();

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
}
