package cn.i7mc.abstracts;

import cn.i7mc.managers.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GUI抽象类 - 统一处理GUI相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public abstract class AbstractGUI {

    protected final Player player;
    protected final String title;
    protected final int size;
    protected Inventory inventory;
    protected GUIManager guiManager;

    /**
     * 构造函数
     *
     * @param player GUI的目标玩家
     * @param title GUI标题（从message.yml获取）
     * @param size GUI大小（9的倍数）
     */
    protected AbstractGUI(@NotNull Player player, @NotNull String title, int size) {
        this.player = player;
        this.title = title;
        this.size = size;
        this.inventory = createInventory();
        this.guiManager = null; // 将在需要时设置
    }

    /**
     * 设置GUI管理器
     * 统一的GUI管理器设置方法
     *
     * @param guiManager GUI管理器实例
     */
    public void setGUIManager(@Nullable GUIManager guiManager) {
        this.guiManager = guiManager;
    }
    
    /**
     * 创建Inventory实例
     * 统一的Inventory创建方法
     *
     * @return 创建的Inventory
     */
    @NotNull
    protected Inventory createInventory() {
        // 将颜色代码转换为实际颜色
        String coloredTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
        return Bukkit.createInventory(null, size, coloredTitle);
    }
    
    /**
     * 初始化GUI内容
     * 统一的GUI初始化方法
     */
    public abstract void initializeGUI();
    
    /**
     * 处理GUI点击事件
     * 统一的点击处理方法
     * 
     * @param slot 点击的槽位
     * @param clickedItem 点击的物品
     * @return 是否取消事件
     */
    public abstract boolean handleClick(int slot, @Nullable ItemStack clickedItem);
    
    /**
     * 打开GUI给玩家
     * 统一的GUI打开方法
     */
    public void openGUI() {
        initializeGUI();

        // 注册到GUI管理器
        if (guiManager != null) {
            guiManager.registerGUI(player, this);
        }

        player.openInventory(inventory);
    }

    /**
     * 关闭GUI
     * 统一的GUI关闭方法
     */
    public void closeGUI() {
        // 从GUI管理器注销
        if (guiManager != null) {
            guiManager.unregisterGUI(player);
        }

        player.closeInventory();
    }
    
    /**
     * 刷新GUI内容
     * 统一的GUI刷新方法
     */
    public void refreshGUI() {
        inventory.clear();
        initializeGUI();
    }
    
    /**
     * 设置GUI中的物品
     * 统一的物品设置方法
     * 
     * @param slot 槽位
     * @param item 物品
     */
    protected void setItem(int slot, @Nullable ItemStack item) {
        if (slot >= 0 && slot < size) {
            inventory.setItem(slot, item);
        }
    }
    
    /**
     * 获取GUI中的物品
     * 统一的物品获取方法
     * 
     * @param slot 槽位
     * @return 物品，如果槽位无效返回null
     */
    @Nullable
    protected ItemStack getItem(int slot) {
        if (slot >= 0 && slot < size) {
            return inventory.getItem(slot);
        }
        return null;
    }
    
    /**
     * 检查是否为有效槽位
     * 
     * @param slot 槽位
     * @return 是否有效
     */
    protected boolean isValidSlot(int slot) {
        return slot >= 0 && slot < size;
    }
    
    /**
     * 获取GUI的玩家
     * 
     * @return 玩家
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * 获取GUI标题
     * 
     * @return 标题
     */
    @NotNull
    public String getTitle() {
        return title;
    }
    
    /**
     * 获取GUI大小
     * 
     * @return 大小
     */
    public int getSize() {
        return size;
    }
    
    /**
     * 获取Inventory实例
     * 
     * @return Inventory
     */
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
