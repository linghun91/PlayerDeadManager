package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.tombstones.PlayerTombstone;
import cn.i7mc.utils.EntityCleanupManager;
import cn.i7mc.utils.HologramUtil;
import cn.i7mc.utils.ParticleUtil;
import cn.i7mc.utils.TimeUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
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
    private final DataManager dataManager;
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
                           @NotNull MessageManager messageManager, @NotNull DataManager dataManager) {
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

            // 计算保护过期时间和消失时间（支持VIP权限时间）
            long currentTime = System.currentTimeMillis();
            VipTimeManager vipTimeManager = plugin.getVipTimeManager();

            long protectionMinutes = vipTimeManager.getProtectionTime(player);
            long protectionDuration = TimeUtil.minutesToMillis(protectionMinutes);
            long protectionExpire = currentTime + protectionDuration;

            long despawnHours = vipTimeManager.getDespawnTime(player);
            long despawnDuration = TimeUtil.hoursToMillis(despawnHours);
            long despawnTime = currentTime + despawnDuration;

            // 保存到数据库
            long tombstoneId = dataManager.saveTombstone(
                player.getUniqueId(),
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                currentTime,
                protectionExpire,
                despawnTime,
                experience,
                items
            );
            
            // 创建墓碑实例
            PlayerTombstone tombstone = new PlayerTombstone(
                player.getUniqueId(),
                location,
                currentTime,
                protectionExpire,
                despawnTime,
                experience,
                tombstoneId
            );
            
            // 放置墓碑方块
            placeTombstoneBlock(location, tombstoneId, player.getUniqueId());
            
            // 添加到活跃墓碑列表
            activeTombstones.put(location, tombstone);

            // 创建全息图和粒子效果
            hologramUtil.createHologram(tombstone);
            tombstone.setHasHologram(true);

            particleUtil.createParticleEffect(tombstone);
            tombstone.setHasParticles(true);

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
        List<DataManager.TombstoneData> playerTombstones = getPlayerTombstones(player.getUniqueId());

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
    private boolean removeOldestTombstone(@NotNull Player player, @NotNull List<DataManager.TombstoneData> tombstones) {
        if (tombstones.isEmpty()) {
            return false;
        }

        // 找到最旧的墓碑
        DataManager.TombstoneData oldestTombstone = tombstones.stream()
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
     * @param playerUuid 玩家UUID（用于设置头颅皮肤）
     */
    private void placeTombstoneBlock(@NotNull Location location, long tombstoneId, @NotNull UUID playerUuid) {
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

            // 如果是玩家头颅，设置玩家皮肤
            if (state instanceof org.bukkit.block.Skull skull && tombstoneMaterial == Material.PLAYER_HEAD) {
                try {
                    // 使用Paper API设置头颅所有者
                    OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerUuid);
                    skull.setOwningPlayer(offlinePlayer);
                } catch (Exception e) {
                    plugin.getLogger().warning("设置头颅皮肤失败: " + e.getMessage());
                }
            }

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

        return removeTombstoneInternal(location, tombstone, true);
    }

    /**
     * 内部墓碑移除方法
     * 统一的内部移除逻辑，支持数据库操作控制
     *
     * @param location 墓碑位置
     * @param tombstone 墓碑实例
     * @param deleteFromDatabase 是否从数据库删除
     * @return 是否成功移除
     */
    private boolean removeTombstoneInternal(@NotNull Location location,
                                          @NotNull PlayerTombstone tombstone,
                                          boolean deleteFromDatabase) {
        boolean success = true;
        List<String> errors = new ArrayList<>();

        try {
            // 1. 先从活跃列表移除，避免并发问题
            activeTombstones.remove(location);

            // 2. 移除全息图（使用try-catch确保即使失败也继续清理）
            if (tombstone.hasHologram()) {
                try {
                    hologramUtil.removeHologram(location);
                    tombstone.setHasHologram(false);
                } catch (Exception e) {
                    errors.add("移除全息图失败: " + e.getMessage());
                    success = false;
                }
            }

            // 3. 额外的全息图清理保障（通过墓碑ID清理）
            try {
                int cleanedHolograms = entityCleanupManager.cleanupHologramsByTombstoneId(
                    tombstone.getTombstoneId(), location.getWorld());
                if (cleanedHolograms > 0) {
                    plugin.getLogger().info("额外清理了 " + cleanedHolograms + " 个残留全息图实体");
                    tombstone.setHasHologram(false);
                }
            } catch (Exception e) {
                errors.add("额外全息图清理失败: " + e.getMessage());
            }

            // 4. 移除粒子效果
            if (tombstone.hasParticles()) {
                try {
                    particleUtil.removeParticleEffect(location);
                    tombstone.setHasParticles(false);
                } catch (Exception e) {
                    errors.add("移除粒子效果失败: " + e.getMessage());
                    success = false;
                }
            }

            // 5. 移除方块
            try {
                tombstone.removeTombstone(); // 使用统一的墓碑移除方法
            } catch (Exception e) {
                errors.add("移除方块失败: " + e.getMessage());
                success = false;
            }

            // 6. 从数据库删除（如果需要）
            if (deleteFromDatabase) {
                try {
                    dataManager.deleteTombstone(tombstone.getTombstoneId());
                } catch (SQLException e) {
                    errors.add("数据库删除失败: " + e.getMessage());
                    success = false;
                }
            }

            // 7. 标记整体实例为已移除
            tombstone.markAsRemoved();

            // 记录错误信息
            if (!errors.isEmpty()) {
                plugin.getLogger().warning("墓碑移除过程中出现错误:");
                for (String error : errors) {
                    plugin.getLogger().warning("  - " + error);
                }
            }

            return success;

        } catch (Exception e) {
            plugin.getLogger().severe("移除墓碑时发生未预期错误: " + e.getMessage());
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
    public List<DataManager.TombstoneData> getPlayerTombstones(@NotNull UUID playerId) {
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
            List<DataManager.TombstoneData> allTombstones = dataManager.getAllTombstones();
            int restoredCount = 0;
            int expiredCount = 0;

            for (DataManager.TombstoneData tombstoneData : allTombstones) {
                // 检查墓碑是否已达到despawn-time，需要完全移除
                long currentTime = System.currentTimeMillis();

                if (currentTime > tombstoneData.despawnTime()) {
                    // 删除已达到despawn-time的墓碑
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
                    tombstoneData.despawnTime(),
                    tombstoneData.experience(),
                    tombstoneData.id()
                );

                // 恢复墓碑方块
                placeTombstoneBlock(location, tombstoneData.id(), tombstoneData.playerUuid());

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
                }

                // 同步更新活跃墓碑列表和完整性检查
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    updateActiveTombstones();
                    checkAndCleanupIncompleteTombstones();
                });

            } catch (SQLException e) {
                plugin.getLogger().severe("定时清理墓碑时数据库错误: " + e.getMessage());
                e.printStackTrace();
            }
        }, intervalTicks, intervalTicks);

        plugin.getLogger().info("定时清理任务已启动 - 间隔: " + cleanupInterval + " 小时");
    }

    /**
     * 检查并清理不完整的墓碑实例
     * 统一的完整性检查和清理方法
     */
    public void checkAndCleanupIncompleteTombstones() {
        List<Location> incompleteLocations = new ArrayList<>();

        // 检查所有活跃墓碑的完整性
        for (Map.Entry<Location, PlayerTombstone> entry : activeTombstones.entrySet()) {
            Location location = entry.getKey();
            PlayerTombstone tombstone = entry.getValue();

            // 检查墓碑实例是否完整
            if (!tombstone.isComplete()) {
                incompleteLocations.add(location);
                plugin.getLogger().info("发现不完整的墓碑实例，位置: " +
                    location.getWorld().getName() + " " + location.getBlockX() +
                    "," + location.getBlockY() + "," + location.getBlockZ());
            }
        }

        // 清理不完整的墓碑实例
        for (Location location : incompleteLocations) {
            PlayerTombstone tombstone = activeTombstones.get(location);
            if (tombstone != null) {
                try {
                    removeTombstoneInternal(location, tombstone, true);
                    plugin.getLogger().info("已清理不完整的墓碑实例");
                } catch (Exception e) {
                    plugin.getLogger().warning("清理不完整墓碑实例时发生错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 更新活跃墓碑列表
     * 统一的活跃墓碑更新方法
     */
    private void updateActiveTombstones() {
        // 收集需要移除的过期墓碑
        List<Location> expiredLocations = new ArrayList<>();

        // 先找出所有过期的墓碑位置
        for (Map.Entry<Location, PlayerTombstone> entry : activeTombstones.entrySet()) {
            Location location = entry.getKey();
            PlayerTombstone tombstone = entry.getValue();

            // 使用墓碑实例的精确despawn检查方法
            if (tombstone.shouldDespawn() || !tombstone.isComplete()) {
                expiredLocations.add(location);
            }
        }

        // 逐个安全移除过期墓碑
        for (Location location : expiredLocations) {
            PlayerTombstone tombstone = activeTombstones.get(location);
            if (tombstone != null) {
                try {
                    // 使用统一的移除方法，确保数据库同步
                    removeTombstoneInternal(location, tombstone, false);
                } catch (Exception e) {
                    plugin.getLogger().warning("移除过期墓碑时发生错误，位置: " +
                        location.getWorld().getName() + " " + location.getBlockX() +
                        "," + location.getBlockY() + "," + location.getBlockZ() +
                        " - " + e.getMessage());

                    // 即使出错也要尝试清理实体
                    try {
                        entityCleanupManager.cleanupTombstoneEntitiesAt(location);
                        activeTombstones.remove(location);
                    } catch (Exception cleanupError) {
                        plugin.getLogger().severe("强制清理墓碑实体失败: " + cleanupError.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 获取实体清理管理器
     * 统一的管理器获取方法
     *
     * @return 实体清理管理器
     */
    @NotNull
    public EntityCleanupManager getEntityCleanupManager() {
        return entityCleanupManager;
    }
}
