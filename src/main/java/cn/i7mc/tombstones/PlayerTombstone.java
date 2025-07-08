package cn.i7mc.tombstones;

import cn.i7mc.abstracts.AbstractTombstone;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 玩家墓碑实现类 - 继承AbstractTombstone
 * 遵循统一方法原则，避免重复造轮子
 * 整体实例管理：头颅+全息图+粒子效果作为统一整体
 *
 * @author saga
 * @version 1.0.0
 */
public class PlayerTombstone extends AbstractTombstone {

    private final long tombstoneId;
    private final long despawnTime;
    private boolean isRemoved;
    private int currentExperience; // 可变的经验值，用于覆盖父类的final字段
    private boolean hasHologram; // 是否有全息图
    private boolean hasParticles; // 是否有粒子效果
    private boolean hasSkull; // 是否有头颅方块
    
    /**
     * 构造函数
     *
     * @param playerId 玩家UUID
     * @param location 墓碑位置
     * @param deathTime 死亡时间戳
     * @param protectionExpire 保护过期时间戳
     * @param despawnTime 消失时间戳
     * @param experience 存储的经验值
     * @param tombstoneId 数据库中的墓碑ID
     */
    public PlayerTombstone(@NotNull UUID playerId, @NotNull Location location,
                          long deathTime, long protectionExpire, long despawnTime,
                          int experience, long tombstoneId) {
        super(playerId, location, deathTime, protectionExpire, experience);
        this.tombstoneId = tombstoneId;
        this.despawnTime = despawnTime;
        this.isRemoved = false;
        this.currentExperience = experience; // 初始化可变经验值
        this.hasHologram = false;
        this.hasParticles = false;
        this.hasSkull = false;
    }
    
    /**
     * 创建墓碑方块
     * 统一的墓碑创建方法
     */
    @Override
    public void createTombstone() {
        if (isRemoved) {
            return;
        }

        Block block = location.getBlock();

        // 设置为箱子方块（默认墓碑类型）
        block.setType(Material.CHEST);
        this.hasSkull = true;

        // 注意：PersistentDataContainer的设置在TombstoneManager中处理
        // 这里只负责方块的基本设置
    }
    
    /**
     * 移除墓碑方块
     * 统一的墓碑移除方法
     */
    @Override
    public void removeTombstone() {
        if (isRemoved) {
            return;
        }

        Block block = location.getBlock();
        block.setType(Material.AIR);
        this.hasSkull = false;
        isRemoved = true;
    }
    
    /**
     * 检查墓碑是否可以被指定玩家访问
     * 统一的权限检查方法
     * 
     * @param player 要检查的玩家
     * @return 是否可以访问
     */
    @Override
    public boolean canAccess(@NotNull Player player) {
        // 墓碑所有者总是可以访问
        if (player.getUniqueId().equals(playerId)) {
            return true;
        }
        
        // 检查管理员权限
        if (player.hasPermission("playerdeadmanager.admin")) {
            return true;
        }
        
        // 检查保护是否已过期
        if (isProtectionExpired()) {
            // 保护过期后，任何玩家都可以访问
            return true;
        }
        
        // 保护期内，只有所有者和管理员可以访问
        return false;
    }
    
    /**
     * 获取数据库中的墓碑ID
     *
     * @return 墓碑ID
     */
    public long getTombstoneId() {
        return tombstoneId;
    }

    /**
     * 获取消失时间戳
     *
     * @return 消失时间戳
     */
    public long getDespawnTime() {
        return despawnTime;
    }

    /**
     * 检查墓碑是否应该被清理（重写父类方法）
     * 使用数据库中的despawn_time字段进行精确判断
     *
     * @return 是否应该清理
     */
    @Override
    public boolean shouldDespawn() {
        return System.currentTimeMillis() > despawnTime;
    }
    
    /**
     * 检查墓碑是否已被移除
     *
     * @return 是否已移除
     */
    public boolean isRemoved() {
        return isRemoved;
    }

