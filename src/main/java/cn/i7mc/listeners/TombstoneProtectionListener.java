package cn.i7mc.listeners;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.TombstoneManager;
import cn.i7mc.managers.WorldConfigManager;
import cn.i7mc.tombstones.PlayerTombstone;
import cn.i7mc.utils.TimeUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.block.BlockState;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
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
    private final WorldConfigManager worldConfigManager;
    
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
        this.worldConfigManager = plugin.getWorldConfigManager();
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

        // 检查是否启用头颅完全保护
        String worldName = player.getWorld().getName();
        boolean skullProtection = worldConfigManager.isSkullProtectionEnabled(worldName);

        if (skullProtection) {
            // 墓碑头颅永远不允许被任何人破坏，包括管理员
            // 只能通过正常收集或过期自动清理来移除整个墓碑实例
            event.setCancelled(true);

            // 发送头颅保护消息
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "tombstone.skull-protected", placeholders);

            // 如果是管理员，额外提示使用管理员指令
            if (hasAdminPermission(player)) {
                placeholders.put("tombstone_id", String.valueOf(tombstoneId));
                messageManager.sendMessage(player, "tombstone.admin-use-command", placeholders);
            }
            return;
        } else {
            // 如果未启用完全保护，则按原有逻辑处理
            // 检查是否为墓碑所有者或管理员
            if (!tombstone.canAccess(player)) {
                event.setCancelled(true);
                sendProtectionMessage(player, tombstone);
                return;
            }
        }

        // 如果是墓碑所有者，提示正确的收集方式
        if (tombstone.getPlayerId().equals(player.getUniqueId())) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "tombstone.use-right-click", placeholders);
        } else {
            sendProtectionMessage(player, tombstone);
        }
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
                // 检查该世界是否启用头颅保护
                String worldName = block.getWorld().getName();
                if (worldConfigManager.isSkullProtectionEnabled(worldName)) {
                    return true; // 移除此方块，墓碑永远不被爆炸破坏
                }
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
                // 检查该世界是否启用头颅保护
                String worldName = block.getWorld().getName();
                if (worldConfigManager.isSkullProtectionEnabled(worldName)) {
                    return true; // 移除此方块，墓碑永远不被爆炸破坏
                }
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
        return worldConfigManager.hasAdminPermission(player);
    }

    /**
     * 处理方块燃烧事件
     * 统一的燃烧保护方法
     *
     * @param event 方块燃烧事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(@NotNull BlockBurnEvent event) {
        Block block = event.getBlock();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());

        if (tombstone != null) {
            event.setCancelled(true); // 墓碑永远不被火烧毁
        }
    }

    /**
     * 处理方块凋零事件
     * 统一的凋零保护方法
     *
     * @param event 方块凋零事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFade(@NotNull BlockFadeEvent event) {
        Block block = event.getBlock();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());

        if (tombstone != null) {
            event.setCancelled(true); // 墓碑永远不凋零
        }
    }

    /**
     * 处理活塞推拉事件
     * 统一的活塞保护方法
     *
     * @param event 活塞伸展事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(@NotNull BlockPistonExtendEvent event) {
        // 检查被推动的方块中是否有墓碑
        for (Block block : event.getBlocks()) {
            PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());
            if (tombstone != null) {
                event.setCancelled(true); // 墓碑永远不被活塞推动
                return;
            }
        }
    }

    /**
     * 处理活塞收缩事件
     * 统一的活塞保护方法
     *
     * @param event 活塞收缩事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(@NotNull BlockPistonRetractEvent event) {
        // 检查被拉动的方块中是否有墓碑
        for (Block block : event.getBlocks()) {
            PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());
            if (tombstone != null) {
                event.setCancelled(true); // 墓碑永远不被活塞拉动
                return;
            }
        }
    }

    /**
     * 处理实体改变方块事件
     * 统一的实体保护方法
     *
     * @param event 实体改变方块事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(@NotNull EntityChangeBlockEvent event) {
        Block block = event.getBlock();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());

        if (tombstone != null) {
            event.setCancelled(true); // 墓碑永远不被实体改变
        }
    }

    /**
     * 处理方块物理事件
     * 统一的物理保护方法
     *
     * @param event 方块物理事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPhysics(@NotNull BlockPhysicsEvent event) {
        Block block = event.getBlock();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());

        if (tombstone != null) {
            event.setCancelled(true); // 墓碑永远不受物理影响
        }
    }

    /**
     * 处理方块破坏方块事件（Paper特有）
     * 统一的方块间破坏保护方法
     *
     * @param event 方块破坏方块事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreakBlock(@NotNull BlockBreakBlockEvent event) {
        Block block = event.getBlock();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());

        if (tombstone != null) {
            // 清空掉落物品，防止墓碑被其他方块破坏
            event.getDrops().clear();
        }
    }

    /**
     * 处理结构生长事件
     * 统一的结构生长保护方法
     *
     * @param event 结构生长事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onStructureGrow(@NotNull StructureGrowEvent event) {
        // 检查生长的方块中是否会覆盖墓碑
        event.getBlocks().removeIf(blockState -> {
            PlayerTombstone tombstone = tombstoneManager.getTombstone(blockState.getLocation());
            return tombstone != null; // 移除会覆盖墓碑的方块状态
        });
    }

    /**
     * 处理TNT爆炸引燃事件
     * 统一的TNT保护方法
     *
     * @param event TNT引燃事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTPrime(@NotNull TNTPrimeEvent event) {
        Block block = event.getBlock();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());

        if (tombstone != null) {
            event.setCancelled(true); // 墓碑位置的TNT永远不被引燃
        }
    }

    /**
     * 处理方块破坏后事件（监听）
     * 统一的墓碑破坏后清理方法
     * 当墓碑头颅被意外破坏时，确保完全移除整个墓碑实例
     *
     * @param event 方块破坏后事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreakMonitor(@NotNull BlockBreakEvent event) {
        // 只在事件没有被取消时处理
        if (event.isCancelled()) {
            return;
        }

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

        // 墓碑头颅被破坏，立即清理整个墓碑实例
        plugin.getLogger().warning("检测到墓碑头颅被意外破坏，正在清理整个墓碑实例。墓碑ID: " + tombstoneId +
            " 位置: " + block.getLocation().getWorld().getName() + " " +
            block.getLocation().getBlockX() + "," + block.getLocation().getBlockY() + "," + block.getLocation().getBlockZ());

        // 使用TombstoneManager的统一移除方法
        boolean removed = tombstoneManager.removeTombstone(block.getLocation());

        if (removed) {
            plugin.getLogger().info("墓碑实例已成功清理。墓碑ID: " + tombstoneId);
        } else {
            plugin.getLogger().warning("墓碑实例清理失败。墓碑ID: " + tombstoneId);
        }
    }

    /**
     * 处理流体流动事件
     * 统一的流体保护方法
     *
     * @param event 方块从流体事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(@NotNull BlockFromToEvent event) {
        Block toBlock = event.getToBlock();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(toBlock.getLocation());

        if (tombstone != null) {
            event.setCancelled(true); // 墓碑永远不被流体冲走
        }
    }

    /**
     * 处理方块多重放置事件
     * 统一的多重放置保护方法
     *
     * @param event 方块多重放置事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockMultiPlace(@NotNull BlockMultiPlaceEvent event) {
        for (BlockState blockState : event.getReplacedBlockStates()) {
            PlayerTombstone tombstone = tombstoneManager.getTombstone(blockState.getLocation());
            if (tombstone != null) {
                event.setCancelled(true); // 墓碑位置不允许多重放置
                return;
            }
        }
    }

    /**
     * 处理方块形成事件
     * 统一的方块形成保护方法
     *
     * @param event 方块形成事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockForm(@NotNull BlockFormEvent event) {
        Block block = event.getBlock();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());

        if (tombstone != null) {
            event.setCancelled(true); // 墓碑位置不允许方块形成
        }
    }

    /**
     * 处理方块蔓延事件
     * 统一的方块蔓延保护方法
     *
     * @param event 方块蔓延事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockSpread(@NotNull BlockSpreadEvent event) {
        Block block = event.getBlock();
        PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());

        if (tombstone != null) {
            event.setCancelled(true); // 墓碑位置不允许方块蔓延
        }
    }

    /**
     * 处理实体爆炸后事件（监听）
     * 统一的爆炸后检查方法
     *
     * @param event 实体爆炸事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplodeMonitor(@NotNull EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // 检查爆炸后是否有墓碑被意外破坏
        for (Block block : event.blockList()) {
            PlayerTombstone tombstone = tombstoneManager.getTombstone(block.getLocation());
            if (tombstone != null) {
                plugin.getLogger().warning("检测到墓碑在爆炸中被破坏，正在恢复墓碑: " +
                    block.getLocation().getWorld().getName() + " " +
                    block.getLocation().getBlockX() + "," + block.getLocation().getBlockY() + "," + block.getLocation().getBlockZ());

                // 恢复墓碑方块
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    tombstone.createTombstone();
                }, 1L);
            }
        }
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
