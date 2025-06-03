package cn.i7mc.managers;

import cn.i7mc.abstracts.AbstractGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI管理器 - 统一管理活跃的GUI实例
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class GUIManager {
    
    private final Map<UUID, AbstractGUI> activeGUIs;
    
    /**
     * 构造函数
     */
    public GUIManager() {
        this.activeGUIs = new ConcurrentHashMap<>();
    }
    
    /**
     * 注册GUI实例
     * 统一的GUI注册方法
     * 
     * @param player 玩家
     * @param gui GUI实例
     */
    public void registerGUI(@NotNull Player player, @NotNull AbstractGUI gui) {
        activeGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 注销GUI实例
     * 统一的GUI注销方法
     * 
     * @param player 玩家
     */
    public void unregisterGUI(@NotNull Player player) {
        activeGUIs.remove(player.getUniqueId());
    }
    
    /**
     * 获取玩家的活跃GUI
     * 统一的GUI获取方法
     * 
     * @param player 玩家
     * @return GUI实例，如果没有返回null
     */
    @Nullable
    public AbstractGUI getActiveGUI(@NotNull Player player) {
        return activeGUIs.get(player.getUniqueId());
    }
    
    /**
     * 检查玩家是否有活跃的GUI
     * 统一的GUI检查方法
     * 
     * @param player 玩家
     * @return 是否有活跃的GUI
     */
    public boolean hasActiveGUI(@NotNull Player player) {
        return activeGUIs.containsKey(player.getUniqueId());
    }
    
    /**
     * 通过Inventory查找对应的GUI
     * 统一的GUI查找方法
     * 
     * @param inventory 背包实例
     * @return GUI实例，如果没有找到返回null
     */
    @Nullable
    public AbstractGUI findGUIByInventory(@NotNull Inventory inventory) {
        for (AbstractGUI gui : activeGUIs.values()) {
            if (gui.getInventory().equals(inventory)) {
                return gui;
            }
        }
        return null;
    }
    
    /**
     * 清理所有GUI实例
     * 统一的清理方法
     */
    public void clearAllGUIs() {
        activeGUIs.clear();
    }
    
    /**
     * 获取活跃GUI数量
     * 统一的计数方法
     * 
     * @return 活跃GUI数量
     */
    public int getActiveGUICount() {
        return activeGUIs.size();
    }
}