    /**
     * 设置全息图状态
     * 统一的全息图状态管理方法
     *
     * @param hasHologram 是否有全息图
     */
    public void setHasHologram(boolean hasHologram) {
        this.hasHologram = hasHologram;
    }

    /**
     * 设置粒子效果状态
     * 统一的粒子效果状态管理方法
     *
     * @param hasParticles 是否有粒子效果
     */
    public void setHasParticles(boolean hasParticles) {
        this.hasParticles = hasParticles;
    }

    /**
     * 检查是否有全息图
     *
     * @return 是否有全息图
     */
    public boolean hasHologram() {
        return hasHologram;
    }

    /**
     * 检查是否有粒子效果
     *
     * @return 是否有粒子效果
     */
    public boolean hasParticles() {
        return hasParticles;
    }

    /**
     * 检查是否有头颅方块
     *
     * @return 是否有头颅方块
     */
    public boolean hasSkull() {
        return hasSkull;
    }

    /**
     * 检查墓碑整体实例是否完整
     * 统一的完整性检查方法
     *
     * @return 是否完整（所有组件都存在）
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

    /**
     * 获取当前经验值
     * 覆盖父类方法，返回可变的经验值
     *
     * @return 当前经验值
     */
    @Override
    public int getExperience() {
        return currentExperience;
    }

    /**
     * 设置经验值
     * 统一的经验值设置方法
     *
     * @param experience 新的经验值
     */
    public void setExperience(int experience) {
        this.currentExperience = Math.max(0, experience); // 确保经验值不为负数
    }
    
    /**
     * 获取墓碑剩余保护时间（毫秒）
     * 统一的保护时间计算方法
     * 
     * @return 剩余保护时间，已过期返回0
     */
    public long getRemainingProtectionTime() {
        if (isProtectionExpired()) {
            return 0;
        }
        return protectionExpire - System.currentTimeMillis();
    }
    
    /**
     * 获取墓碑剩余保护时间（格式化字符串）
     * 统一的时间格式化方法
     *
     * @return 格式化的剩余时间
     * @deprecated 使用 {@link cn.i7mc.utils.TimeUtil#formatRemainingTime(long, cn.i7mc.managers.MessageManager)} 替代
     */
    @NotNull
    @Deprecated
    public String getFormattedRemainingTime() {
        return cn.i7mc.utils.TimeUtil.formatRemainingTime(getRemainingProtectionTime(), null);
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
    
    /**
     * 获取墓碑存在时间（格式化字符串）
     * 统一的时间格式化方法
     *
     * @return 格式化的存在时间
     * @deprecated 使用 {@link cn.i7mc.utils.TimeUtil#formatDuration(long, cn.i7mc.managers.MessageManager)} 替代
     */
    @NotNull
    @Deprecated
    public String getFormattedExistenceTime() {
        return cn.i7mc.utils.TimeUtil.formatDuration(getExistenceTime(), null);
    }
    
    /**
     * 检查墓碑是否应该自动清理
     * 统一的清理检查方法
     * 
     * @param maxExistenceTime 最大存在时间（毫秒）
     * @return 是否应该清理
     */
    public boolean shouldAutoClean(long maxExistenceTime) {
        return getExistenceTime() > maxExistenceTime;
    }
    
    /**
     * 获取墓碑状态描述
     * 统一的状态描述方法
     *
     * @return 状态描述
     * @deprecated 状态描述应通过MessageManager获取，避免硬编码
     */
    @NotNull
    @Deprecated
    public String getStatusDescription() {
        if (isRemoved) {
            return "已移除";
        }

        if (isProtectionExpired()) {
            return "保护已过期";
        }

        return "受保护中";
    }
    
    @Override
    public String toString() {
        return String.format("PlayerTombstone{id=%d, player=%s, location=%s, status=%s}", 
                           tombstoneId, playerId, location, getStatusDescription());
    }
}
