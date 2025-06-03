package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.abstracts.AbstractDataManager;
import cn.i7mc.tombstones.PlayerTombstone;
import cn.i7mc.utils.EntityCleanupManager;
import cn.i7mc.utils.HologramUtil;
import cn.i7mc.utils.ParticleUtil;
import cn.i7mc.utils.TimeUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 墓碑管理器 - 统一处理墓碑创建、管理和移除
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class TombstoneManager {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final AbstractDataManager dataManager;
    private final HologramUtil hologramUtil;
    private final ParticleUtil particleUtil;
    private final EntityCleanupManager entityCleanupManager;
    private final Map<Location, PlayerTombstone> activeTombstones;
    private final NamespacedKey tombstoneKey;
    private BukkitTask cleanupTask;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param configManager 配置管理器
     * @param messageManager 消息管理器
     * @param dataManager 数据管理器
     */
    public TombstoneManager(@NotNull PlayerDeadManager plugin, @NotNull ConfigManager configManager,
                           @NotNull MessageManager messageManager, @NotNull AbstractDataManager dataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.dataManager = dataManager;
        this.hologramUtil = new HologramUtil(plugin, configManager, messageManager);
        this.particleUtil = new ParticleUtil(plugin, configManager);
        this.entityCleanupManager = new EntityCleanupManager(plugin, configManager, messageManager);
        this.activeTombstones = new HashMap<>();
        this.tombstoneKey = new NamespacedKey(plugin, "tombstone_id");

        // 设置HologramUtil的TombstoneManager引用
        this.hologramUtil.setTombstoneManager(this);
    }
    
    /**
     * 创建墓碑
     * 统一的墓碑创建方法
     * 
     * @param player 死亡玩家
     * @param location 墓碑位置
     * @param items 物品数组
     * @param experience 经验值
     * @return 创建的墓碑实例，失败返回null
     */
    @Nullable
    public PlayerTombstone createTombstone(@NotNull Player player, @NotNull Location location,
                                         @NotNull ItemStack[] items, int experience) {
        try {
            // 检查墓碑数量限制
            if (!checkTombstoneLimit(player)) {
                return null;
            }

            // 检查位置是否可以放置墓碑
            if (!canPlaceTombstone(location)) {
                // 寻找附近可用位置
                location = findNearbyLocation(location);
                if (location == null) {
                    Map<String, String> placeholders = messageManager.createPlaceholders();
                    messageManager.addPlayerPlaceholders(placeholders, player);
                    messageManager.sendMessage(player, "tombstone.no-space", placeholders);
                    return null;
                }
            }

            // 计算保护过期时间
            long currentTime = System.currentTimeMillis();
            long protectionMinutes = configManager.getLong("tombstone.protection-time", 60); // 默认60分钟
            long protectionDuration = TimeUtil.minutesToMillis(protectionMinutes);
            long protectionExpire = currentTime + protectionDuration;
            
            // 保存到数据库
            long tombstoneId = dataManager.saveTombstone(
                player.getUniqueId(),
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                currentTime,
                protectionExpire,
                experience,
                items
            );
            
            // 创建墓碑实例
            PlayerTombstone tombstone = new PlayerTombstone(
                player.getUniqueId(),
                location,
                currentTime,
                protectionExpire,
                experience,
                tombstoneId
            );
            
            // 放置墓碑方块
            placeTombstoneBlock(location, tombstoneId);
            
            // 添加到活跃墓碑列表
            activeTombstones.put(location, tombstone);

            // 创建全息图和粒子效果
            hologramUtil.createHologram(tombstone);
            particleUtil.createParticleEffect(tombstone);

            // 发送成功消息
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.addLocationPlaceholders(placeholders,
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
            messageManager.sendMessage(player, "tombstone.created", placeholders);

            return tombstone;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("创建墓碑时数据库错误: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "tombstone.creation-failed", placeholders);
            return null;
        }
    }
    
    /**
     * 检查玩家墓碑数量限制
     * 统一的数量限制检查方法
     *
     * @param player 玩家
     * @return 是否可以创建新墓碑
     */
    private boolean checkTombstoneLimit(@NotNull Player player) {
        int maxTombstones = configManager.getInt("tombstone.max-tombstones", 3);

        // 获取玩家当前墓碑数量
        List<AbstractDataManager.TombstoneData> playerTombstones = getPlayerTombstones(player.getUniqueId());

        if (playerTombstones.size() >= maxTombstones) {
            // 尝试删除最旧的墓碑
            if (!removeOldestTombstone(player, playerTombstones)) {
                // 发送限制消息
                Map<String, String> placeholders = messageManager.createPlaceholders();
                messageManager.addPlayerPlaceholders(placeholders, player);
                placeholders.put("max_tombstones", String.valueOf(maxTombstones));
                placeholders.put("current_tombstones", String.valueOf(playerTombstones.size()));
                messageManager.sendMessage(player, "tombstone.limit-reached", placeholders);
                return false;
            }
        }

        return true;
    }

    /**
     * 移除最旧的墓碑
     * 统一的旧墓碑移除方法
     *
     * @param player 玩家
     * @param tombstones 墓碑列表
     * @return 是否成功移除
     */
    private boolean removeOldestTombstone(@NotNull Player player, @NotNull List<AbstractDataManager.TombstoneData> tombstones) {
        if (tombstones.isEmpty()) {
            return false;
        }

        // 找到最旧的墓碑
        AbstractDataManager.TombstoneData oldestTombstone = tombstones.stream()
            .min((t1, t2) -> Long.compare(t1.deathTime(), t2.deathTime()))
            .orElse(null);

        if (oldestTombstone == null) {
            return false;
        }

        try {
            // 从数据库删除
            dataManager.deleteTombstone(oldestTombstone.id());

            // 从活跃列表移除
            Location location = new Location(
                plugin.getServer().getWorld(oldestTombstone.worldName()),
                oldestTombstone.x(),
                oldestTombstone.y(),
                oldestTombstone.z()
            );

            if (location.getWorld() != null) {
                activeTombstones.remove(location);
                // 移除方块
                location.getBlock().setType(Material.AIR);
            }

            // 发送移除消息
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.addLocationPlaceholders(placeholders,
                oldestTombstone.worldName(),
                oldestTombstone.x(),
                oldestTombstone.y(),
                oldestTombstone.z());
            messageManager.sendMessage(player, "tombstone.oldest-removed", placeholders);

            return true;

        } catch (SQLException e) {
            plugin.getLogger().severe("移除最旧墓碑时数据库错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查位置是否可以放置墓碑
     * 统一的位置检查方法
     *
     * @param location 要检查的位置
     * @return 是否可以放置
     */
    private boolean canPlaceTombstone(@NotNull Location location) {
        Block block = location.getBlock();
        Material blockType = block.getType();
        
        // 检查是否为空气或可替换方块
        return blockType.isAir() || 
               blockType == Material.WATER || 
               blockType == Material.LAVA ||
               blockType == Material.TALL_GRASS ||
               blockType == Material.GRASS ||
               blockType == Material.FERN ||
               blockType == Material.DEAD_BUSH;
    }
    
    /**
     * 寻找附近可用位置
     * 统一的位置搜索方法
     * 
     * @param originalLocation 原始位置
     * @return 可用位置，未找到返回null
     */
    @Nullable
    private Location findNearbyLocation(@NotNull Location originalLocation) {
        int searchRadius = configManager.getInt("tombstone.search-radius", 5);
        
        // 在原位置周围搜索
        for (int y = 0; y <= searchRadius; y++) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    Location testLocation = originalLocation.clone().add(x, y, z);
                    if (canPlaceTombstone(testLocation)) {
                        return testLocation;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 放置墓碑方块
     * 统一的方块放置方法
     *
     * @param location 位置
     * @param tombstoneId 墓碑ID
     */
    private void placeTombstoneBlock(@NotNull Location location, long tombstoneId) {
        Block block = location.getBlock();

        // 获取配置的墓碑方块类型
        String materialName = configManager.getString("tombstone.block-type", "CHEST");
        Material tombstoneMaterial;

        try {
            tombstoneMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的墓碑方块类型: " + materialName + "，使用默认的CHEST");
            tombstoneMaterial = Material.CHEST;
        }

        // 设置方块类型
        block.setType(tombstoneMaterial);

        // 获取方块状态并存储墓碑ID标记
        org.bukkit.block.BlockState state = block.getState();

        // 根据Paper API，TileState（包括Skull）都实现了PersistentDataHolder
        if (state instanceof org.bukkit.block.TileState tileState) {
            tileState.getPersistentDataContainer().set(tombstoneKey, PersistentDataType.LONG, tombstoneId);
            tileState.update();


        } else {
            plugin.getLogger().warning("方块状态不是TileState，无法存储PersistentData: " + state.getClass().getSimpleName() +
                " 在位置 " + location.getWorld().getName() + " " + location.getBlockX() + "," +
                location.getBlockY() + "," + location.getBlockZ());
        }
    }
    
    /**
     * 移除墓碑
     * 统一的墓碑移除方法
     *
     * @param location 墓碑位置
     * @return 是否成功移除
     */
    public boolean removeTombstone(@NotNull Location location) {
        PlayerTombstone tombstone = activeTombstones.get(location);
        if (tombstone == null) {
            return false;
        }

        try {
            // 移除全息图和粒子效果
            hologramUtil.removeHologram(location);
            particleUtil.removeParticleEffect(location);

            // 从数据库删除
            dataManager.deleteTombstone(tombstone.getTombstoneId());

            // 移除方块
            Block block = location.getBlock();
            block.setType(Material.AIR);

            // 从活跃列表移除
            activeTombstones.remove(location);

            return true;

        } catch (SQLException e) {
            plugin.getLogger().severe("移除墓碑时数据库错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除指定的墓碑实例
     * 统一的墓碑实例移除方法
     *
     * @param tombstone 墓碑实例
     * @return 是否成功移除
     */
    public boolean removeTombstone(@NotNull PlayerTombstone tombstone) {
        return removeTombstone(tombstone.getLocation());
    }

    /**
     * 通过ID移除墓碑
     * 统一的墓碑ID移除方法
     *
     * @param tombstoneId 墓碑ID
     * @return 是否成功移除
     */
    public boolean removeTombstoneById(long tombstoneId) {
        // 查找墓碑
        PlayerTombstone tombstone = getTombstoneById(tombstoneId);
        if (tombstone == null) {
            return false;
        }

        // 查找墓碑位置
        Location location = null;
        for (Map.Entry<Location, PlayerTombstone> entry : activeTombstones.entrySet()) {
            if (entry.getValue().getTombstoneId() == tombstoneId) {
                location = entry.getKey();
                break;
            }
        }

        if (location == null) {
            plugin.getLogger().warning("找到墓碑实例但无法找到位置，墓碑ID: " + tombstoneId);
            return false;
        }

        // 使用位置移除墓碑
        return removeTombstone(location);
    }
    
    /**
     * 获取位置的墓碑
     * 统一的墓碑获取方法
     *
     * @param location 位置
     * @return 墓碑实例，不存在返回null
     */
    @Nullable
    public PlayerTombstone getTombstone(@NotNull Location location) {
        return activeTombstones.get(location);
    }

    /**
     * 通过ID获取墓碑实例
     * 统一的墓碑ID查找方法
     *
     * @param tombstoneId 墓碑ID
     * @return 墓碑实例，不存在返回null
     */
    @Nullable
    public PlayerTombstone getTombstoneById(long tombstoneId) {
        return activeTombstones.values().stream()
            .filter(tombstone -> tombstone.getTombstoneId() == tombstoneId)
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取活跃墓碑数量
     * 统一的墓碑计数方法
     *
     * @return 活跃墓碑数量
     */
    public int getActiveTombstonesCount() {
        return activeTombstones.size();
    }

    /**
     * 获取所有活跃墓碑
     * 统一的活跃墓碑获取方法
     *
     * @return 活跃墓碑映射
     */
    @NotNull
    public Map<Location, PlayerTombstone> getActiveTombstones() {
        return new HashMap<>(activeTombstones);
    }
    
    /**
     * 获取墓碑ID从方块
     * 统一的ID获取方法
     *
     * @param block 方块
     * @return 墓碑ID，不存在返回null
     */
    @Nullable
    public Long getTombstoneId(@NotNull Block block) {
        org.bukkit.block.BlockState state = block.getState();

        // 根据Paper API，检查是否为TileState
        if (state instanceof org.bukkit.block.TileState tileState) {
            Long tombstoneId = tileState.getPersistentDataContainer().get(tombstoneKey, PersistentDataType.LONG);



            return tombstoneId;
        }
        return null;
    }
    
    /**
     * 加载玩家的墓碑列表
     * 统一的墓碑加载方法
     * 
     * @param playerId 玩家UUID
     * @return 墓碑数据列表
     */
    @NotNull
    public List<AbstractDataManager.TombstoneData> getPlayerTombstones(@NotNull UUID playerId) {
        try {
            return dataManager.getPlayerTombstones(playerId);
        } catch (SQLException e) {
            plugin.getLogger().severe("加载玩家墓碑时数据库错误: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    /**
     * 初始化管理器
     * 统一的初始化方法
     */
    public void initialize() {
        try {
            dataManager.initializeDatabase();

            // 清理所有残留的墓碑实体
            entityCleanupManager.cleanupAllTombstoneEntities();

            // 恢复服务器重启前的墓碑
            restoreTombstonesFromDatabase();

            // 启动定时清理任务
            startCleanupTask();

            plugin.getLogger().info("墓碑管理器初始化完成");
        } catch (SQLException e) {
            plugin.getLogger().severe("墓碑管理器初始化失败!");
            e.printStackTrace();
        }
    }

    /**
     * 从数据库恢复墓碑
     * 统一的墓碑恢复方法
     */
    private void restoreTombstonesFromDatabase() {
        try {
            List<AbstractDataManager.TombstoneData> allTombstones = dataManager.getAllTombstones();
            int restoredCount = 0;
            int expiredCount = 0;

            for (AbstractDataManager.TombstoneData tombstoneData : allTombstones) {
                // 检查墓碑是否已过期
                if (System.currentTimeMillis() > tombstoneData.protectionExpire()) {
                    // 删除过期墓碑
                    dataManager.deleteTombstone(tombstoneData.id());
                    expiredCount++;
                    continue;
                }

                // 恢复墓碑方块和数据
                Location location = new Location(
                    plugin.getServer().getWorld(tombstoneData.worldName()),
                    tombstoneData.x(),
                    tombstoneData.y(),
                    tombstoneData.z()
                );

                // 检查世界是否存在
                if (location.getWorld() == null) {
                    plugin.getLogger().warning("墓碑所在世界不存在，跳过恢复: " + tombstoneData.worldName());
                    continue;
                }

                // 创建墓碑实例
                PlayerTombstone tombstone = new PlayerTombstone(
                    tombstoneData.playerUuid(),
                    location,
                    tombstoneData.deathTime(),
                    tombstoneData.protectionExpire(),
                    tombstoneData.experience(),
                    tombstoneData.id()
                );

                // 恢复墓碑方块
                placeTombstoneBlock(location, tombstoneData.id());

                // 添加到活跃墓碑列表
                activeTombstones.put(location, tombstone);

                // 恢复全息图和粒子效果
                hologramUtil.createHologram(tombstone);
                particleUtil.createParticleEffect(tombstone);

                restoredCount++;
            }

            if (restoredCount > 0 || expiredCount > 0) {
                plugin.getLogger().info("墓碑恢复完成 - 恢复: " + restoredCount + " 个，清理过期: " + expiredCount + " 个");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("恢复墓碑时数据库错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 关闭管理器
     * 统一的关闭方法
     */
    public void shutdown() {
        // 取消定时清理任务
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        // 关闭全息图和粒子效果系统
        hologramUtil.shutdown();
        particleUtil.shutdown();

        dataManager.closeDatabase();
        activeTombstones.clear();
        plugin.getLogger().info("墓碑管理器已关闭");
    }

    /**
     * 启动定时清理任务
     * 统一的定时任务启动方法
     */
    private void startCleanupTask() {
        // 获取清理间隔配置 (小时)
        int cleanupInterval = configManager.getInt("tombstone.cleanup-interval", 1);

        // 转换为tick (1小时 = 72000 tick)
        long intervalTicks = cleanupInterval * 72000L;

        // 启动定时任务
        cleanupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                long currentTime = System.currentTimeMillis();
                int cleanedCount = dataManager.cleanupExpiredTombstones(currentTime);

                if (cleanedCount > 0) {
                    plugin.getLogger().info("定时清理完成 - 清理了 " + cleanedCount + " 个过期墓碑");

                    // 同步更新活跃墓碑列表
                    plugin.getServer().getScheduler().runTask(plugin, this::updateActiveTombstones);
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("定时清理墓碑时数据库错误: " + e.getMessage());
                e.printStackTrace();
            }
        }, intervalTicks, intervalTicks);

        plugin.getLogger().info("定时清理任务已启动 - 间隔: " + cleanupInterval + " 小时");
    }

    /**
     * 更新活跃墓碑列表
     * 统一的活跃墓碑更新方法
     */
    private void updateActiveTombstones() {
        // 移除已过期的墓碑
        activeTombstones.entrySet().removeIf(entry -> {
            Location location = entry.getKey();
            PlayerTombstone tombstone = entry.getValue();

            // 检查墓碑是否已过期 (24小时后)
            long despawnTime = configManager.getLong("tombstone.despawn-time", 24) * 60 * 60 * 1000;
            if (System.currentTimeMillis() - tombstone.getDeathTime() > despawnTime) {
                // 移除方块和效果
                location.getBlock().setType(Material.AIR);
                hologramUtil.removeHologram(location);
                particleUtil.removeParticleEffect(location);
                return true;
            }

            return false;
        });
    }
}
