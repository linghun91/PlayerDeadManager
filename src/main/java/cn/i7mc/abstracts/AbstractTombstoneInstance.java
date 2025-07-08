package cn.i7mc.abstracts;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 墓碑整体实例抽象类
 * 统一管理墓碑的所有组件：头颅+全息图+粒子效果
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public abstract class AbstractTombstoneInstance {
    
    protected final UUID playerId;
    protected final Location location;
    protected final long deathTime;
    protected final long tombstoneId;
    protected boolean isRemoved;
    
    // 组件状态标记
    protected boolean hasSkull;
    protected boolean hasHologram;
    protected boolean hasParticles;
    
    /**
     * 构造函数
     * 
     * @param playerId 玩家UUID
     * @param location 墓碑位置
     * @param deathTime 死亡时间戳
     * @param tombstoneId 墓碑ID
     */
    public AbstractTombstoneInstance(@NotNull UUID playerId, @NotNull Location location, 
                                   long deathTime, long tombstoneId) {
        this.playerId = playerId;
        this.location = location;
        this.deathTime = deathTime;
        this.tombstoneId = tombstoneId;
        this.isRemoved = false;
        this.hasSkull = false;
        this.hasHologram = false;
        this.hasParticles = false;
    }
    
    /**
     * 创建墓碑头颅
     * 统一的头颅创建方法
     */
    public abstract void createSkull();
    
    /**
     * 创建全息图
     * 统一的全息图创建方法
     */
    public abstract void createHologram();
    
    /**
     * 创建粒子效果
     * 统一的粒子效果创建方法
     */
    public abstract void createParticles();
    
    /**
     * 移除墓碑头颅
     * 统一的头颅移除方法
     */
    public abstract void removeSkull();
    
    /**
     * 移除全息图
     * 统一的全息图移除方法
     */
    public abstract void removeHologram();
    
    /**
     * 移除粒子效果
     * 统一的粒子效果移除方法
     */
    public abstract void removeParticles();
    
    /**
     * 创建完整的墓碑实例
     * 统一的整体创建方法
     */
    public void createCompleteInstance() {
        if (isRemoved) {
            return;
        }
        
        createSkull();
        createHologram();
        createParticles();
    }
    
    /**
     * 移除完整的墓碑实例
     * 统一的整体移除方法
     */
    public void removeCompleteInstance() {
        removeSkull();
        removeHologram();
        removeParticles();
        markAsRemoved();
    }
    
    /**
     * 检查墓碑实例是否完整
     * 统一的完整性检查方法
     * 
     * @return 是否完整
     */
    public boolean isComplete() {
        return hasSkull && !isRemoved;
    }
    
    /**
     * 检查墓碑是否应该被清理（基于despawn-time）
     * 统一的清理检查方法
     * 
     * @param despawnTimeHours 配置的despawn-time（小时）
     * @return 是否应该清理
     */
    public boolean shouldDespawn(long despawnTimeHours) {
        long despawnTimeMillis = despawnTimeHours * 60 * 60 * 1000;
        return (System.currentTimeMillis() - deathTime) > despawnTimeMillis;
    }
    
    /**
     * 标记整体实例为已移除
     * 统一的移除标记方法
     */
    public void markAsRemoved() {
        this.isRemoved = true;
        this.hasSkull = false;
        this.hasHologram = false;
        this.hasParticles = false;
    }
    
    // Getter方法
    
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }
    
    @NotNull
    public Location getLocation() {
        return location;
    }
    
    public long getDeathTime() {
        return deathTime;
    }
    
    public long getTombstoneId() {
        return tombstoneId;
    }
    
    public boolean isRemoved() {
        return isRemoved;
    }
    
    public boolean hasSkull() {
        return hasSkull;
    }
    
    public boolean hasHologram() {
        return hasHologram;
    }
    
    public boolean hasParticles() {
        return hasParticles;
    }
    
    // Setter方法
    
    public void setHasSkull(boolean hasSkull) {
        this.hasSkull = hasSkull;
    }
    
    public void setHasHologram(boolean hasHologram) {
        this.hasHologram = hasHologram;
    }
    
    public void setHasParticles(boolean hasParticles) {
        this.hasParticles = hasParticles;
    }
    
    /**
     * 获取墓碑存在时间（毫秒）
     * 统一的存在时间计算方法
     * 
     * @return 存在时间
     */
    public long getExistenceTime() {
        return System.currentTimeMillis() - deathTime;
    }
    
    @Override
    public String toString() {
        return String.format("TombstoneInstance{id=%d, player=%s, location=%s, complete=%s, removed=%s}", 
                           tombstoneId, playerId, location, isComplete(), isRemoved);
    }
}
