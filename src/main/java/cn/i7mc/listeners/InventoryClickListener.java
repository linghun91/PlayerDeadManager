package cn.i7mc.listeners;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.abstracts.AbstractGUI;
import cn.i7mc.guis.TeleportGUI;
import cn.i7mc.managers.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * GUI点击事件监听器 - 统一处理GUI点击逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class InventoryClickListener implements Listener {
    
    private final PlayerDeadManager plugin;
    private final MessageManager messageManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public InventoryClickListener(@NotNull PlayerDeadManager plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }
    
    /**
     * 处理GUI点击事件
     * 统一的GUI点击处理方法
     *
     * @param event GUI点击事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        // 检查是否为玩家
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // 检查是否在插件GUI界面中（通过InventoryView检查）
        if (isPlayerInPluginGUI(event)) {
            handlePluginGUIClick(event, player);
        }
    }

    /**
     * 处理GUI关闭事件
     * 统一的GUI关闭处理方法
     *
     * @param event GUI关闭事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        // 检查是否为玩家
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // 检查是否为插件的GUI
        AbstractGUI activeGUI = plugin.getGUIManager().getActiveGUI(player);
        if (activeGUI != null) {
            // 从GUI管理器注销
            plugin.getGUIManager().unregisterGUI(player);
        }
    }
    
    /**
     * 检查玩家是否在插件GUI界面中
     * 统一的GUI界面检查方法
     * 通过InventoryView检查当前打开的GUI，而不是点击的背包
     *
     * @param event 点击事件
     * @return 是否在插件GUI界面中
     */
    private boolean isPlayerInPluginGUI(@NotNull InventoryClickEvent event) {
        // 通过InventoryView的TopInventory检查当前打开的GUI
        Inventory topInventory = event.getView().getTopInventory();

        // 检查是否为传送GUI
        if (isTeleportGUIByInventory(topInventory, event.getView())) {
            return true;
        }

        // 检查是否为墓碑物品GUI
        if (isTombstoneItemsGUIByInventory(topInventory, event.getView())) {
            return true;
        }

        return false;
    }

    /**
     * 通过Inventory检查是否为传送GUI
     * 统一的传送GUI检查方法（基于Inventory）
     *
     * @param inventory 背包实例
     * @param view 背包视图
     * @return 是否为传送GUI
     */
    private boolean isTeleportGUIByInventory(@NotNull Inventory inventory, @NotNull org.bukkit.inventory.InventoryView view) {
        // 通过GUI标题判断（从配置文件获取）
        Map<String, String> placeholders = messageManager.createPlaceholders();
        String teleportTitle = messageManager.getMessage("gui.teleport.title", placeholders);

        // 检查标题和大小
        if (inventory.getSize() != 54) {
            return false;
        }

        // 获取实际的GUI标题
        String actualTitle = view.getTitle();
        if (actualTitle == null) {
            return false;
        }

        // 将配置中的标题转换为带颜色的格式，然后比较
        String coloredTeleportTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', teleportTitle);

        return coloredTeleportTitle.equals(actualTitle);
    }

    /**
     * 通过Inventory检查是否为墓碑物品GUI
     * 统一的墓碑物品GUI检查方法（基于Inventory）
     *
     * @param inventory 背包实例
     * @param view 背包视图
     * @return 是否为墓碑物品GUI
     */
    private boolean isTombstoneItemsGUIByInventory(@NotNull Inventory inventory, @NotNull org.bukkit.inventory.InventoryView view) {
        // 通过GUI标题判断（从配置文件获取）
        Map<String, String> placeholders = messageManager.createPlaceholders();
        String itemsTitle = messageManager.getMessage("gui.tombstone-items.title", placeholders);

        // 检查标题和大小
        if (inventory.getSize() != 54) {
            return false;
        }

        // 获取实际的GUI标题
        String actualTitle = view.getTitle();
        if (actualTitle == null) {
            return false;
        }

        // 将配置中的标题转换为带颜色的格式，然后比较
        String coloredItemsTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', itemsTitle);

        return coloredItemsTitle.equals(actualTitle);
    }

    /**
     * 处理插件GUI点击
     * 统一的插件GUI点击处理方法
     *
     * @param event 点击事件
     * @param player 玩家
     */
    private void handlePluginGUIClick(@NotNull InventoryClickEvent event, @NotNull Player player) {
        // 取消事件，防止物品被拿走
        event.setCancelled(true);

        // 防止玩家将物品移动到GUI中的额外检查
        if (isProhibitedAction(event)) {
            return; // 直接返回，事件已被取消
        }

        // 检查权限
        if (!player.hasPermission("playerdeadmanager.gui")) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "permission.no-gui", placeholders);
            return;
        }

        // 根据GUI类型处理点击（使用新的检查方法）
        Inventory topInventory = event.getView().getTopInventory();
        if (isTeleportGUIByInventory(topInventory, event.getView())) {
            handleTeleportGUIClick(event, player);
        } else if (isTombstoneItemsGUIByInventory(topInventory, event.getView())) {
            handleTombstoneItemsGUIClick(event, player);
        }
    }
    
    /**
     * 处理传送GUI点击
     * 统一的传送GUI点击处理方法
     *
     * @param event 点击事件
     * @param player 玩家
     */
    private void handleTeleportGUIClick(@NotNull InventoryClickEvent event, @NotNull Player player) {
        // 取消事件，防止物品被拿走
        event.setCancelled(true);

        // 通过GUI管理器查找对应的GUI实例
        AbstractGUI activeGUI = plugin.getGUIManager().getActiveGUI(player);

        if (activeGUI instanceof TeleportGUI teleportGUI) {
            // 调用GUI的点击处理方法
            teleportGUI.handleClick(event.getSlot(), event.getCurrentItem());
        } else {
            // 如果没有找到对应的GUI实例，记录警告
            plugin.getLogger().warning("无法找到玩家 " + player.getName() + " 的传送GUI实例");
        }


    }

    /**
     * 处理墓碑物品GUI点击
     * 统一的墓碑物品GUI点击处理方法
     *
     * @param event 点击事件
     * @param player 玩家
     */
    private void handleTombstoneItemsGUIClick(@NotNull InventoryClickEvent event, @NotNull Player player) {
        // 取消事件，防止物品被拿走
        event.setCancelled(true);

        // 通过GUI管理器查找对应的GUI实例
        AbstractGUI activeGUI = plugin.getGUIManager().getActiveGUI(player);

        if (activeGUI instanceof cn.i7mc.guis.TombstoneItemsGUI tombstoneGUI) {
            // 调用GUI的点击处理方法，传递点击类型
            tombstoneGUI.handleClick(event.getSlot(), event.getCurrentItem(), event.getClick());
        } else {
            // 如果没有找到对应的GUI实例，记录警告
            plugin.getLogger().warning("无法找到玩家 " + player.getName() + " 的墓碑物品GUI实例");
        }


    }

    /**
     * 检查是否为禁止的操作
     * 统一的禁止操作检查方法
     * 防止玩家通过shift+点击或拖拽将物品移动到GUI中
     * 注意：只在墓碑物品GUI中阻止这些操作，传送GUI允许正常点击
     *
     * @param event 点击事件
     * @return 是否为禁止的操作
     */
    private boolean isProhibitedAction(@NotNull InventoryClickEvent event) {
        InventoryAction action = event.getAction();
        Inventory topInventory = event.getView().getTopInventory();

        // 只在墓碑物品GUI中阻止物品移动操作
        // 传送GUI需要允许正常的点击操作
        if (!isTombstoneItemsGUIByInventory(topInventory, event.getView())) {
            return false; // 不是墓碑物品GUI，允许所有操作
        }

        // 检查是否点击的是GUI的上半部分（墓碑物品区域）
        // 对于54槽位的GUI，上半部分是槽位0-53，下半部分是玩家背包
        boolean isClickingGUIArea = event.getRawSlot() < topInventory.getSize();

        // 情况1：直接点击GUI区域并试图放置物品
        if (isClickingGUIArea) {
            // 在墓碑物品GUI中，只允许PICKUP类型的操作（取出物品）
            // 禁止放置、交换或移动物品到GUI中
            return action == InventoryAction.PLACE_ALL ||
                   action == InventoryAction.PLACE_SOME ||
                   action == InventoryAction.PLACE_ONE ||
                   action == InventoryAction.SWAP_WITH_CURSOR ||
                   action == InventoryAction.HOTBAR_MOVE_AND_READD ||
                   action == InventoryAction.HOTBAR_SWAP;
        }

        // 情况2：点击玩家背包区域但试图将物品移动到GUI中
        // 这主要是shift+点击的情况
        if (!isClickingGUIArea && action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // 禁止从玩家背包shift+点击移动物品到墓碑物品GUI中
            return true;
        }

        // 其他情况允许
        return false;
    }

}
