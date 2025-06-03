package cn.i7mc.abstracts;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 墓碑抽象类 - 统一处理墓碑相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public abstract class AbstractTombstone {
    
    protected final UUID playerId;
    protected final Location location;
    protected final long deathTime;
    protected final long protectionExpire;
    protected final int experience;
    
    /**
     * 构造函数
     * 
     * @param playerId 玩家UUID
     * @param location 墓碑位置
     * @param deathTime 死亡时间戳
     * @param protectionExpire 保护过期时间戳
     * @param experience 存储的经验值
     */
    protected AbstractTombstone(@NotNull UUID playerId, @NotNull Location location, 
                               long deathTime, long protectionExpire, int experience) {
        this.playerId = playerId;
        this.location = location;
        this.deathTime = deathTime;
        this.protectionExpire = protectionExpire;
        this.experience = experience;
    }
    
    /**
     * 创建墓碑方块
     * 统一的墓碑创建方法
     */
    public abstract void createTombstone();
    
    /**
     * 移除墓碑方块
     * 统一的墓碑移除方法
     */
    public abstract void removeTombstone();
    
    /**
     * 检查墓碑是否可以被指定玩家访问
     * 统一的权限检查方法
     * 
     * @param player 要检查的玩家
     * @return 是否可以访问
     */
    public abstract boolean canAccess(@NotNull Player player);
    
    /**
     * 检查保护是否已过期
     * 统一的保护检查方法
     * 
     * @return 保护是否已过期
     */
    public boolean isProtectionExpired() {
        return System.currentTimeMillis() > protectionExpire;
    }
    
    /**
     * 获取墓碑所有者UUID
     * 
     * @return 玩家UUID
     */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * 获取墓碑位置
     * 
     * @return 墓碑位置
     */
    @NotNull
    public Location getLocation() {
        return location;
    }
    
    /**
     * 获取死亡时间
     * 
     * @return 死亡时间戳
     */
    public long getDeathTime() {
        return deathTime;
    }
    
    /**
     * 获取保护过期时间
     * 
     * @return 保护过期时间戳
     */
    public long getProtectionExpire() {
        return protectionExpire;
    }
    
    /**
     * 获取存储的经验值
     * 
     * @return 经验值
     */
    public int getExperience() {
        return experience;
    }
    
    /**
     * 获取剩余保护时间（秒）
     * 
     * @return 剩余保护时间，如果已过期返回0
     */
    public long getRemainingProtectionTime() {
        long remaining = (protectionExpire - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    /**
     * 检查墓碑是否应该被清理
     * 注意：此方法需要配置管理器实例，建议在具体实现类中重写
     *
     * @return 是否应该清理
     */
    public boolean shouldDespawn() {
        // 默认24小时，具体实现类应该重写此方法使用配置值
        long despawnTime = deathTime + (24 * 60 * 60 * 1000); // 24小时
        return System.currentTimeMillis() > despawnTime;
    }
}
