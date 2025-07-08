package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VIP豁免管理器 - 管理玩家每日死亡豁免次数
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class VipExemptionManager {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final DataManager dataManager;
    
    // 权限模式匹配
    private static final Pattern VIP_SAVE_PATTERN = Pattern.compile("playerdeadmanager\\.vip\\.save\\.(\\d+|unlimited)");
    
    // 缓存玩家今日豁免使用情况
    private final Map<UUID, Integer> dailyExemptionUsage = new HashMap<>();
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public VipExemptionManager(@NotNull PlayerDeadManager plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.dataManager = plugin.getDataManager();
    }
    
    /**
     * 检查玩家是否有VIP豁免权限并且今日还有可用次数
     * 统一的豁免检查方法
     *
     * @param player 玩家
     * @return 是否可以使用豁免
     */
    public boolean canUseExemption(@NotNull Player player) {
        if (!isExemptionEnabled()) {
            return false;
        }

        int maxExemptions = getPlayerMaxExemptions(player);
        if (maxExemptions <= 0) {
            return false; // 没有豁免权限
        }

        // 如果是无限豁免，直接返回true
        if (maxExemptions == Integer.MAX_VALUE) {
            return true;
        }

        int usedExemptions = getPlayerUsedExemptions(player);
        return usedExemptions < maxExemptions;
    }
    
    /**
     * 使用一次豁免
     * 统一的豁免使用方法
     * 
     * @param player 玩家
     * @return 是否成功使用豁免
     */
    public boolean useExemption(@NotNull Player player) {
        if (!canUseExemption(player)) {
            return false;
        }
        
        try {
            // 增加使用次数
            incrementExemptionUsage(player);
            
            // 发送豁免使用消息
            sendExemptionUsedMessage(player);
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("保存豁免使用记录失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取玩家的最大豁免次数
     * 统一的最大豁免次数获取方法
     * 
     * @param player 玩家
     * @return 最大豁免次数
     */
    public int getPlayerMaxExemptions(@NotNull Player player) {
        if (!isExemptionEnabled()) {
            return 0;
        }
        
        int maxExemptions = 0;
        
        // 检查所有VIP豁免权限，取最高值
        for (String permission : player.getEffectivePermissions().stream()
                .map(perm -> perm.getPermission())
                .filter(perm -> VIP_SAVE_PATTERN.matcher(perm).matches())
                .toList()) {

            Matcher matcher = VIP_SAVE_PATTERN.matcher(permission);
            if (matcher.matches()) {
                String exemptionValue = matcher.group(1);
                if ("unlimited".equals(exemptionValue)) {
                    return Integer.MAX_VALUE; // 无限次豁免
                }

                try {
                    int exemptions = Integer.parseInt(exemptionValue);
                    maxExemptions = Math.max(maxExemptions, exemptions);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("无效的VIP豁免权限格式: " + permission);
                }
            }
        }
        
        return maxExemptions;
    }
    
    /**
     * 获取玩家今日已使用的豁免次数
     * 统一的已使用次数获取方法
     * 
     * @param player 玩家
     * @return 已使用次数
     */
    public int getPlayerUsedExemptions(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        
        // 先从缓存中获取
        Integer cached = dailyExemptionUsage.get(playerId);
        if (cached != null) {
            return cached;
        }
        
        // 从数据库获取
        try {
            int used = getExemptionUsageFromDatabase(playerId);
            dailyExemptionUsage.put(playerId, used);
            return used;
        } catch (SQLException e) {
            plugin.getLogger().warning("获取豁免使用记录失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 检查是否启用豁免功能
     * 统一的功能启用检查方法
     * 
     * @return 是否启用
     */
    public boolean isExemptionEnabled() {
        return configManager.getBoolean("vip-exemption.enabled", true);
    }
    
    /**
     * 获取今日日期字符串
     * 统一的日期格式化方法
     * 
     * @return 今日日期字符串
     */
    private String getTodayDateString() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    /**
     * 从数据库获取豁免使用次数
     * 统一的数据库查询方法
     *
     * @param playerId 玩家UUID
     * @return 使用次数
     * @throws SQLException 数据库异常
     */
    private int getExemptionUsageFromDatabase(@NotNull UUID playerId) throws SQLException {
        String query = """
            SELECT used_count FROM player_exemptions
            WHERE player_uuid = ? AND exemption_date = ?
        """;

        try (PreparedStatement stmt = dataManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, getTodayDateString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("used_count");
                }
            }
        }

        return 0; // 今日未使用过
    }
    
    /**
     * 增加豁免使用次数
     * 统一的使用次数增加方法
     *
     * @param player 玩家
     * @throws SQLException 数据库异常
     */
    private void incrementExemptionUsage(@NotNull Player player) throws SQLException {
        UUID playerId = player.getUniqueId();
        String today = getTodayDateString();

        // SQLite使用INSERT OR REPLACE语法
        String upsertQuery = """
            INSERT OR REPLACE INTO player_exemptions (player_uuid, exemption_date, used_count)
            VALUES (?, ?, COALESCE((SELECT used_count FROM player_exemptions WHERE player_uuid = ? AND exemption_date = ?), 0) + 1)
        """;

        try (PreparedStatement stmt = dataManager.getConnection().prepareStatement(upsertQuery)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, today);
            stmt.setString(3, playerId.toString());
            stmt.setString(4, today);
            stmt.executeUpdate();

            // 更新缓存
            int currentUsage = dailyExemptionUsage.getOrDefault(playerId, 0);
            dailyExemptionUsage.put(playerId, currentUsage + 1);
        }
    }
    
    /**
     * 发送豁免使用消息
     * 统一的消息发送方法
     *
     * @param player 玩家
     */
    private void sendExemptionUsedMessage(@NotNull Player player) {
        int maxExemptions = getPlayerMaxExemptions(player);
        int usedExemptions = getPlayerUsedExemptions(player);

        Map<String, String> placeholders = messageManager.createPlaceholders();
        messageManager.addPlayerPlaceholders(placeholders, player);
        placeholders.put("used", String.valueOf(usedExemptions));

        if (maxExemptions == Integer.MAX_VALUE) {
            placeholders.put("max", "无限");
            placeholders.put("remaining", "无限");
        } else {
            int remainingExemptions = maxExemptions - usedExemptions;
            placeholders.put("max", String.valueOf(maxExemptions));
            placeholders.put("remaining", String.valueOf(remainingExemptions));
        }

        messageManager.sendMessage(player, "vip-exemption.used", placeholders);
    }
    
    /**
     * 清理过期的豁免记录
     * 统一的数据清理方法
     */
    public void cleanupExpiredExemptions() {
        try {
            String deleteQuery = """
                DELETE FROM player_exemptions
                WHERE exemption_date < ?
            """;

            // 删除7天前的记录
            String cutoffDate = LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE);

            try (PreparedStatement stmt = dataManager.getConnection().prepareStatement(deleteQuery)) {
                stmt.setString(1, cutoffDate);
                int deletedRows = stmt.executeUpdate();

                if (deletedRows > 0) {
                    plugin.getLogger().info("清理了 " + deletedRows + " 条过期的豁免记录");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("清理过期豁免记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取玩家豁免状态信息
     * 统一的状态信息获取方法
     *
     * @param player 玩家
     * @return 豁免状态信息
     */
    public String getExemptionStatusInfo(@NotNull Player player) {
        if (!isExemptionEnabled()) {
            return messageManager.getMessage("vip-exemption.disabled", null);
        }

        int maxExemptions = getPlayerMaxExemptions(player);
        if (maxExemptions <= 0) {
            return messageManager.getMessage("vip-exemption.no-permission", null);
        }

        int usedExemptions = getPlayerUsedExemptions(player);

        Map<String, String> placeholders = messageManager.createPlaceholders();
        messageManager.addPlayerPlaceholders(placeholders, player);
        placeholders.put("used", String.valueOf(usedExemptions));

        if (maxExemptions == Integer.MAX_VALUE) {
            placeholders.put("max", "无限");
            placeholders.put("remaining", "无限");
        } else {
            int remainingExemptions = maxExemptions - usedExemptions;
            placeholders.put("max", String.valueOf(maxExemptions));
            placeholders.put("remaining", String.valueOf(remainingExemptions));
        }

        return messageManager.getMessage("vip-exemption.status", placeholders);
    }
    
    /**
     * 重置玩家缓存
     * 统一的缓存重置方法
     * 
     * @param playerId 玩家UUID
     */
    public void resetPlayerCache(@NotNull UUID playerId) {
        dailyExemptionUsage.remove(playerId);
    }
    
    /**
     * 清空所有缓存
     * 统一的缓存清空方法
     */
    public void clearAllCache() {
        dailyExemptionUsage.clear();
    }
}
