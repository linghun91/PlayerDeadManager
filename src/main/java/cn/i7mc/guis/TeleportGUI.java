package cn.i7mc.guis;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.abstracts.AbstractGUI;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.DataManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.TombstoneManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 传送GUI - 统一处理墓碑传送界面
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class TeleportGUI extends AbstractGUI {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final TombstoneManager tombstoneManager;
    private final List<DataManager.TombstoneData> tombstones;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param player 目标玩家
     * @param configManager 配置管理器
     * @param messageManager 消息管理器
     * @param tombstoneManager 墓碑管理器
     */
    public TeleportGUI(@NotNull PlayerDeadManager plugin, @NotNull Player player,
                      @NotNull ConfigManager configManager, @NotNull MessageManager messageManager,
                      @NotNull TombstoneManager tombstoneManager) {
        super(player, getGUITitle(messageManager), 54); // 6行GUI
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.tombstoneManager = tombstoneManager;
        this.tombstones = tombstoneManager.getPlayerTombstones(player.getUniqueId());
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
        return messageManager.getMessage("gui.teleport.title", placeholders);
    }
    
    /**
     * 初始化GUI内容
     * 统一的GUI初始化方法
     */
    @Override
    public void initializeGUI() {
        // 清空GUI
        inventory.clear();
        
        // 添加装饰性边框
        addBorder();
        
        // 添加墓碑物品
        addTombstoneItems();
        
        // 添加功能按钮
        addFunctionButtons();
    }
    
    /**
     * 添加装饰性边框
     * 统一的边框添加方法
     */
    private void addBorder() {
        ItemStack borderItem = createBorderItem();
        
        // 顶部和底部边框
        for (int i = 0; i < 9; i++) {
            setItem(i, borderItem);
            setItem(45 + i, borderItem);
        }
        
        // 左右边框
        for (int i = 1; i < 5; i++) {
            setItem(i * 9, borderItem);
            setItem(i * 9 + 8, borderItem);
        }
    }
    
    /**
     * 创建边框物品
     * 统一的边框物品创建方法
     * 
     * @return 边框物品
     */
    @NotNull
    private ItemStack createBorderItem() {
        String materialName = configManager.getString("gui.border.material", "GRAY_STAINED_GLASS_PANE");
        Material borderMaterial;
        
        try {
            borderMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            borderMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }
        
        ItemStack item = new ItemStack(borderMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            meta.setDisplayName(messageManager.getMessage("gui.border.name", placeholders));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 添加墓碑物品
     * 统一的墓碑物品添加方法
     */
    private void addTombstoneItems() {
        int slot = 10; // 从第二行第二列开始
        int maxTombstones = Math.min(tombstones.size(), 28); // 最多显示28个墓碑
        
        for (int i = 0; i < maxTombstones; i++) {
            DataManager.TombstoneData tombstone = tombstones.get(i);
            ItemStack tombstoneItem = createTombstoneItem(tombstone, i);
            
            setItem(slot, tombstoneItem);
            
            // 计算下一个槽位（跳过边框）
            slot++;
            if ((slot + 1) % 9 == 0) { // 到达右边框
                slot += 2; // 跳到下一行的第二列
            }
            if (slot >= 45) { // 到达底部边框
                break;
            }
        }
    }
    
    /**
     * 创建墓碑物品
     * 统一的墓碑物品创建方法
     * 
     * @param tombstone 墓碑数据
     * @param index 索引
     * @return 墓碑物品
     */
    @NotNull
    private ItemStack createTombstoneItem(@NotNull DataManager.TombstoneData tombstone, int index) {
        String materialName = configManager.getString("gui.tombstone.material", "CHEST");
        Material tombstoneMaterial;
        
        try {
            tombstoneMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            tombstoneMaterial = Material.CHEST;
        }
        
        ItemStack item = new ItemStack(tombstoneMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 设置显示名称
            Map<String, String> placeholders = messageManager.createPlaceholders();
            placeholders.put("index", String.valueOf(index + 1));
            placeholders.put("world", tombstone.worldName());
            placeholders.put("x", String.valueOf(tombstone.x()));
            placeholders.put("y", String.valueOf(tombstone.y()));
            placeholders.put("z", String.valueOf(tombstone.z()));
            
            meta.setDisplayName(messageManager.getMessage("gui.tombstone.name", placeholders));
            
            // 设置Lore
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getMessage("gui.tombstone.lore.location", placeholders));
            lore.add(messageManager.getMessage("gui.tombstone.lore.experience", 
                Map.of("exp", String.valueOf(tombstone.experience()))));
            lore.add(messageManager.getMessage("gui.tombstone.lore.death-time", 
                Map.of("time", formatTime(tombstone.deathTime()))));
            lore.add(messageManager.getMessage("gui.tombstone.lore.protection", 
                Map.of("time", formatTime(tombstone.protectionExpire()))));
            lore.add("");
            lore.add(messageManager.getMessage("gui.tombstone.lore.click-to-teleport", placeholders));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 添加功能按钮
     * 统一的功能按钮添加方法
     */
    private void addFunctionButtons() {
        // 刷新按钮
        setItem(49, createRefreshButton());
        
        // 关闭按钮
        setItem(53, createCloseButton());
    }
    
    /**
     * 创建刷新按钮
     * 统一的刷新按钮创建方法
     * 
     * @return 刷新按钮
     */
    @NotNull
    private ItemStack createRefreshButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            meta.setDisplayName(messageManager.getMessage("gui.buttons.refresh.name", placeholders));
            meta.setLore(List.of(messageManager.getMessage("gui.buttons.refresh.lore", placeholders)));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建关闭按钮
     * 统一的关闭按钮创建方法
     * 
     * @return 关闭按钮
     */
    @NotNull
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            meta.setDisplayName(messageManager.getMessage("gui.buttons.close.name", placeholders));
            meta.setLore(List.of(messageManager.getMessage("gui.buttons.close.lore", placeholders)));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 格式化时间
     * 统一的时间格式化方法
     *
     * @param timestamp 时间戳
     * @return 格式化的时间字符串
     */
    @NotNull
    private String formatTime(long timestamp) {
        // 使用MessageManager的相对时间格式化
        Map<String, String> placeholders = messageManager.createPlaceholders();
        messageManager.addTimePlaceholders(placeholders, timestamp);
        return placeholders.get("relative_time");
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
        if (slot == 49) { // 刷新按钮
            handleRefreshClick();
            return true;
        }
        
        if (slot == 53) { // 关闭按钮
            handleCloseClick();
            return true;
        }
        
        // 处理墓碑点击
        if (isTombstoneSlot(slot)) {
            handleTombstoneClick(slot);
            return true;
        }
        
        return true; // 取消所有其他点击
    }
    
    /**
     * 检查是否为墓碑槽位
     * 统一的槽位检查方法
     * 
     * @param slot 槽位
     * @return 是否为墓碑槽位
     */
    private boolean isTombstoneSlot(int slot) {
        // 检查是否在有效的墓碑区域内（排除边框）
        int row = slot / 9;
        int col = slot % 9;
        return row >= 1 && row <= 4 && col >= 1 && col <= 7;
    }
    
    /**
     * 处理刷新点击
     * 统一的刷新处理方法
     */
    private void handleRefreshClick() {
        // 重新加载墓碑数据
        tombstones.clear();
        tombstones.addAll(tombstoneManager.getPlayerTombstones(player.getUniqueId()));
        
        // 刷新GUI
        refreshGUI();
        
        // 发送消息
        Map<String, String> placeholders = messageManager.createPlaceholders();
        messageManager.addPlayerPlaceholders(placeholders, player);
        messageManager.sendMessage(player, "gui.refreshed", placeholders);
    }
    
    /**
     * 处理关闭点击
     * 统一的关闭处理方法
     */
    private void handleCloseClick() {
        closeGUI();
    }
    
    /**
     * 处理墓碑点击
     * 统一的墓碑点击处理方法
     * 
     * @param slot 点击的槽位
     */
    private void handleTombstoneClick(int slot) {
        // 计算墓碑索引
        int tombstoneIndex = calculateTombstoneIndex(slot);
        
        if (tombstoneIndex >= 0 && tombstoneIndex < tombstones.size()) {
            DataManager.TombstoneData tombstone = tombstones.get(tombstoneIndex);
            teleportToTombstone(tombstone);
        }
    }
    
    /**
     * 计算墓碑索引
     * 统一的索引计算方法
     * 
     * @param slot 槽位
     * @return 墓碑索引
     */
    private int calculateTombstoneIndex(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        
        // 调整为墓碑区域的相对位置
        int adjustedRow = row - 1;
        int adjustedCol = col - 1;
        
        return adjustedRow * 7 + adjustedCol;
    }
    
    /**
     * 传送到墓碑
     * 统一的传送方法
     * 
     * @param tombstone 墓碑数据
     */
    private void teleportToTombstone(@NotNull DataManager.TombstoneData tombstone) {
        // 检查权限
        if (!player.hasPermission("playerdeadmanager.teleport")) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "permission.no-teleport", placeholders);
            return;
        }
        
        // 创建传送位置
        Location teleportLocation = new Location(
            plugin.getServer().getWorld(tombstone.worldName()),
            tombstone.x() + 0.5,
            tombstone.y() + 1,
            tombstone.z() + 0.5
        );
        
        // 检查世界是否存在
        if (teleportLocation.getWorld() == null) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            placeholders.put("world", tombstone.worldName());
            messageManager.sendMessage(player, "teleport.world-not-found", placeholders);
            return;
        }
        
        // 执行传送
        player.teleport(teleportLocation);
        
        // 关闭GUI
        closeGUI();
        
        // 发送成功消息
        Map<String, String> placeholders = messageManager.createPlaceholders();
        messageManager.addPlayerPlaceholders(placeholders, player);
        messageManager.addLocationPlaceholders(placeholders, 
            tombstone.worldName(), tombstone.x(), tombstone.y(), tombstone.z());
        messageManager.sendMessage(player, "teleport.success", placeholders);
    }
}
