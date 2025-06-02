package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息管理器 - 统一处理消息发送和格式化
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class MessageManager {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final Pattern placeholderPattern = Pattern.compile("\\{([^}]+)\\}");
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param configManager 配置管理器
     */
    public MessageManager(@NotNull PlayerDeadManager plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    /**
     * 发送消息给玩家
     * 统一的消息发送方法
     * 
     * @param sender 消息接收者
     * @param messageKey 消息键
     * @param placeholders 占位符替换
     */
    public void sendMessage(@NotNull CommandSender sender, @NotNull String messageKey, 
                           @Nullable Map<String, String> placeholders) {
        String message = getMessage(messageKey, placeholders);
        if (message != null && !message.trim().isEmpty()) {
            sender.sendMessage(message);
        }
    }
    
    /**
     * 发送消息给玩家（无占位符）
     * 
     * @param sender 消息接收者
     * @param messageKey 消息键
     */
    public void sendMessage(@NotNull CommandSender sender, @NotNull String messageKey) {
        sendMessage(sender, messageKey, null);
    }
    

    
    /**
     * 获取格式化消息
     * 统一的消息获取方法
     * 
     * @param messageKey 消息键
     * @param placeholders 占位符替换
     * @return 格式化后的消息
     */
    @Nullable
    public String getMessage(@NotNull String messageKey, @Nullable Map<String, String> placeholders) {
        String message = configManager.getMessageConfig().getString(messageKey);
        if (message == null) {
            return null;
        }
        
        // 添加前缀
        String prefix = configManager.getMessageConfig().getString("prefix", "");
        if (!prefix.isEmpty() && !messageKey.startsWith("prefix")) {
            message = prefix + message;
        }
        
        // 替换占位符
        message = replacePlaceholders(message, placeholders);
        
        // 处理颜色代码
        message = ChatColor.translateAlternateColorCodes('&', message);
        
        return message;
    }
    

    
    /**
     * 替换消息中的占位符
     * 统一的占位符替换方法
     * 
     * @param message 原始消息
     * @param placeholders 占位符映射
     * @return 替换后的消息
     */
    @NotNull
    private String replacePlaceholders(@NotNull String message, @Nullable Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }
        
        Matcher matcher = placeholderPattern.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = placeholders.get(placeholder);
            if (replacement != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 创建占位符映射
     * 统一的占位符创建方法
     * 
     * @return 新的占位符映射
     */
    @NotNull
    public Map<String, String> createPlaceholders() {
        return new HashMap<>();
    }
    
    /**
     * 添加玩家相关占位符
     * 
     * @param placeholders 占位符映射
     * @param player 玩家
     */
    public void addPlayerPlaceholders(@NotNull Map<String, String> placeholders, @NotNull Player player) {
        placeholders.put("player", player.getName());
        placeholders.put("uuid", player.getUniqueId().toString());
        placeholders.put("world", player.getWorld().getName());
    }
    
    /**
     * 添加位置相关占位符
     * 
     * @param placeholders 占位符映射
     * @param world 世界名
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     */
    public void addLocationPlaceholders(@NotNull Map<String, String> placeholders, 
                                      @NotNull String world, int x, int y, int z) {
        placeholders.put("world", world);
        placeholders.put("x", String.valueOf(x));
        placeholders.put("y", String.valueOf(y));
        placeholders.put("z", String.valueOf(z));
        placeholders.put("location", world + " " + x + ", " + y + ", " + z);
    }
    
    /**
     * 添加时间相关占位符
     * 
     * @param placeholders 占位符映射
     * @param timestamp 时间戳
     */
    public void addTimePlaceholders(@NotNull Map<String, String> placeholders, long timestamp) {
        String timeFormat = configManager.getMessageConfig().getString("time.format", "yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        String formattedTime = formatter.format(new Date(timestamp));
        
        placeholders.put("time", formattedTime);
        placeholders.put("timestamp", String.valueOf(timestamp));
        
        // 添加相对时间
        String relativeTime = getRelativeTime(timestamp);
        placeholders.put("relative_time", relativeTime);
    }
    
    /**
     * 获取相对时间描述
     * 
     * @param timestamp 时间戳
     * @return 相对时间描述
     */
    @NotNull
    private String getRelativeTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (seconds < 60) {
            if (seconds < 5) {
                return configManager.getMessageConfig().getString("time.just-now", "刚刚");
            }
            return configManager.getMessageConfig().getString("time.seconds-ago", "{time}秒前")
                    .replace("{time}", String.valueOf(seconds));
        } else if (minutes < 60) {
            return configManager.getMessageConfig().getString("time.minutes-ago", "{time}分钟前")
                    .replace("{time}", String.valueOf(minutes));
        } else if (hours < 24) {
            return configManager.getMessageConfig().getString("time.hours-ago", "{time}小时前")
                    .replace("{time}", String.valueOf(hours));
        } else {
            return configManager.getMessageConfig().getString("time.days-ago", "{time}天前")
                    .replace("{time}", String.valueOf(days));
        }
    }
    
    /**
     * 格式化保护状态
     * 
     * @param isProtected 是否受保护
     * @param remainingTime 剩余时间（秒）
     * @return 格式化的保护状态
     */
    @NotNull
    public String formatProtectionStatus(boolean isProtected, long remainingTime) {
        if (!isProtected) {
            return configManager.getMessageConfig().getString("protection.expired", "&c已过期");
        }
        
        if (remainingTime <= 0) {
            return configManager.getMessageConfig().getString("protection.public", "&e公共访问");
        }
        
        String timeStr = formatTime(remainingTime);
        return configManager.getMessageConfig().getString("protection.active", "&a受保护 ({time})")
                .replace("{time}", timeStr);
    }
    
    /**
     * 格式化时间（秒转换为可读格式）
     * 
     * @param seconds 秒数
     * @return 格式化的时间
     */
    @NotNull
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else {
            return (seconds / 3600) + "小时";
        }
    }
}
