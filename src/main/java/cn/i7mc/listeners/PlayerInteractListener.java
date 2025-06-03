package cn.i7mc.listeners;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.guis.TombstoneItemsGUI;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.TombstoneManager;
import cn.i7mc.tombstones.PlayerTombstone;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * 玩家交互事件监听器 - 统一处理墓碑交互逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class PlayerInteractListener implements Listener {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final TombstoneManager tombstoneManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public PlayerInteractListener(@NotNull PlayerDeadManager plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.tombstoneManager = plugin.getTombstoneManager();
    }
    
    /**
     * 处理玩家交互事件
     * 统一的交互事件处理方法
     *
     * @param event 玩家交互事件
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // 检查是否为右键点击方块
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || clickedBlock == null) {
            return;
        }

        // 检查手部，避免重复触发（只处理主手）
        if (event.getHand() != null && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }



        // 检查是否为墓碑方块
        if (isTombstoneBlock(clickedBlock)) {
            handleTombstoneInteraction(event, player, clickedBlock);
        }
    }
    
    /**
     * 检查是否为墓碑方块
     * 统一的墓碑方块检查方法
     * 
     * @param block 方块
     * @return 是否为墓碑方块
     */
    private boolean isTombstoneBlock(@NotNull Block block) {
        // 检查方块类型是否为配置的墓碑类型
        String configuredType = configManager.getString("tombstone.block-type", "CHEST");
        Material tombstoneMaterial;
        
        try {
            tombstoneMaterial = Material.valueOf(configuredType.toUpperCase());
        } catch (IllegalArgumentException e) {
            tombstoneMaterial = Material.CHEST;
        }
        
        if (block.getType() != tombstoneMaterial) {
            return false;
        }
        
        // 检查是否有墓碑ID标记
        Long tombstoneId = tombstoneManager.getTombstoneId(block);
        return tombstoneId != null;
    }
    
    /**
     * 处理墓碑交互
     * 统一的墓碑交互处理方法
     * 
     * @param event 交互事件
     * @param player 玩家
     * @param block 墓碑方块
     */
    private void handleTombstoneInteraction(@NotNull PlayerInteractEvent event, @NotNull Player player, 
                                          @NotNull Block block) {
        // 取消默认交互（防止打开箱子等）
        event.setCancelled(true);
        
        // 检查基本权限
        if (!player.hasPermission("playerdeadmanager.use")) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "permission.no-basic-permission", placeholders);
            return;
        }
        
        // 获取墓碑ID
        Long tombstoneId = tombstoneManager.getTombstoneId(block);
        if (tombstoneId == null) {
            // 方块没有墓碑ID标记
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "tombstone.not-found", placeholders);
            return;
        }

        // 通过ID获取墓碑信息
        PlayerTombstone tombstone = tombstoneManager.getTombstoneById(tombstoneId);
        if (tombstone == null) {
            // 墓碑不存在，可能是数据不一致
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            placeholders.put("tombstone_id", String.valueOf(tombstoneId));
            messageManager.sendMessage(player, "tombstone.not-found", placeholders);


            return;
        }
        
        // 检查墓碑所有权和权限
        if (!canAccessTombstone(player, tombstone)) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "tombstone.no-access", placeholders);
            return;
        }
        
        // 右键打开物品回收GUI
        handleTombstoneItemsGUI(player, tombstone);
    }
    
    /**
     * 检查玩家是否可以访问墓碑
     * 统一的访问权限检查方法
     * 
     * @param player 玩家
     * @param tombstone 墓碑
     * @return 是否可以访问
     */
    private boolean canAccessTombstone(@NotNull Player player, @NotNull PlayerTombstone tombstone) {
        UUID playerUuid = player.getUniqueId();
        UUID tombstoneOwner = tombstone.getPlayerId();
        
        // 检查是否为墓碑所有者
        if (playerUuid.equals(tombstoneOwner)) {
            return true;
        }
        
        // 检查是否有管理员权限
        if (player.hasPermission("playerdeadmanager.admin")) {
            return true;
        }
        
        // 检查墓碑保护是否已过期
        if (System.currentTimeMillis() > tombstone.getProtectionExpire()) {
            // 保护过期后，任何玩家都可以访问
            return true;
        }
        
        return false;
    }

    /**
     * 处理墓碑物品GUI
     * 统一的墓碑物品GUI处理方法
     *
     * @param player 玩家
     * @param tombstone 墓碑
     */
    private void handleTombstoneItemsGUI(@NotNull Player player, @NotNull PlayerTombstone tombstone) {
        // 检查GUI权限
        if (!player.hasPermission("playerdeadmanager.gui")) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "permission.no-gui", placeholders);
            return;
        }

        // 打开墓碑物品GUI
        TombstoneItemsGUI itemsGUI = new TombstoneItemsGUI(
            plugin,
            player,
            configManager,
            messageManager,
            tombstoneManager,
            tombstone
        );

        // 设置GUI管理器
        itemsGUI.setGUIManager(plugin.getGUIManager());

        itemsGUI.openGUI();
    }
}
