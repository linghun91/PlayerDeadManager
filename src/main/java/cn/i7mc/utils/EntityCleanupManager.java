package cn.i7mc.utils;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.MessageManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体清理管理器 - 统一处理墓碑相关实体的清理和管理
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class EntityCleanupManager {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final NamespacedKey tombstoneKey;
    private final NamespacedKey hologramKey;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param configManager 配置管理器
     * @param messageManager 消息管理器
     */
    public EntityCleanupManager(@NotNull PlayerDeadManager plugin,
                               @NotNull ConfigManager configManager,
                               @NotNull MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.tombstoneKey = new NamespacedKey(plugin, "tombstone_id");
        this.hologramKey = new NamespacedKey(plugin, "pdm_hologram");
    }
    
    /**
     * 清理所有残留的墓碑实体
     * 统一的实体清理方法
     */
    public void cleanupAllTombstoneEntities() {
        int cleanedBlocks = 0;
        int cleanedHolograms = 0;

        // 清理所有世界中的残留实体
        for (World world : plugin.getServer().getWorlds()) {
            cleanedBlocks += cleanupTombstoneBlocks(world);
            cleanedHolograms += cleanupHologramEntities(world);
        }

        // 输出清理结果
        if (cleanedBlocks > 0 || cleanedHolograms > 0) {
            // 使用消息管理器输出日志消息
            java.util.Map<String, String> placeholders = messageManager.createPlaceholders();
            placeholders.put("blocks", String.valueOf(cleanedBlocks));
            placeholders.put("holograms", String.valueOf(cleanedHolograms));

            String logMessage = messageManager.getMessage("logs.cleanup.entity-cleanup-completed", placeholders);
            if (logMessage != null) {
                plugin.getLogger().info(logMessage);
            } else {
                plugin.getLogger().info("实体清理完成 - 清理方块: " + cleanedBlocks + " 个，清理全息图: " + cleanedHolograms + " 个");
            }
        }
    }
    
    /**
     * 清理指定世界中的墓碑方块
     * 统一的方块清理方法
     *
     * @param world 世界
     * @return 清理的方块数量
     */
    private int cleanupTombstoneBlocks(@NotNull World world) {
        int cleanedCount = 0;
        List<Block> blocksToClean = new ArrayList<>();

        // 遍历世界中的所有加载区块
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            try {
                // 安全获取区块的方块实体，避免异步访问错误
                org.bukkit.block.BlockState[] tileEntities = chunk.getTileEntities();
                for (org.bukkit.block.BlockState tileEntity : tileEntities) {
                    if (tileEntity instanceof TileState tileState) {
                        try {
                            // 检查是否有墓碑标记
                            if (tileState.getPersistentDataContainer().has(tombstoneKey, PersistentDataType.LONG)) {
                                blocksToClean.add(tileState.getBlock());
                            }
                        } catch (Exception e) {
                            // 忽略单个方块状态访问错误，继续处理其他方块
                            plugin.getLogger().warning("访问方块状态时发生错误，跳过该方块: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略区块访问错误，继续处理其他区块
                plugin.getLogger().warning("访问区块 " + chunk.getX() + "," + chunk.getZ() + " 时发生错误: " + e.getMessage());
            }
        }

        // 清理找到的方块
        for (Block block : blocksToClean) {
            try {
                block.setType(Material.AIR);
                cleanedCount++;
            } catch (Exception e) {
                plugin.getLogger().warning("清理方块时发生错误: " + e.getMessage());
            }
        }

        return cleanedCount;
    }
    
    /**
     * 清理指定世界中的全息图实体
     * 统一的全息图清理方法
     * 
     * @param world 世界
     * @return 清理的全息图数量
     */
    private int cleanupHologramEntities(@NotNull World world) {
        int cleanedCount = 0;
        List<ArmorStand> armorStandsToRemove = new ArrayList<>();
        
        // 遍历世界中的所有实体
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ArmorStand armorStand) {
                // 检查是否有PDM全息图标记
                if (armorStand.getPersistentDataContainer().has(hologramKey, PersistentDataType.STRING)) {
                    armorStandsToRemove.add(armorStand);
                }
            }
        }
        
        // 移除找到的盔甲架
        for (ArmorStand armorStand : armorStandsToRemove) {
            armorStand.remove();
            cleanedCount++;
        }
        
        return cleanedCount;
    }
    
    /**
     * 检查方块是否为墓碑方块
     * 统一的墓碑方块检查方法
     *
     * @param block 方块
     * @return 是否为墓碑方块
     */
    public boolean isTombstoneBlock(@NotNull Block block) {
        try {
            org.bukkit.block.BlockState state = block.getState();

            if (state instanceof TileState tileState) {
                return tileState.getPersistentDataContainer().has(tombstoneKey, PersistentDataType.LONG);
            }
        } catch (Exception e) {
            // 忽略方块状态访问错误，返回false
            plugin.getLogger().warning("检查墓碑方块时发生错误: " + e.getMessage());
        }

        return false;
    }
    
    /**
     * 检查实体是否为PDM全息图
     * 统一的全息图实体检查方法
     * 
     * @param entity 实体
     * @return 是否为PDM全息图
     */
    public boolean isPDMHologram(@NotNull Entity entity) {
        if (!(entity instanceof ArmorStand armorStand)) {
            return false;
        }
        
        return armorStand.getPersistentDataContainer().has(hologramKey, PersistentDataType.STRING);
    }
    
    /**
     * 获取墓碑方块的ID
     * 统一的墓碑ID获取方法
     *
     * @param block 方块
     * @return 墓碑ID，不存在返回null
     */
    public Long getTombstoneId(@NotNull Block block) {
        try {
            org.bukkit.block.BlockState state = block.getState();

            if (state instanceof TileState tileState) {
                return tileState.getPersistentDataContainer().get(tombstoneKey, PersistentDataType.LONG);
            }
        } catch (Exception e) {
            // 忽略方块状态访问错误，返回null
            plugin.getLogger().warning("获取墓碑ID时发生错误: " + e.getMessage());
        }

        return null;
    }
    
    /**
     * 获取全息图的墓碑ID
     * 统一的全息图墓碑ID获取方法
     * 
     * @param armorStand 盔甲架实体
     * @return 墓碑ID，不存在返回null
     */
    public Long getHologramTombstoneId(@NotNull ArmorStand armorStand) {
        String idString = armorStand.getPersistentDataContainer().get(hologramKey, PersistentDataType.STRING);
        
        if (idString != null) {
            try {
                return Long.parseLong(idString);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("无效的全息图墓碑ID: " + idString);
            }
        }
        
        return null;
    }
    
    /**
     * 清理指定位置的墓碑实体
     * 统一的位置清理方法
     *
     * @param location 位置
     */
    public void cleanupTombstoneEntitiesAt(@NotNull Location location) {
        try {
            // 清理方块
            Block block = location.getBlock();
            if (isTombstoneBlock(block)) {
                block.setType(Material.AIR);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("清理位置 " + location + " 的墓碑方块时发生错误: " + e.getMessage());
        }

        try {
            // 清理附近的全息图实体
            World world = location.getWorld();
            if (world != null) {
                double radius = 5.0; // 搜索半径
                for (Entity entity : world.getNearbyEntities(location, radius, radius, radius)) {
                    if (isPDMHologram(entity)) {
                        entity.remove();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("清理位置 " + location + " 的全息图实体时发生错误: " + e.getMessage());
        }
    }

    /**
     * 清理指定墓碑ID的全息图实体
     * 统一的墓碑ID清理方法
     *
     * @param tombstoneId 墓碑ID
     * @param world 世界（可选，为null时搜索所有世界）
     * @return 清理的全息图数量
     */
    public int cleanupHologramsByTombstoneId(long tombstoneId, @Nullable World world) {
        int cleanedCount = 0;
        List<World> worldsToSearch = new ArrayList<>();

        if (world != null) {
            worldsToSearch.add(world);
        } else {
            worldsToSearch.addAll(plugin.getServer().getWorlds());
        }

        for (World searchWorld : worldsToSearch) {
            List<ArmorStand> armorStandsToRemove = new ArrayList<>();

            // 遍历世界中的所有实体
            for (Entity entity : searchWorld.getEntities()) {
                if (entity instanceof ArmorStand armorStand) {
                    // 检查是否有PDM全息图标记且墓碑ID匹配
                    Long hologramTombstoneId = getHologramTombstoneId(armorStand);
                    if (hologramTombstoneId != null && hologramTombstoneId == tombstoneId) {
                        armorStandsToRemove.add(armorStand);
                    }
                }
            }

            // 移除找到的盔甲架
            for (ArmorStand armorStand : armorStandsToRemove) {
                try {
                    armorStand.remove();
                    cleanedCount++;
                } catch (Exception e) {
                    plugin.getLogger().warning("移除墓碑ID " + tombstoneId + " 的全息图时发生错误: " + e.getMessage());
                }
            }
        }

        return cleanedCount;
    }
    
    /**
     * 获取墓碑标记的NamespacedKey
     * 统一的Key获取方法
     * 
     * @return 墓碑标记Key
     */
    @NotNull
    public NamespacedKey getTombstoneKey() {
        return tombstoneKey;
    }
    
    /**
     * 获取全息图标记的NamespacedKey
     * 统一的Key获取方法
     * 
     * @return 全息图标记Key
     */
    @NotNull
    public NamespacedKey getHologramKey() {
        return hologramKey;
    }
}
