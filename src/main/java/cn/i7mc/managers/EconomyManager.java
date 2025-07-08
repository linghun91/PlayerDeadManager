package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

/**
 * 经济管理器 - 统一处理Vault经济系统集成
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class EconomyManager {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private Economy economy;
    private boolean vaultEnabled;
    
    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param configManager 配置管理器
     */
    public EconomyManager(@NotNull PlayerDeadManager plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.vaultEnabled = false;
        
        setupEconomy();
    }
    
    /**
     * 设置经济系统
     * 统一的Vault经济系统初始化方法
     */
    private void setupEconomy() {
        if (!configManager.getBoolean("compatibility.vault", true)) {
            plugin.getLogger().info("Vault经济系统已在配置中禁用");
            return;
        }
        
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("未找到Vault插件，经济功能将被禁用");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("未找到经济插件，经济功能将被禁用");
            return;
        }
        
        economy = rsp.getProvider();
        vaultEnabled = true;
        plugin.getLogger().info("Vault经济系统已成功连接: " + economy.getName());
    }
    
    /**
     * 检查是否启用了经济系统
     * 统一的经济系统状态检查方法
     * 
     * @return 是否启用经济系统
     */
    public boolean isEconomyEnabled() {
        return vaultEnabled && economy != null && configManager.getBoolean("economy.enabled", true);
    }
    
    /**
     * 获取玩家余额
     * 统一的余额查询方法
     * 
     * @param player 玩家
     * @return 玩家余额
     */
    public double getBalance(@NotNull Player player) {
        if (!isEconomyEnabled()) {
            return 0.0;
        }
        
        return economy.getBalance(player);
    }
    
    /**
     * 检查玩家是否有足够的金币
     * 统一的余额检查方法
     * 
     * @param player 玩家
     * @param amount 需要的金额
     * @return 是否有足够的金币
     */
    public boolean hasEnough(@NotNull Player player, double amount) {
        if (!isEconomyEnabled()) {
            return false;
        }
        
        return economy.has(player, amount);
    }
    
    /**
     * 从玩家账户扣除金币
     * 统一的扣费方法
     * 
     * @param player 玩家
     * @param amount 扣除金额
     * @return 是否扣费成功
     */
    public boolean withdraw(@NotNull Player player, double amount) {
        if (!isEconomyEnabled()) {
            return false;
        }
        
        if (!hasEnough(player, amount)) {
            return false;
        }
        
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    /**
     * 向玩家账户存入金币
     * 统一的存款方法
     * 
     * @param player 玩家
     * @param amount 存入金额
     * @return 是否存款成功
     */
    public boolean deposit(@NotNull Player player, double amount) {
        if (!isEconomyEnabled()) {
            return false;
        }
        
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    /**
     * 获取死亡扣费金额
     * 统一的扣费金额获取方法
     * 
     * @return 死亡扣费金额
     */
    public double getDeathCost() {
        return configManager.getDouble("economy.cost", 1.0);
    }
    
    /**
     * 处理死亡扣费保险机制
     * 统一的死亡扣费处理方法
     * 
     * @param player 玩家
     * @return 是否扣费成功（成功=保住物品，失败=正常掉落）
     */
    public boolean handleDeathCost(@NotNull Player player) {
        if (!isEconomyEnabled()) {
            return false; // 经济系统未启用，按正常掉落处理
        }
        
        double cost = getDeathCost();
        if (cost <= 0) {
            return false; // 扣费金额为0或负数，按正常掉落处理
        }
        
        return withdraw(player, cost);
    }
    
    /**
     * 获取经济插件名称
     * 统一的经济插件信息获取方法
     * 
     * @return 经济插件名称
     */
    public String getEconomyName() {
        if (!isEconomyEnabled()) {
            return "未启用";
        }
        
        return economy.getName();
    }
    
    /**
     * 格式化金额显示
     * 统一的金额格式化方法
     * 
     * @param amount 金额
     * @return 格式化后的金额字符串
     */
    public String formatAmount(double amount) {
        if (!isEconomyEnabled()) {
            return String.valueOf(amount);
        }
        
        return economy.format(amount);
    }
}
