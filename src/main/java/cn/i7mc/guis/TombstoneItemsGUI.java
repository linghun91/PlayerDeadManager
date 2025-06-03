package cn.i7mc.guis;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.abstracts.AbstractGUI;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.DataManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.TombstoneManager;
import cn.i7mc.tombstones.PlayerTombstone;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 墓碑物品GUI - 显示和管理墓碑中的物品
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class TombstoneItemsGUI extends AbstractGUI {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final TombstoneManager tombstoneManager;
    private final PlayerTombstone tombstone;
    private List<DataManager.TombstoneItemData> tombstoneItems;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param player 目标玩家
     * @param configManager 配置管理器
     * @param messageManager 消息管理器
     * @param tombstoneManager 墓碑管理器
     * @param tombstone 墓碑实例
     */
    public TombstoneItemsGUI(@NotNull PlayerDeadManager plugin, @NotNull Player player,
                            @NotNull ConfigManager configManager, @NotNull MessageManager messageManager,
                            @NotNull TombstoneManager tombstoneManager, @NotNull PlayerTombstone tombstone) {
        super(player, getGUITitle(messageManager), 54); // 6行GUI
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.tombstoneManager = tombstoneManager;
        this.tombstone = tombstone;
        this.tombstoneItems = new ArrayList<>();
    }
    
    /**
     * 获取GUI标题
     * 统一的标题获取方法
     * 
     * @param messageManager 消息管理器
     * @return GUI标题
     */
    @NotNull
    private static String getGUITitle(@NotNull MessageManager messageManager) {
        Map<String, String> placeholders = messageManager.createPlaceholders();
        return messageManager.getMessage("gui.tombstone-items.title", placeholders);
    }
    
    /**
     * 初始化GUI内容
     * 统一的GUI初始化方法
     */
    @Override
    public void initializeGUI() {
        // 加载墓碑物品
        loadTombstoneItems();
        
        // 添加边框
        addBorder();
        
        // 添加墓碑物品
        addTombstoneItems();
        
        // 添加功能按钮
        addFunctionButtons();
    }
    
    /**
     * 加载墓碑物品
     * 统一的物品加载方法
     */
    private void loadTombstoneItems() {
        try {
            tombstoneItems = plugin.getDataManager().loadTombstoneItems(tombstone.getTombstoneId());
        } catch (SQLException e) {
            Map<String, String> logPlaceholders = new HashMap<>();
            logPlaceholders.put("error", e.getMessage());
            String logMessage = messageManager.getMessage("logs.tombstone-items.load-failed", logPlaceholders);
            plugin.getLogger().warning(logMessage != null ? logMessage : "加载墓碑物品失败: " + e.getMessage());
            tombstoneItems = new ArrayList<>();
        }
    }
    
    /**
     * 添加边框
     * 统一的边框添加方法
     * GUI设计：第1-5行为物品展示区域，第6行为功能按钮区域
     */
    private void addBorder() {
        // 第1-5行为物品展示区域，不添加边框
        // 第6行为功能按钮区域，也不添加边框
        // 保持简洁的设计，所有45个槽位都可以用来展示物品
    }
    
    /**
     * 添加墓碑物品
     * 统一的墓碑物品添加方法
     * 按顺序展示掉落物品，第1-5行（45个槽位）
     */
    private void addTombstoneItems() {
        // 第1-5行用于展示物品（槽位0-44）
        for (int slot = 0; slot < 45; slot++) {
            if (slot < tombstoneItems.size()) {
                DataManager.TombstoneItemData itemData = tombstoneItems.get(slot);
                ItemStack item = itemData.item();

                if (item != null && item.getType() != Material.AIR) {
                    // 添加物品描述
                    ItemStack displayItem = item.clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.getLore();
                        if (lore == null) {
                            lore = new ArrayList<>();
                        }
                        lore.add("");
                        lore.add(messageManager.getMessage("gui.items.click-to-take", messageManager.createPlaceholders()));
                        meta.setLore(lore);
                        displayItem.setItemMeta(meta);
                    }
                    setItem(slot, displayItem);
                } else {
                    // 空槽位显示为空气（不显示任何物品）
                    setItem(slot, null);
                }
            } else {
                // 超出物品数量的槽位显示为空气
                setItem(slot, null);
            }
        }
    }


    
    /**
     * 创建空槽位物品
     * 统一的空槽位创建方法
     * 
     * @return 空槽位物品
     */
    @NotNull
    private ItemStack createEmptySlotItem() {
        Map<String, String> placeholders = messageManager.createPlaceholders();
        String materialName = messageManager.getMessage("gui.items.empty-slot.material", placeholders);
        String displayName = messageManager.getMessage("gui.items.empty-slot.name", placeholders);
        
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.GRAY_STAINED_GLASS_PANE;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 添加功能按钮
     * 统一的功能按钮添加方法
     */
    private void addFunctionButtons() {
        // 经验按钮 (底部中间)
        setItem(49, createExperienceButton());
        
        // 关闭按钮 (右下角)
        setItem(53, createCloseButton());
    }
    
    /**
     * 创建经验按钮
     * 统一的经验按钮创建方法
     * 
     * @return 经验按钮物品
     */
    @NotNull
    private ItemStack createExperienceButton() {
        Map<String, String> placeholders = messageManager.createPlaceholders();
        placeholders.put("exp", String.valueOf(tombstone.getExperience()));
        
        ItemStack item;
        String buttonName;
        List<String> buttonLore = new ArrayList<>();
        
        if (tombstone.getExperience() > 0) {
            item = new ItemStack(Material.EXPERIENCE_BOTTLE);
            buttonName = messageManager.getMessage("gui.buttons.experience.name", placeholders);
            // 手动添加lore行，因为MessageManager没有getMessageList方法
            String expLore1 = messageManager.getMessage("gui.buttons.experience.lore", placeholders);
            if (expLore1 != null) {
                // 如果配置中有多行，用换行符分割
                String[] loreLines = expLore1.split("\\n");
                for (String line : loreLines) {
                    buttonLore.add(line.trim());
                }
            }
        } else {
            item = new ItemStack(Material.GLASS_BOTTLE);
            buttonName = messageManager.getMessage("gui.buttons.no-experience.name", placeholders);
            String noExpLore = messageManager.getMessage("gui.buttons.no-experience.lore", placeholders);
            if (noExpLore != null) {
                buttonLore.add(noExpLore);
            }
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(buttonName);
            meta.setLore(buttonLore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 创建关闭按钮
     * 统一的关闭按钮创建方法
     * 
     * @return 关闭按钮物品
     */
    @NotNull
    private ItemStack createCloseButton() {
        Map<String, String> placeholders = messageManager.createPlaceholders();
        String buttonName = messageManager.getMessage("gui.buttons.close.name", placeholders);
        String buttonLore = messageManager.getMessage("gui.buttons.close.lore", placeholders);
        
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(buttonName);
            List<String> lore = new ArrayList<>();
            lore.add(buttonLore);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 处理GUI点击事件
     * 统一的点击处理方法
     *
     * @param slot 点击的槽位
     * @param clickedItem 点击的物品
     * @return 是否取消事件
     */
    @Override
    public boolean handleClick(int slot, @Nullable ItemStack clickedItem) {
        // 取消所有点击事件
        if (clickedItem == null) {
            return true;
        }

        // 处理功能按钮
        if (slot == 49) { // 经验按钮
            handleExperienceClick();
            return true;
        }

        if (slot == 53) { // 关闭按钮
            handleCloseClick();
            return true;
        }

        // 处理物品点击
        if (isItemSlot(slot)) {
            handleItemClick(slot);
            return true;
        }

        return true; // 取消所有其他点击
    }

    /**
     * 处理GUI点击事件（带点击类型）
     * 统一的点击处理方法，支持点击类型检查
     *
     * @param slot 点击的槽位
     * @param clickedItem 点击的物品
     * @param clickType 点击类型
     * @return 是否取消事件
     */
    public boolean handleClick(int slot, @Nullable ItemStack clickedItem, @NotNull org.bukkit.event.inventory.ClickType clickType) {
        // 取消所有点击事件
        if (clickedItem == null) {
            return true;
        }

        // 处理功能按钮（允许左键和右键）
        if (slot == 49) { // 经验按钮
            handleExperienceClick();
            return true;
        }

        if (slot == 53) { // 关闭按钮
            handleCloseClick();
            return true;
        }

        // 处理物品点击 - 只允许右键点击
        if (isItemSlot(slot)) {
            if (clickType.isRightClick()) {
                handleItemClick(slot);
            } else {
                // 发送提示消息：只能右键点击取出物品
                Map<String, String> placeholders = messageManager.createPlaceholders();
                messageManager.addPlayerPlaceholders(placeholders, player);
                messageManager.sendMessage(player, "gui.right-click-only", placeholders);
            }
            return true;
        }

        return true; // 取消所有其他点击
    }
    
    /**
     * 检查是否为物品槽位
     * 统一的槽位检查方法
     *
     * @param slot 槽位
     * @return 是否为物品槽位
     */
    private boolean isItemSlot(int slot) {
        // 第1-5行为物品展示区域（槽位0-44）
        return slot >= 0 && slot < 45;
    }
    
    /**
     * 处理物品点击
     * 统一的物品点击处理方法
     *
     * @param slot 点击的槽位
     */
    private void handleItemClick(int slot) {
        // 直接使用槽位作为索引，简单的顺序展示
        if (slot < 0 || slot >= tombstoneItems.size()) {
            return;
        }

        DataManager.TombstoneItemData itemData = tombstoneItems.get(slot);
        ItemStack item = itemData.item();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // 取出物品 - 使用原始索引进行数据库操作
        takeItem(itemData.originalSlotIndex(), item, slot);
    }
    

    
    /**
     * 取出物品
     * 统一的物品取出方法
     *
     * @param originalSlotIndex 物品在PlayerInventory中的原始索引
     * @param item 物品
     * @param guiSlot GUI中的槽位索引
     */
    private void takeItem(int originalSlotIndex, @NotNull ItemStack item, int guiSlot) {
        try {
            // 先检查是否能完全添加物品到背包，不实际添加
            if (!canAddItemToInventory(player, item)) {
                Map<String, String> placeholders = messageManager.createPlaceholders();
                messageManager.addPlayerPlaceholders(placeholders, player);
                messageManager.sendMessage(player, "pickup.inventory-full", placeholders);
                return;
            }

            // 确认可以添加后，才实际添加到背包
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);

            // 理论上不应该有剩余物品，但为了安全起见还是检查一下
            if (!leftover.isEmpty()) {
                Map<String, String> logPlaceholders = new HashMap<>();
                logPlaceholders.put("player", player.getName());
                String logMessage = messageManager.getMessage("logs.tombstone-items.inventory-add-failed", logPlaceholders);
                plugin.getLogger().warning(logMessage != null ? logMessage : "物品添加异常：预检查通过但实际添加失败，玩家: " + player.getName());

                Map<String, String> placeholders = messageManager.createPlaceholders();
                messageManager.addPlayerPlaceholders(placeholders, player);
                messageManager.sendMessage(player, "pickup.inventory-full", placeholders);
                return;
            }

            // 成功添加到背包后，才从数据库中移除物品（使用原始索引）
            plugin.getDataManager().removeTombstoneItem(tombstone.getTombstoneId(), originalSlotIndex);

            // 从本地列表中移除（从GUI列表中移除）
            tombstoneItems.remove(guiSlot);

            // 发送成功消息
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            placeholders.put("item", item.getType().name());
            messageManager.sendMessage(player, "pickup.item-taken", placeholders);

            // 刷新GUI
            refreshGUI();

            // 检查墓碑是否为空
            checkAndRemoveEmptyTombstone();

        } catch (SQLException e) {
            Map<String, String> logPlaceholders = new HashMap<>();
            logPlaceholders.put("error", e.getMessage());
            String logMessage = messageManager.getMessage("logs.tombstone-items.remove-failed", logPlaceholders);
            plugin.getLogger().warning(logMessage != null ? logMessage : "移除墓碑物品失败: " + e.getMessage());

            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "error.database-error", placeholders);
        }
    }
    
    /**
     * 处理经验点击
     * 统一的经验点击处理方法
     */
    private void handleExperienceClick() {
        if (tombstone.getExperience() <= 0) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "pickup.no-experience", placeholders);
            return;
        }
        
        try {
            // 给玩家经验
            player.giveExp(tombstone.getExperience());
            
            // 从数据库中移除经验
            plugin.getDataManager().removeTombstoneExperience(tombstone.getTombstoneId());
            
            // 发送成功消息
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            placeholders.put("exp", String.valueOf(tombstone.getExperience()));
            messageManager.sendMessage(player, "pickup.experience-taken", placeholders);
            
            // 更新墓碑经验
            tombstone.setExperience(0);
            
            // 刷新GUI
            refreshGUI();
            
            // 检查墓碑是否为空
            checkAndRemoveEmptyTombstone();
            
        } catch (SQLException e) {
            Map<String, String> logPlaceholders = new HashMap<>();
            logPlaceholders.put("error", e.getMessage());
            String logMessage = messageManager.getMessage("logs.experience.remove-failed", logPlaceholders);
            plugin.getLogger().warning(logMessage != null ? logMessage : "移除墓碑经验失败: " + e.getMessage());

            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "error.database-error", placeholders);
        }
    }
    
    /**
     * 处理关闭点击
     * 统一的关闭点击处理方法
     */
    private void handleCloseClick() {
        closeGUI();
    }
    
    /**
     * 检查并移除空墓碑
     * 统一的空墓碑检查方法
     */
    private void checkAndRemoveEmptyTombstone() {
        try {
            // 检查是否还有物品
            boolean hasItems = !tombstoneItems.isEmpty();

            // 检查是否还有经验
            boolean hasExperience = tombstone.getExperience() > 0;

            if (!hasItems && !hasExperience) {
                // 墓碑为空，移除它
                tombstoneManager.removeTombstone(tombstone);

                // 发送消息
                Map<String, String> placeholders = messageManager.createPlaceholders();
                messageManager.addPlayerPlaceholders(placeholders, player);
                messageManager.sendMessage(player, "pickup.tombstone-empty", placeholders);

                // 关闭GUI
                closeGUI();
            }
        } catch (Exception e) {
            Map<String, String> logPlaceholders = new HashMap<>();
            logPlaceholders.put("error", e.getMessage());
            String logMessage = messageManager.getMessage("logs.tombstone.empty-check-failed", logPlaceholders);
            plugin.getLogger().warning(logMessage != null ? logMessage : "检查空墓碑失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否能将物品完全添加到玩家背包
     * 统一的背包空间检查方法，不实际添加物品
     * 通过克隆背包并模拟addItem操作来精确检查
     *
     * @param player 玩家
     * @param item 要添加的物品
     * @return 是否能完全添加
     */
    private boolean canAddItemToInventory(@NotNull Player player, @NotNull ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return true;
        }

        // 创建一个临时的背包来模拟添加操作
        PlayerInventory originalInventory = player.getInventory();

        // 克隆主背包的内容（36个槽位）
        ItemStack[] tempContents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            ItemStack originalItem = originalInventory.getItem(i);
            if (originalItem != null) {
                tempContents[i] = originalItem.clone();
            }
        }

        // 模拟addItem的逻辑
        ItemStack itemToAdd = item.clone();
        int remainingAmount = itemToAdd.getAmount();
        int maxStackSize = itemToAdd.getMaxStackSize();

        // 第一步：尝试堆叠到现有的相同物品
        for (int slot = 0; slot < 36; slot++) {
            ItemStack slotItem = tempContents[slot];

            if (slotItem != null && slotItem.isSimilar(itemToAdd)) {
                int currentAmount = slotItem.getAmount();
                int canAdd = maxStackSize - currentAmount;

                if (canAdd > 0) {
                    int addAmount = Math.min(canAdd, remainingAmount);
                    remainingAmount -= addAmount;

                    if (remainingAmount <= 0) {
                        return true; // 全部可以添加
                    }
                }
            }
        }

        // 第二步：尝试放入空槽位
        for (int slot = 0; slot < 36; slot++) {
            ItemStack slotItem = tempContents[slot];

            if (slotItem == null) {
                int addAmount = Math.min(maxStackSize, remainingAmount);
                remainingAmount -= addAmount;

                if (remainingAmount <= 0) {
                    return true; // 全部可以添加
                }
            }
        }

        // 如果还有剩余，说明背包空间不足
        return remainingAmount <= 0;
    }
}
