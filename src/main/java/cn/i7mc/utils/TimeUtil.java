package cn.i7mc.utils;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 时间工具类 - 统一处理时间相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class TimeUtil {
    
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SIMPLE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    
    /**
     * 格式化时间戳为可读字符串
     * 统一的时间格式化方法
     * 
     * @param timestamp 时间戳（毫秒）
     * @return 格式化的时间字符串
     */
    @NotNull
    public static String formatTimestamp(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(DEFAULT_FORMATTER);
    }
    
    /**
     * 格式化时间戳为简单字符串
     * 统一的简单时间格式化方法
     * 
     * @param timestamp 时间戳（毫秒）
     * @return 简单格式化的时间字符串
     */
    @NotNull
    public static String formatTimestampSimple(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(SIMPLE_FORMATTER);
    }
    
    /**
     * 格式化剩余时间为可读字符串
     * 统一的剩余时间格式化方法
     * 
     * @param remainingMillis 剩余毫秒数
     * @return 格式化的剩余时间字符串
     */
    @NotNull
    public static String formatRemainingTime(long remainingMillis) {
        if (remainingMillis <= 0) {
            return "已过期";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(remainingMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(remainingMillis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60;
        
        if (days > 0) {
            return String.format("%d天%d小时", days, hours);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }
    
    /**
     * 格式化持续时间为可读字符串
     * 统一的持续时间格式化方法
     * 
     * @param durationMillis 持续时间（毫秒）
     * @return 格式化的持续时间字符串
     */
    @NotNull
    public static String formatDuration(long durationMillis) {
        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
        
        if (days > 0) {
            return String.format("%d天%d小时", days, hours);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else {
            return String.format("%d分钟", minutes);
        }
    }
    
    /**
     * 检查时间是否已过期
     * 统一的过期检查方法
     * 
     * @param expireTime 过期时间戳（毫秒）
     * @return 是否已过期
     */
    public static boolean isExpired(long expireTime) {
        return System.currentTimeMillis() > expireTime;
    }
    
    /**
     * 计算剩余时间
     * 统一的剩余时间计算方法
     * 
     * @param expireTime 过期时间戳（毫秒）
     * @return 剩余时间（毫秒），已过期返回0
     */
    public static long getRemainingTime(long expireTime) {
        long remaining = expireTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * 将分钟转换为毫秒
     * 统一的时间单位转换方法
     * 
     * @param minutes 分钟数
     * @return 毫秒数
     */
    public static long minutesToMillis(long minutes) {
        return TimeUnit.MINUTES.toMillis(minutes);
    }
    
    /**
     * 将小时转换为毫秒
     * 统一的时间单位转换方法
     * 
     * @param hours 小时数
     * @return 毫秒数
     */
    public static long hoursToMillis(long hours) {
        return TimeUnit.HOURS.toMillis(hours);
    }
    
    /**
     * 将天数转换为毫秒
     * 统一的时间单位转换方法
     * 
     * @param days 天数
     * @return 毫秒数
     */
    public static long daysToMillis(long days) {
        return TimeUnit.DAYS.toMillis(days);
    }
    
    /**
     * 获取当前时间戳
     * 统一的当前时间获取方法
     * 
     * @return 当前时间戳（毫秒）
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}
