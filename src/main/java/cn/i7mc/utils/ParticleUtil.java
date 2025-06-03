package cn.i7mc.utils;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.tombstones.PlayerTombstone;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 粒子效果工具类 - 统一处理粒子效果相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class ParticleUtil {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final Map<Location, BukkitTask> particleTasks;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param configManager 配置管理器
     */
    public ParticleUtil(@NotNull PlayerDeadManager plugin,
                       @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.particleTasks = new HashMap<>();
    }
    
    /**
     * 为墓碑创建粒子效果
     * 统一的粒子效果创建方法
     * 
     * @param tombstone 墓碑实例
     */
    public void createParticleEffect(@NotNull PlayerTombstone tombstone) {
        if (!configManager.getBoolean("particles.enabled", true)) {
            return;
        }
        
        Location location = tombstone.getLocation();
        
        // 如果已经有粒子效果，先移除
        removeParticleEffect(location);
        
        // 创建新的粒子效果任务
        BukkitTask task = new ParticleTask(location).runTaskTimer(
            plugin, 
            0, 
            configManager.getInt("particles.interval", 20)
        );
        
        particleTasks.put(location, task);
    }
    
    /**
     * 移除墓碑粒子效果
     * 统一的粒子效果移除方法
     * 
     * @param location 墓碑位置
     */
    public void removeParticleEffect(@NotNull Location location) {
        BukkitTask task = particleTasks.remove(location);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * 创建引导粒子效果
     * 统一的引导效果创建方法
     * 
     * @param player 玩家
     * @param targetLocation 目标位置
     */
    public void createGuidanceEffect(@NotNull Player player, @NotNull Location targetLocation) {
        if (!configManager.getBoolean("notifications.visual-guidance", true)) {
            return;
        }
        
        Location playerLocation = player.getLocation();
        double distance = LocationUtil.getDistance(playerLocation, targetLocation);
        int guidanceRange = configManager.getInt("notifications.guidance-range", 50);
        
        // 检查距离是否在引导范围内
        if (distance < 0 || distance > guidanceRange) {
            return;
        }
        
        // 创建引导粒子线
        createParticleLine(playerLocation, targetLocation, player);
    }
    
    /**
     * 创建粒子线
     * 统一的粒子线创建方法
     * 
     * @param start 起始位置
     * @param end 结束位置
     * @param player 观看的玩家
     */
    private void createParticleLine(@NotNull Location start, @NotNull Location end, @NotNull Player player) {
        if (!start.getWorld().equals(end.getWorld())) {
            return;
        }
        
        double distance = start.distance(end);
        int points = Math.min((int) distance * 2, 50); // 限制粒子数量
        
        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            Location particleLocation = start.clone().add(
                (end.getX() - start.getX()) * ratio,
                (end.getY() - start.getY()) * ratio + 1, // 稍微抬高
                (end.getZ() - start.getZ()) * ratio
            );
            
            // 只对指定玩家显示粒子
            player.spawnParticle(
                getParticleType(),
                particleLocation,
                1,
                0, 0, 0,
                0
            );
        }
    }
    
    /**
     * 获取配置的粒子类型
     * 统一的粒子类型获取方法
     * 
     * @return 粒子类型
     */
    @NotNull
    private Particle getParticleType() {
        String particleName = configManager.getString("particles.type", "SOUL");
        
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的粒子类型: " + particleName + "，使用默认的SOUL");
            return Particle.SOUL;
        }
    }
    
    /**
     * 关闭粒子效果系统
     * 统一的关闭方法
     */
    public void shutdown() {
        // 取消所有粒子任务
        for (BukkitTask task : particleTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        particleTasks.clear();
    }
    
    /**
     * 检查位置是否有粒子效果
     * 统一的存在检查方法
     * 
     * @param location 位置
     * @return 是否存在粒子效果
     */
    public boolean hasParticleEffect(@NotNull Location location) {
        return particleTasks.containsKey(location);
    }
    
    /**
     * 获取活跃粒子效果数量
     * 统一的数量获取方法
     * 
     * @return 粒子效果数量
     */
    public int getActiveEffectCount() {
        return particleTasks.size();
    }
    
    /**
     * 粒子效果任务类
     * 统一的粒子任务处理
     */
    private class ParticleTask extends BukkitRunnable {
        
        private final Location location;
        
        public ParticleTask(@NotNull Location location) {
            this.location = location.clone().add(0.5, 1, 0.5); // 中心位置并稍微抬高
        }
        
        @Override
        public void run() {
            if (location.getWorld() == null) {
                cancel();
                return;
            }
            
            // 检查是否还启用粒子效果
            if (!configManager.getBoolean("particles.enabled", true)) {
                cancel();
                return;
            }
            
            // 生成粒子效果
            Particle particleType = getParticleType();
            int count = configManager.getInt("particles.count", 5);
            double range = configManager.getDouble("particles.range", 0.5);
            
            location.getWorld().spawnParticle(
                particleType,
                location,
                count,
                range, range, range,
                0
            );
        }
    }
}
