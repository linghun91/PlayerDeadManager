package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * VIP权限时间管理器 - 统一处理VIP玩家的特殊时间权限
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class VipTimeManager {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    
    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param configManager 配置管理器
     */
    public VipTimeManager(@NotNull PlayerDeadManager plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    /**
     * 检查是否启用VIP权限时间
     * 统一的VIP功能状态检查方法
     * 
     * @return 是否启用VIP权限时间
     */
    public boolean isVipTimeEnabled() {
        return configManager.getBoolean("vip-times.enabled", true);
    }
    
    /**
     * 获取玩家的保护时间（分钟）
     * 统一的保护时间获取方法
     * 
     * @param player 玩家
     * @return 保护时间（分钟）
     */
    public long getProtectionTime(@NotNull Player player) {
        if (!isVipTimeEnabled()) {
            return configManager.getLong("tombstone.protection-time", 60);
        }
        
        // 获取玩家的最高VIP权限
        String highestVipPermission = getHighestVipPermission(player);
        if (highestVipPermission != null) {
            return configManager.getLong("vip-times.permissions." + highestVipPermission + ".protection-time", 
                                       configManager.getLong("tombstone.protection-time", 60));
        }
        
        // 没有VIP权限，使用默认时间
        return configManager.getLong("tombstone.protection-time", 60);
    }
    
    /**
     * 获取玩家的消失时间（小时）
     * 统一的消失时间获取方法
     * 
     * @param player 玩家
     * @return 消失时间（小时）
     */
    public long getDespawnTime(@NotNull Player player) {
        if (!isVipTimeEnabled()) {
            return configManager.getLong("tombstone.despawn-time", 24);
        }
        
        // 获取玩家的最高VIP权限
        String highestVipPermission = getHighestVipPermission(player);
        if (highestVipPermission != null) {
            return configManager.getLong("vip-times.permissions." + highestVipPermission + ".despawn-time", 
                                       configManager.getLong("tombstone.despawn-time", 24));
        }
        
        // 没有VIP权限，使用默认时间
        return configManager.getLong("tombstone.despawn-time", 24);
    }
    
    /**
     * 获取玩家的最高VIP权限
     * 统一的VIP权限检查方法
     * 
     * @param player 玩家
     * @return 最高VIP权限节点，如果没有VIP权限则返回null
     */
    private String getHighestVipPermission(@NotNull Player player) {
        Set<String> vipPermissions = configManager.getConfig().getConfigurationSection("vip-times.permissions").getKeys(false);
        
        // 按权限优先级排序（假设数字越大权限越高）
        String highestPermission = null;
        int highestLevel = -1;
        
        for (String permission : vipPermissions) {
            if (player.hasPermission(permission)) {
                // 尝试从权限名称中提取等级数字
                int level = extractVipLevel(permission);
                if (level > highestLevel) {
                    highestLevel = level;
                    highestPermission = permission;
                }
            }
        }
        
        return highestPermission;
    }
    
    /**
     * 从权限节点中提取VIP等级
     * 统一的VIP等级提取方法
     * 
     * @param permission 权限节点
     * @return VIP等级，如果无法提取则返回0
     */
    private int extractVipLevel(@NotNull String permission) {
        // 尝试从权限名称末尾提取数字
        // 例如: playerdeadmanager.vip1 -> 1, playerdeadmanager.vip2 -> 2
        String[] parts = permission.split("\\.");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            // 提取末尾的数字
            StringBuilder numberStr = new StringBuilder();
            for (int i = lastPart.length() - 1; i >= 0; i--) {
                char c = lastPart.charAt(i);
                if (Character.isDigit(c)) {
                    numberStr.insert(0, c);
                } else {
                    break;
                }
            }
            
            if (numberStr.length() > 0) {
                try {
                    return Integer.parseInt(numberStr.toString());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        
        return 0;
    }
    
    /**
     * 获取玩家的VIP等级显示名称
     * 统一的VIP等级显示方法
     * 
     * @param player 玩家
     * @return VIP等级显示名称
     */
    public String getVipLevelDisplay(@NotNull Player player) {
        if (!isVipTimeEnabled()) {
            return "普通";
        }
        
        String highestVipPermission = getHighestVipPermission(player);
        if (highestVipPermission != null) {
            int level = extractVipLevel(highestVipPermission);
            return "VIP" + level;
        }
        
        return "普通";
    }
    
    /**
     * 检查玩家是否有任何VIP权限
     * 统一的VIP状态检查方法
     * 
     * @param player 玩家
     * @return 是否为VIP玩家
     */
    public boolean isVipPlayer(@NotNull Player player) {
        if (!isVipTimeEnabled()) {
            return false;
        }
        
        return getHighestVipPermission(player) != null;
    }
    
    /**
     * 获取所有配置的VIP权限节点
     * 统一的VIP权限列表获取方法
     * 
     * @return VIP权限节点集合
     */
    public Set<String> getAllVipPermissions() {
        if (!isVipTimeEnabled()) {
            return Set.of();
        }
        
        return configManager.getConfig().getConfigurationSection("vip-times.permissions").getKeys(false);
    }
}
