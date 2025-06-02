package cn.i7mc.listeners;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.TombstoneManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 玩家死亡事件监听器 - 统一处理玩家死亡相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class PlayerDeathListener implements Listener {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final TombstoneManager tombstoneManager;
    
    /**
     * 构造函数
     *
     * @param plugin 插件实例
     */
    public PlayerDeathListener(@NotNull PlayerDeadManager plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.tombstoneManager = plugin.getTombstoneManager();
    }
    
    /**
     * 处理玩家死亡事件
     * 统一的死亡事件处理方法
     * 
     * @param event 玩家死亡事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // 检查世界是否启用
        if (!isWorldEnabled(player.getWorld().getName())) {
            return;
        }

        // 检查权限
        if (!player.hasPermission("playerdeadmanager.use")) {
            return;
        }
        
        // 检查保存图腾
        if (checkAndConsumeTotem(player, event)) {
            return; // 图腾已激活，不创建墓碑
        }
        
        // 处理死亡掉落
        handleDeathDrops(player, event);

        // 创建墓碑
        createTombstone(player, event);
    }
    
    /**
     * 检查世界是否启用墓碑功能
     * 统一的世界检查方法
     * 
     * @param worldName 世界名称
     * @return 是否启用
     */
    private boolean isWorldEnabled(@NotNull String worldName) {
        // 检查禁用世界列表
        if (configManager.getStringList("worlds.disabled-worlds").contains(worldName)) {
            return false;
        }
        
        // 检查启用世界列表（空列表表示所有世界）
        var enabledWorlds = configManager.getStringList("worlds.enabled-worlds");
        return enabledWorlds.isEmpty() || enabledWorlds.contains(worldName);
    }
    
    /**
     * 检查并消耗保存图腾
     * 统一的图腾检查方法
     * 
     * @param player 玩家
     * @param event 死亡事件
     * @return 是否消耗了图腾
     */
    private boolean checkAndConsumeTotem(@NotNull Player player, @NotNull PlayerDeathEvent event) {
        // 检查是否启用图腾功能
        if (!configManager.getBoolean("totem.enabled", true)) {
            return false;
        }
        

        
        PlayerInventory inventory = player.getInventory();
        String totemType = configManager.getString("totem.item-type", "TOTEM_OF_UNDYING");
        Material totemMaterial;
        
        try {
            totemMaterial = Material.valueOf(totemType);
        } catch (IllegalArgumentException e) {
            return false;
        }

        // 查找图腾
        ItemStack totemItem = findTotemInInventory(inventory, totemMaterial);
        if (totemItem == null) {
            return false;
        }
        
        // 消耗图腾
        int amount = totemItem.getAmount();
        if (amount > 1) {
            totemItem.setAmount(amount - 1);
        } else {
            inventory.remove(totemItem);
        }
        
        // 设置保持物品和经验
        event.setKeepInventory(true);
        event.setKeepLevel(configManager.getBoolean("experience.keep-level", false));
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // 发送消息
        Map<String, String> placeholders = messageManager.createPlaceholders();
        messageManager.addPlayerPlaceholders(placeholders, player);
        messageManager.sendMessage(player, "totem.used", placeholders);
        
        return true;
    }
    
    /**
     * 在背包中查找图腾
     * 统一的图腾查找方法
     * 
     * @param inventory 玩家背包
     * @param totemMaterial 图腾材质
     * @return 找到的图腾物品，未找到返回null
     */
    private ItemStack findTotemInInventory(@NotNull PlayerInventory inventory, @NotNull Material totemMaterial) {
        // 检查主背包
        for (ItemStack item : inventory.getContents()) {
            if (isValidTotem(item, totemMaterial)) {
                return item;
            }
        }
        
        // 检查快捷栏
        for (ItemStack item : inventory.getStorageContents()) {
            if (isValidTotem(item, totemMaterial)) {
                return item;
            }
        }
        
        return null;
    }
    
    /**
     * 检查物品是否为有效的保存图腾
     * 统一的图腾验证方法
     * 
     * @param item 物品
     * @param totemMaterial 图腾材质
     * @return 是否为有效图腾
     */
    private boolean isValidTotem(ItemStack item, @NotNull Material totemMaterial) {
        if (item == null || item.getType() != totemMaterial) {
            return false;
        }
        
        // 检查自定义模型数据（如果配置了）
        int customModelData = configManager.getInt("totem.custom-model-data", 0);
        if (customModelData > 0 && item.hasItemMeta()) {
            return item.getItemMeta().hasCustomModelData() && 
                   item.getItemMeta().getCustomModelData() == customModelData;
        }
        
        // 检查显示名称（如果配置了）
        String displayName = configManager.getString("totem.display-name", null);
        if (displayName != null && !displayName.isEmpty() && item.hasItemMeta()) {
            return item.getItemMeta().hasDisplayName() && 
                   item.getItemMeta().getDisplayName().equals(displayName);
        }
        
        return true;
    }
    
    /**
     * 处理死亡掉落物品
     * 统一的掉落处理方法
     * 
     * @param player 玩家
     * @param event 死亡事件
     */
    private void handleDeathDrops(@NotNull Player player, @NotNull PlayerDeathEvent event) {
        // 清空掉落物品（将由墓碑系统处理）
        event.getDrops().clear();
        
        // 设置不保持背包和等级
        event.setKeepInventory(false);
        event.setKeepLevel(false);
        
        // 设置不掉落经验（将由墓碑系统处理）
        event.setDroppedExp(0);
    }

    /**
     * 创建墓碑
     * 统一的墓碑创建方法
     *
     * @param player 死亡玩家
     * @param event 死亡事件
     */
    private void createTombstone(@NotNull Player player, @NotNull PlayerDeathEvent event) {
        // 获取玩家所有物品 - getContents()已经包含了所有槽位（背包+装备+副手）
        ItemStack[] allItems = player.getInventory().getContents();

        // 检查是否有物品需要保存
        boolean hasItems = false;
        for (ItemStack item : allItems) {
            if (item != null && !item.getType().isAir()) {
                hasItems = true;
                break;
            }
        }

        // 获取经验值
        int experience = player.getTotalExperience();

        // 如果没有物品也没有经验，不创建墓碑
        if (!hasItems && experience <= 0) {
            return;
        }

        // 创建墓碑
        tombstoneManager.createTombstone(player, player.getLocation(), allItems, experience);
    }
}
