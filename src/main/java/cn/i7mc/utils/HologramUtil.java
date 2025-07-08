package cn.i7mc.utils;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.tombstones.PlayerTombstone;
import cn.i7mc.utils.TimeUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全息图工具类 - 统一处理全息图显示相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class HologramUtil {

    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final Map<Location, List<ArmorStand>> holograms;
    private final NamespacedKey hologramKey;
    private BukkitRunnable updateTask;
    private cn.i7mc.managers.TombstoneManager tombstoneManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param configManager 配置管理器
     * @param messageManager 消息管理器
     */
    public HologramUtil(@NotNull PlayerDeadManager plugin,
                       @NotNull ConfigManager configManager,
                       @NotNull MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.holograms = new HashMap<>();
        this.hologramKey = new NamespacedKey(plugin, "pdm_hologram");

        startUpdateTask();
    }

    /**
     * 设置墓碑管理器引用
     * 统一的管理器设置方法
     *
     * @param tombstoneManager 墓碑管理器
     */
    public void setTombstoneManager(@NotNull cn.i7mc.managers.TombstoneManager tombstoneManager) {
        this.tombstoneManager = tombstoneManager;
    }
    
    /**
     * 创建墓碑全息图
     * 统一的全息图创建方法
     * 
     * @param tombstone 墓碑实例
     */
    public void createHologram(@NotNull PlayerTombstone tombstone) {
        if (!configManager.getBoolean("hologram.enabled", true)) {
            return;
        }
        
        Location location = tombstone.getLocation();
        double heightOffset = configManager.getDouble("hologram.height-offset", 1.5);
        Location hologramLocation = location.clone().add(0.5, heightOffset, 0.5);
        
        // 获取全息图内容
        List<String> lines = getHologramLines(tombstone);
        List<ArmorStand> armorStands = new ArrayList<>();
        
        // 创建每一行的盔甲架
        for (int i = 0; i < lines.size(); i++) {
            Location lineLocation = hologramLocation.clone().subtract(0, i * 0.25, 0);
            ArmorStand armorStand = createHologramLine(lineLocation, lines.get(i), tombstone.getTombstoneId());
            if (armorStand != null) {
                armorStands.add(armorStand);
            }
        }
        
        // 存储全息图
        holograms.put(location, armorStands);
    }
    
    /**
     * 移除墓碑全息图
     * 统一的全息图移除方法
     *
     * @param location 墓碑位置
     */
    public void removeHologram(@NotNull Location location) {
        List<ArmorStand> armorStands = holograms.remove(location);
        if (armorStands != null) {
            for (ArmorStand armorStand : armorStands) {
                if (armorStand != null && !armorStand.isDead()) {
                    try {
                        armorStand.remove();
                    } catch (Exception e) {
                        plugin.getLogger().warning("移除全息图盔甲架时发生错误: " + e.getMessage());
                    }
                }
            }
        }

        // 额外安全检查：如果位置仍有残留的全息图实体，尝试清理
        if (location.getWorld() != null) {
            try {
                double radius = 2.0; // 小范围搜索
                for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
                    if (entity instanceof org.bukkit.entity.ArmorStand armorStand) {
                        // 检查是否有PDM全息图标记
                        if (armorStand.getPersistentDataContainer().has(hologramKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                            try {
                                armorStand.remove();
                            } catch (Exception e) {
                                plugin.getLogger().warning("清理残留全息图实体时发生错误: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("搜索残留全息图实体时发生错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 更新墓碑全息图
     * 统一的全息图更新方法
     * 
     * @param tombstone 墓碑实例
     */
    public void updateHologram(@NotNull PlayerTombstone tombstone) {
        Location location = tombstone.getLocation();
        List<ArmorStand> armorStands = holograms.get(location);
        
        if (armorStands == null || armorStands.isEmpty()) {
            return;
        }
        
        // 获取更新后的内容
        List<String> lines = getHologramLines(tombstone);
        
        // 更新每一行
        for (int i = 0; i < Math.min(lines.size(), armorStands.size()); i++) {
            ArmorStand armorStand = armorStands.get(i);
            if (armorStand != null && !armorStand.isDead()) {
                armorStand.setCustomName(lines.get(i));
            }
        }
    }
    
    /**
     * 创建单行全息图
     * 统一的单行创建方法
     *
     * @param location 位置
     * @param text 文本
     * @param tombstoneId 墓碑ID
     * @return 盔甲架实体
     */
    @Nullable
    private ArmorStand createHologramLine(@NotNull Location location, @NotNull String text, long tombstoneId) {
        if (location.getWorld() == null) {
            return null;
        }

        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

        // 设置盔甲架属性
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(true);
        armorStand.setCustomName(text);
        armorStand.setMarker(true);
        armorStand.setSmall(true);
        armorStand.setInvulnerable(true);

        // 添加PDM全息图标记
        armorStand.getPersistentDataContainer().set(hologramKey, PersistentDataType.STRING, String.valueOf(tombstoneId));

        return armorStand;
    }
    
    /**
     * 获取全息图内容行
     * 统一的内容获取方法
     * 
     * @param tombstone 墓碑实例
     * @return 内容行列表
     */
    @NotNull
    private List<String> getHologramLines(@NotNull PlayerTombstone tombstone) {
        List<String> configLines = configManager.getStringList("hologram.format");
        List<String> processedLines = new ArrayList<>();
        
        // 创建占位符
        Map<String, String> placeholders = messageManager.createPlaceholders();
        
        // 添加墓碑相关占位符
        String ownerName = plugin.getServer().getOfflinePlayer(tombstone.getPlayerId()).getName();
        if (ownerName == null) {
            String unknownPlayer = messageManager.getMessage("time.unknown-player", null);
            ownerName = unknownPlayer != null ? unknownPlayer : "未知玩家";
        }

        placeholders.put("player", ownerName);
        placeholders.put("time", TimeUtil.formatTimestampSimple(tombstone.getDeathTime()));
        placeholders.put("protection", TimeUtil.formatRemainingTime(
            TimeUtil.getRemainingTime(tombstone.getProtectionExpire()), messageManager
        ));

        // 处理每一行
        for (String line : configLines) {
            // 手动替换占位符
            String processedLine = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                processedLine = processedLine.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            // 处理颜色代码
            processedLine = org.bukkit.ChatColor.translateAlternateColorCodes('&', processedLine);
            processedLines.add(processedLine);
        }
        
        return processedLines;
    }
    
    /**
     * 启动更新任务
     * 统一的任务启动方法
     */
    private void startUpdateTask() {
        int updateInterval = configManager.getInt("hologram.update-interval", 5) * 20; // 转换为tick
        
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllHolograms();
            }
        };
        
        updateTask.runTaskTimer(plugin, updateInterval, updateInterval);
    }
    
    /**
     * 更新所有全息图
     * 统一的批量更新方法
     */
    private void updateAllHolograms() {
        if (!configManager.getBoolean("hologram.enabled", true)) {
            return;
        }

        if (tombstoneManager == null) {
            return;
        }

        // 获取所有活跃墓碑并更新全息图
        Map<Location, PlayerTombstone> activeTombstones = tombstoneManager.getActiveTombstones();
        for (Map.Entry<Location, PlayerTombstone> entry : activeTombstones.entrySet()) {
            Location location = entry.getKey();
            PlayerTombstone tombstone = entry.getValue();

            // 只更新存在全息图的墓碑
            if (holograms.containsKey(location)) {
                updateHologram(tombstone);
            }
        }
    }
    
    /**
     * 关闭全息图系统
     * 统一的关闭方法
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        
        // 移除所有全息图
        for (List<ArmorStand> armorStands : holograms.values()) {
            for (ArmorStand armorStand : armorStands) {
                if (armorStand != null && !armorStand.isDead()) {
                    armorStand.remove();
                }
            }
        }
        
        holograms.clear();
    }
    
    /**
     * 检查位置是否有全息图
     * 统一的存在检查方法
     * 
     * @param location 位置
     * @return 是否存在全息图
     */
    public boolean hasHologram(@NotNull Location location) {
        return holograms.containsKey(location);
    }
    
    /**
     * 获取全息图数量
     * 统一的数量获取方法
     * 
     * @return 全息图数量
     */
    public int getHologramCount() {
        return holograms.size();
    }
}
