package cn.i7mc.listeners;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.TombstoneManager;
import cn.i7mc.tombstones.PlayerTombstone;
import cn.i7mc.utils.TimeUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 墓碑保护监听器 - 统一处理墓碑保护相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class TombstoneProtectionListener implements Listener {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final TombstoneManager tombstoneManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param configManager 配置管理器
     * @param messageManager 消息管理器
     * @param tombstoneManager 墓碑管理器
     */
    public TombstoneProtectionListener(@NotNull PlayerDeadManager plugin,
                                     @NotNull ConfigManager configManager,
                                     @NotNull MessageManager messageManager,
                                     @NotNull TombstoneManager tombstoneManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.tombstoneManager = tombstoneManager;
    }
    
    /**
     * 处理方块破坏事件
     * 统一的墓碑保护方法
     *
     * @param event 方块破坏事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();

        // 检查是否为墓碑方块
        Long tombstoneId = tombstoneManager.getTombstoneId(block);
        if (tombstoneId == null) {
            return;
        }

        // 通过ID获取墓碑
        PlayerTombstone tombstone = tombstoneManager.getTombstoneById(tombstoneId);
        if (tombstone == null) {
            return;
        }

        Player player = event.getPlayer();

        // 检查是否有管理员权限
        if (hasAdminPermission(player)) {
            return;
        }

        // 检查是否是墓碑所有者
        if (tombstone.getPlayerId().equals(player.getUniqueId())) {
            return;
        }

        // 墓碑头颅永远不允许被其他玩家破坏，只能由插件自动清理
        // 取消破坏事件并发送消息
        event.setCancelled(true);
        sendProtectionMessage(player, tombstone);
    }
    
    /**
     * 处理方块放置事件
     * 统一的墓碑位置保护方法
     *
     * @param event 方块放置事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        Block block = event.getBlock();

        // 检查是否在墓碑位置放置方块
        Location location = block.getLocation();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(location);

        if (tombstone == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 检查是否有管理员权限
        if (hasAdminPermission(player)) {
            return;
        }
        
        // 取消放置事件并发送消息
        event.setCancelled(true);
        sendProtectionMessage(player, tombstone);
    }
    
    /**
     * 处理实体爆炸事件
     * 统一的爆炸保护方法
     * 
     * @param event 实体爆炸事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        // 移除爆炸影响的墓碑方块
        event.blockList().removeIf(block -> {
            PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());
            if (tombstone != null) {
                return true; // 移除此方块，墓碑永远不被爆炸破坏
            }
            return false;
        });
    }
    
    /**
     * 处理方块爆炸事件
     * 统一的方块爆炸保护方法
     * 
     * @param event 方块爆炸事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        // 移除爆炸影响的墓碑方块
        event.blockList().removeIf(block -> {
            PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());
            if (tombstone != null) {
                return true; // 移除此方块，墓碑永远不被爆炸破坏
            }
            return false;
        });
    }
    
    /**
     * 检查玩家是否有管理员权限
     * 统一的权限检查方法
     * 
     * @param player 玩家
     * @return 是否有管理员权限
     */
    private boolean hasAdminPermission(@NotNull Player player) {
        String adminPermission = configManager.getString("permissions.admin", "playerdeadmanager.admin");
        return player.hasPermission(adminPermission);
    }
    
    /**
     * 发送保护消息
     * 统一的保护消息发送方法
     * 
     * @param player 玩家
     * @param tombstone 墓碑
     */
    private void sendProtectionMessage(@NotNull Player player, @NotNull PlayerTombstone tombstone) {
        Map<String, String> placeholders = messageManager.createPlaceholders();
        messageManager.addPlayerPlaceholders(placeholders, player);

        // 添加墓碑相关占位符
        String ownerName = plugin.getServer().getOfflinePlayer(tombstone.getPlayerId()).getName();
        if (ownerName == null) {
            String unknownPlayer = messageManager.getMessage("time.unknown-player", null);
            ownerName = unknownPlayer != null ? unknownPlayer : "未知玩家";
        }

        placeholders.put("owner", ownerName);
        placeholders.put("protection_time", TimeUtil.formatRemainingTime(
            TimeUtil.getRemainingTime(tombstone.getProtectionExpire()), messageManager
        ));

        messageManager.sendMessage(player, "tombstone.protection.blocked", placeholders);
    }
}
