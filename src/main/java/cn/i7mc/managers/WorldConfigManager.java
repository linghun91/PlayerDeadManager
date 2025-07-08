package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 世界配置管理器 - 统一处理世界特定配置和权限控制
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class WorldConfigManager {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    
    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param configManager 配置管理器
     */
    public WorldConfigManager(@NotNull PlayerDeadManager plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    /**
     * 检查世界是否启用墓碑功能
     * 统一的世界检查方法
     * 
     * @param worldName 世界名称
     * @return 是否启用
     */
    public boolean isWorldEnabled(@NotNull String worldName) {
        // 检查禁用世界列表
        if (configManager.getStringList("worlds.disabled-worlds").contains(worldName)) {
            return false;
        }
        
        // 检查启用世界列表（空列表表示所有世界）
        var enabledWorlds = configManager.getStringList("worlds.enabled-worlds");
        return enabledWorlds.isEmpty() || enabledWorlds.contains(worldName);
    }
    
    /**
     * 检查玩家是否有绕过世界限制的权限
     * 统一的绕过权限检查方法
     * 
     * @param player 玩家
     * @return 是否有绕过权限
     */
    public boolean canBypassWorldRestriction(@NotNull Player player) {
        String bypassPermission = configManager.getString("permissions.features.bypass-world", "playerdeadmanager.bypass.world");
        return player.hasPermission(bypassPermission);
    }
    
    /**
     * 检查世界是否启用PVP死亡墓碑
     * 统一的PVP配置检查方法
     * 
     * @param worldName 世界名称
     * @return 是否启用PVP死亡墓碑
     */
    public boolean isPvpOnlyEnabled(@NotNull String worldName) {
        // 检查世界特定配置
        String worldConfigPath = "worlds.world-configs." + worldName + ".pvp-only";
        if (configManager.getConfig().contains(worldConfigPath)) {
            return configManager.getBoolean(worldConfigPath, true);
        }
        
        // 使用全局配置
        return configManager.getBoolean("tombstone.pvp-only", true);
    }
    
    /**
     * 检查世界是否启用经济系统
     * 统一的经济配置检查方法
     * 
     * @param worldName 世界名称
     * @return 是否启用经济系统
     */
    public boolean isEconomyEnabled(@NotNull String worldName) {
        // 检查世界特定配置
        String worldConfigPath = "worlds.world-configs." + worldName + ".economy-enabled";
        if (configManager.getConfig().contains(worldConfigPath)) {
            return configManager.getBoolean(worldConfigPath, true);
        }
        
        // 使用全局配置
        return configManager.getBoolean("economy.enabled", true);
    }
    
    /**
     * 检查世界是否启用头颅保护
     * 统一的头颅保护配置检查方法
     * 
     * @param worldName 世界名称
     * @return 是否启用头颅保护
     */
    public boolean isSkullProtectionEnabled(@NotNull String worldName) {
        // 检查世界特定配置
        String worldConfigPath = "worlds.world-configs." + worldName + ".skull-protection";
        if (configManager.getConfig().contains(worldConfigPath)) {
            return configManager.getBoolean(worldConfigPath, true);
        }
        
        // 使用全局配置
        return configManager.getBoolean("tombstone.skull-protection", true);
    }
    
    /**
     * 检查玩家是否有使用墓碑系统的权限
     * 统一的基础权限检查方法
     * 
     * @param player 玩家
     * @return 是否有权限
     */
    public boolean hasBasicPermission(@NotNull Player player) {
        if (!configManager.getBoolean("permissions.enabled", true)) {
            return true; // 权限检查已禁用
        }
        
        String basePermission = configManager.getString("permissions.base", "playerdeadmanager.use");
        return player.hasPermission(basePermission);
    }
    
    /**
     * 检查玩家是否有PVP墓碑权限
     * 统一的PVP权限检查方法
     * 
     * @param player 玩家
     * @return 是否有PVP权限
     */
    public boolean hasPvpPermission(@NotNull Player player) {
        String pvpPermission = configManager.getString("permissions.features.pvp-tombstone", "playerdeadmanager.pvp");
        return player.hasPermission(pvpPermission);
    }
    
    /**
     * 检查玩家是否有经济系统权限
     * 统一的经济权限检查方法
     * 
     * @param player 玩家
     * @return 是否有经济权限
     */
    public boolean hasEconomyPermission(@NotNull Player player) {
        String economyPermission = configManager.getString("permissions.features.economy", "playerdeadmanager.economy");
        return player.hasPermission(economyPermission);
    }
    
    /**
     * 检查玩家是否有头颅保护权限
     * 统一的头颅保护权限检查方法
     * 
     * @param player 玩家
     * @return 是否有头颅保护权限
     */
    public boolean hasSkullProtectionPermission(@NotNull Player player) {
        String protectionPermission = configManager.getString("permissions.features.skull-protection", "playerdeadmanager.protection");
        return player.hasPermission(protectionPermission);
    }
    
    /**
     * 检查玩家是否有管理员权限
     * 统一的管理员权限检查方法
     * 
     * @param player 玩家
     * @return 是否有管理员权限
     */
    public boolean hasAdminPermission(@NotNull Player player) {
        String adminPermission = configManager.getString("permissions.admin", "playerdeadmanager.admin");
        return player.hasPermission(adminPermission);
    }
    
    /**
     * 检查玩家在指定世界是否可以使用墓碑功能
     * 统一的综合权限检查方法
     * 
     * @param player 玩家
     * @param worldName 世界名称
     * @return 是否可以使用
     */
    public boolean canUseTombstoneInWorld(@NotNull Player player, @NotNull String worldName) {
        // 检查基础权限
        if (!hasBasicPermission(player)) {
            return false;
        }
        
        // 检查世界是否启用
        if (!isWorldEnabled(worldName)) {
            // 检查是否有绕过权限
            return canBypassWorldRestriction(player);
        }
        
        return true;
    }
    
    /**
     * 检查玩家在指定世界是否可以使用PVP墓碑功能
     * 统一的PVP功能权限检查方法
     * 
     * @param player 玩家
     * @param worldName 世界名称
     * @return 是否可以使用PVP功能
     */
    public boolean canUsePvpTombstoneInWorld(@NotNull Player player, @NotNull String worldName) {
        // 先检查基础权限
        if (!canUseTombstoneInWorld(player, worldName)) {
            return false;
        }
        
        // 检查世界是否启用PVP墓碑
        if (!isPvpOnlyEnabled(worldName)) {
            return true; // 世界未启用PVP限制，所有死亡都可以创建墓碑
        }
        
        // 检查PVP权限
        return hasPvpPermission(player);
    }
    
    /**
     * 检查玩家在指定世界是否可以使用经济功能
     * 统一的经济功能权限检查方法
     * 
     * @param player 玩家
     * @param worldName 世界名称
     * @return 是否可以使用经济功能
     */
    public boolean canUseEconomyInWorld(@NotNull Player player, @NotNull String worldName) {
        // 先检查基础权限
        if (!canUseTombstoneInWorld(player, worldName)) {
            return false;
        }
        
        // 检查世界是否启用经济系统
        if (!isEconomyEnabled(worldName)) {
            return false;
        }
        
        // 检查经济权限
        return hasEconomyPermission(player);
    }
    
    /**
     * 获取所有启用的世界列表
     * 统一的世界列表获取方法
     * 
     * @return 启用的世界名称列表
     */
    public java.util.List<String> getEnabledWorlds() {
        var enabledWorlds = configManager.getStringList("worlds.enabled-worlds");
        if (enabledWorlds.isEmpty()) {
            // 如果启用列表为空，返回所有已加载的世界（除了禁用的）
            var disabledWorlds = configManager.getStringList("worlds.disabled-worlds");
            return plugin.getServer().getWorlds().stream()
                .map(world -> world.getName())
                .filter(name -> !disabledWorlds.contains(name))
                .toList();
        }
        return enabledWorlds;
    }
    
    /**
     * 获取所有禁用的世界列表
     * 统一的禁用世界列表获取方法
     * 
     * @return 禁用的世界名称列表
     */
    public java.util.List<String> getDisabledWorlds() {
        return configManager.getStringList("worlds.disabled-worlds");
    }
}
