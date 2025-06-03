package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * 配置管理器 - 统一处理配置文件相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class ConfigManager {
    
    private final PlayerDeadManager plugin;
    private FileConfiguration config;
    private FileConfiguration messageConfig;

    private File configFile;
    private File messageFile;
    private String currentLanguage;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public ConfigManager(@NotNull PlayerDeadManager plugin) {
        this.plugin = plugin;
        initializeFiles();
    }
    
    /**
     * 初始化配置文件
     * 统一的文件初始化方法
     */
    private void initializeFiles() {
        // 创建插件数据文件夹
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // 初始化配置文件
        configFile = new File(plugin.getDataFolder(), "config.yml");

        // 创建默认配置文件
        createDefaultConfig("config.yml", configFile);
        createDefaultConfig("message.yml", new File(plugin.getDataFolder(), "message.yml"));
        createDefaultConfig("message_en.yml", new File(plugin.getDataFolder(), "message_en.yml"));

        // 加载配置
        loadConfigs();
    }
    
    /**
     * 创建默认配置文件
     * 统一的默认配置创建方法
     * 
     * @param resourceName 资源文件名
     * @param targetFile 目标文件
     */
    private void createDefaultConfig(@NotNull String resourceName, @NotNull File targetFile) {
        if (!targetFile.exists()) {
            try (InputStream inputStream = plugin.getResource(resourceName)) {
                if (inputStream != null) {
                    Files.copy(inputStream, targetFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建默认配置文件: " + resourceName);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 加载所有配置文件
     * 统一的配置加载方法
     */
    public void loadConfigs() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);

            // 获取语言设置
            currentLanguage = config.getString("language.code", "zh");

            // 根据语言设置加载对应的消息文件
            loadMessageConfig();

            plugin.getLogger().info("配置文件加载完成 - 语言: " + currentLanguage);
        } catch (Exception e) {
            plugin.getLogger().severe("配置文件加载失败!");
            e.printStackTrace();
        }
    }

    /**
     * 加载消息配置文件
     * 根据语言设置加载对应的消息文件
     */
    private void loadMessageConfig() {
        String messageFileName = "message.yml";
        if ("en".equals(currentLanguage)) {
            messageFileName = "message_en.yml";
        }

        messageFile = new File(plugin.getDataFolder(), messageFileName);

        // 如果指定语言的文件不存在，回退到默认中文
        if (!messageFile.exists()) {
            plugin.getLogger().warning("语言文件 " + messageFileName + " 不存在，回退到默认中文");
            messageFile = new File(plugin.getDataFolder(), "message.yml");
            currentLanguage = "zh";
        }

        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
    }
    
    /**
     * 重新加载所有配置文件
     * 统一的配置重载方法
     */
    public void reloadConfigs() {
        loadConfigs();
        plugin.getLogger().info("配置文件已重新加载");
    }
    
    /**
     * 保存配置文件
     * 统一的配置保存方法
     * 
     * @param configType 配置类型
     */
    public void saveConfig(@NotNull ConfigType configType) {
        try {
            switch (configType) {
                case MAIN:
                    config.save(configFile);
                    break;
                case MESSAGE:
                    messageConfig.save(messageFile);
                    break;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("保存配置文件失败: " + configType);
            e.printStackTrace();
        }
    }
    
    /**
     * 获取主配置文件
     * 
     * @return 主配置文件
     */
    @NotNull
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 获取消息配置文件
     *
     * @return 消息配置文件
     */
    @NotNull
    public FileConfiguration getMessageConfig() {
        return messageConfig;
    }

    /**
     * 获取当前语言代码
     *
     * @return 当前语言代码
     */
    @NotNull
    public String getCurrentLanguage() {
        return currentLanguage != null ? currentLanguage : "zh";
    }
    

    
    /**
     * 获取配置值（带默认值）
     * 统一的配置获取方法
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Object getConfigValue(@NotNull String path, @Nullable Object defaultValue) {
        return config.get(path, defaultValue);
    }
    
    /**
     * 获取字符串配置值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 字符串值
     */
    @Nullable
    public String getString(@NotNull String path, @Nullable String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    /**
     * 获取整数配置值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 整数值
     */
    public int getInt(@NotNull String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    /**
     * 获取布尔配置值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 布尔值
     */
    public boolean getBoolean(@NotNull String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    /**
     * 获取双精度配置值
     *
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 双精度值
     */
    public double getDouble(@NotNull String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    /**
     * 获取长整数配置值
     * 统一的长整数获取方法
     *
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 长整数值
     */
    public long getLong(@NotNull String path, long defaultValue) {
        return config.getLong(path, defaultValue);
    }
    
    /**
     * 获取字符串列表配置值
     * 
     * @param path 配置路径
     * @return 字符串列表
     */
    @NotNull
    public List<String> getStringList(@NotNull String path) {
        return config.getStringList(path);
    }
    
    /**
     * 配置类型枚举
     */
    public enum ConfigType {
        MAIN,
        MESSAGE
    }
}
